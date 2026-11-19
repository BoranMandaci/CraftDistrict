package com.craftdistrict.block;

import com.craftdistrict.item.WrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Local blockstate pairing for ordinary hinged doors only; no ticking or special multiblock doors. */
public abstract class PairedDoorBlock extends DoorBlock {
    public static final BooleanProperty PAIRED = BooleanProperty.create("paired");
    private static final ThreadLocal<Boolean> UPDATING = ThreadLocal.withInitial(() -> false);
    private final VoxelShape[][][] shapes = new VoxelShape[2][2][];

    protected PairedDoorBlock(BlockSetType type, Properties properties, double thickness) {
        this(type, properties, thickness, false);
    }

    protected PairedDoorBlock(BlockSetType type, Properties properties, double thickness, boolean frontPlane) {
        //? if >=1.21 {
        super(type, properties);
        //?} else {
        /*super(properties, type);
        *///?}
        registerDefaultState(defaultBlockState().setValue(PAIRED, false));
        VoxelShape[] closed = RestaurantKitchenBlock.rotations(box(0, 0, frontPlane ? 0 : 16 - thickness,
                16, 16, frontPlane ? thickness : 16));
        for (int right = 0; right < 2; right++) {
            shapes[0][right] = closed;
            // Same quarter-turn hinge transform as the leaf models: stay in the outer edge strip.
            shapes[1][right] = RestaurantKitchenBlock.rotations(box(right == 0 ? 0 : 16 - thickness,
                    0, 0, right == 0 ? thickness : 16, 16, 16));
        }
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PAIRED);
    }

    protected final BlockPos bottom(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    protected final boolean complete(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState lower = level.getBlockState(pos), upper = level.getBlockState(pos.above());
        return lower.is(this) && upper.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER
                && upper.getValue(HALF) == DoubleBlockHalf.UPPER && lower.getValue(FACING) == upper.getValue(FACING);
    }

    private BlockPos partnerPosition(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return pos.relative(state.getValue(HINGE) == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
    }

    protected final BlockPos partner(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(PAIRED)) return null;
        BlockPos otherPos = partnerPosition(state, pos);
        if (!complete(level, otherPos)) return null;
        BlockState other = level.getBlockState(otherPos);
        return other.getValue(PAIRED) && other.getValue(FACING) == state.getValue(FACING)
                && other.getValue(HINGE) != state.getValue(HINGE) ? otherPos : null;
    }

    private void writeDoor(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos) != state) level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        BlockState upper = state.setValue(HALF, DoubleBlockHalf.UPPER);
        if (level.getBlockState(pos.above()) != upper)
            level.setBlock(pos.above(), upper, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private void notifyDoor(Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.above(), this);
    }

    private void refreshPair(Level level, BlockPos pos) {
        if (level.isClientSide || UPDATING.get() || !complete(level, pos)) return;
        UPDATING.set(true);
        try {
            BlockState state = level.getBlockState(pos);
            if (partner(level, pos, state) != null) return; // Never steal a member of a valid pair.
            boolean changed = false;
            if (state.getValue(PAIRED)) {
                if (!level.hasChunkAt(partnerPosition(state, pos))) return; // Retain unloaded partners.
                state = state.setValue(PAIRED, false);
                writeDoor(level, pos, state);
                changed = true;
            }
            Direction facing = state.getValue(FACING);
            // Stable left-before-right tie break; an already established pair always wins.
            for (Direction side : new Direction[]{facing.getCounterClockWise(), facing.getClockWise()}) {
                BlockPos otherPos = pos.relative(side);
                if (!complete(level, otherPos)) continue;
                BlockState other = level.getBlockState(otherPos);
                if (other.getValue(FACING) != facing || partner(level, otherPos, other) != null
                        || other.getValue(PAIRED) && !level.hasChunkAt(partnerPosition(other, otherPos))) continue;
                DoorHingeSide hinge = side == facing.getCounterClockWise() ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
                boolean open = state.getValue(OPEN) || other.getValue(OPEN);
                writeDoor(level, pos, pairedSettings(state, other).setValue(PAIRED, true).setValue(HINGE, hinge).setValue(OPEN, open));
                writeDoor(level, otherPos, pairedSettings(other, state).setValue(PAIRED, true).setValue(HINGE,
                        hinge == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT).setValue(OPEN, open));
                notifyDoor(level, otherPos);
                changed = true;
                break;
            }
            if (changed) notifyDoor(level, pos);
        } finally { UPDATING.remove(); }
    }

    /** Specialized entrances can share configuration without changing ordinary hinged-door behavior. */
    protected BlockState pairedSettings(BlockState own, BlockState other) { return own; }
    protected boolean canManuallyOpen(BlockState state, BlockPos pos, Player player) { return true; }
    protected boolean redstoneOpen(Level level, BlockPos root, boolean powered) { return powered; }
    protected void doorChanged(Level level, BlockPos root) { }
    protected void playDoorSounds(Entity source, Level level, BlockPos[] positions, BlockState[] before, boolean open) {
        for (int i = 0; i < positions.length; i += 2) if (before[i].getValue(OPEN) != open)
            level.playSound(source instanceof Player player ? player : null, positions[i],
                    open ? type().doorOpen() : type().doorClose(), SoundSource.BLOCKS,
                    1, level.random.nextFloat() * .1F + .9F);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        refreshPair(level, pos);
        if (!level.isClientSide) neighborChanged(level.getBlockState(pos), level, pos, this, pos, false);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (next.is(this) || level.isClientSide || UPDATING.get()) return;
        BlockPos root = bottom(state, pos);
        Direction facing = state.getValue(FACING);
        refreshPair(level, root.relative(facing.getCounterClockWise()));
        refreshPair(level, root.relative(facing.getClockWise()));
    }

    @Override public void setOpen(Entity source, Level level, BlockState state, BlockPos pos, boolean open) {
        if (UPDATING.get()) return;
        BlockPos root = bottom(state, pos);
        if (!complete(level, root)) return;
        refreshPair(level, root);
        BlockState lower = level.getBlockState(root);
        BlockPos other = partner(level, root, lower);
        change(source, level, root, other, open, null);
    }

    private boolean change(Entity source, Level level, BlockPos root, BlockPos other, boolean open, Boolean powered) {
        UPDATING.set(true);
        try {
            BlockPos[] positions = other == null ? new BlockPos[]{root, root.above()}
                    : new BlockPos[]{root, root.above(), other, other.above()};
            BlockState[] before = new BlockState[positions.length], after = new BlockState[positions.length];
            boolean changed = false;
            for (int i = 0; i < positions.length; i++) before[i] = level.getBlockState(positions[i]);
            for (int i = 0; i < positions.length; i += 2) {
                after[i] = before[i].setValue(OPEN, open);
                if (powered != null) after[i] = after[i].setValue(POWERED, powered);
                after[i + 1] = after[i].setValue(HALF, DoubleBlockHalf.UPPER);
                changed |= before[i] != after[i] || before[i + 1] != after[i + 1];
            }
            if (!changed || !DoorStateUpdates.commit(level, positions, before, after)) return false;
            // Preserve vanilla connections only after the complete entrance has committed.
            for (int i = 0; i < positions.length; i++) if (before[i] != after[i])
                after[i].updateNeighbourShapes(level, positions[i], Block.UPDATE_CLIENTS, 512);
            playDoorSounds(source, level, positions, before, open);
            for (int i = 0; i < positions.length; i += 2) if (before[i].getValue(OPEN) != open) {
                if (!level.isClientSide) level.gameEvent(source, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, positions[i]);
            }
            doorChanged(level, root);
            return true;
        } finally { UPDATING.remove(); }
    }

    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide || UPDATING.get()) return;
        BlockPos root = bottom(state, pos);
        if (!complete(level, root)) return;
        refreshPair(level, root);
        BlockState lower = level.getBlockState(root);
        if (lower.getValue(PAIRED) && !level.hasChunkAt(partnerPosition(lower, root))) return;
        BlockPos other = partner(level, root, lower);
        boolean powered = level.hasNeighborSignal(root) || level.hasNeighborSignal(root.above())
                || other != null && (level.hasNeighborSignal(other) || level.hasNeighborSignal(other.above()));
        if (lower.getValue(POWERED) == powered && (other == null || level.getBlockState(other).getValue(POWERED) == powered)) return;
        change(null, level, root, other, redstoneOpen(level, root, powered), powered);
    }

    private InteractionResult toggle(BlockState state, Level level, BlockPos pos, Player player) {
        BlockPos root = bottom(state, pos);
        if (!complete(level, root)) return InteractionResult.PASS;
        BlockState lower = level.getBlockState(root);
        if (!lower.getValue(OPEN) && !canManuallyOpen(lower, root, player)) return InteractionResult.FAIL;
        refreshPair(level, root);
        lower = level.getBlockState(root);
        if (!change(player, level, root, partner(level, root, lower), !lower.getValue(OPEN), null)) return InteractionResult.PASS;
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var result = toggle(state, level, pos, player);
        if (!level.isClientSide && result.consumesAction() && level.getBlockState(pos) != state)
            WrenchItem.usedIfHeld(player);
        return result;
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        var result = toggle(state, level, pos, player);
        if (!level.isClientSide && result.consumesAction() && level.getBlockState(pos) != state)
            WrenchItem.used(player, hand, player.getItemInHand(hand));
        return result;
    }
    *///?}

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(OPEN) ? 1 : 0][state.getValue(HINGE) == DoorHingeSide.RIGHT ? 1 : 0]
                [RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
