package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.HashSet;
import java.util.Set;

/** Nine doorway cells; temporary swing cells exist only while the full leaf is open. */
public final class VaultDoorBlock extends WideTallDecorationBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 17);
    public static final BooleanProperty LARGE = BooleanProperty.create("large");
    public static final BooleanProperty FORWARD = BooleanProperty.create("forward");
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private record TransitionCell(Level level, BlockPos pos) { }
    private static final ThreadLocal<Set<TransitionCell>> CHANGING_WINGS = new ThreadLocal<>();
    private static final VoxelShape[][][][] LEGACY_SHAPES = createLegacyShapes();
    private static final VoxelShape[][][][] LARGE_SHAPES = createLargeShapes(false);
    private static final VoxelShape[][][][] FORWARD_SHAPES = createLargeShapes(true);
    //? if >=1.21 {
    public static final MapCodec<VaultDoorBlock> CODEC = simpleCodec(VaultDoorBlock::new);
    //?}

    public VaultDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LARGE, false).setValue(FORWARD, false).setValue(BlockStateProperties.OPEN, false)
                .setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, LARGE, FORWARD, BlockStateProperties.OPEN, BlockStateProperties.DOOR_HINGE);
    }
    @Override protected int partCount() { return 9; }
    @Override protected int partCount(BlockState state) {
        return !state.getValue(LARGE) ? 4 : state.getValue(BlockStateProperties.OPEN) ? (state.getValue(FORWARD) ? 18 : 15) : 9;
    }
    @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        Direction right = state.getValue(FACING).getClockWise();
        if (!state.getValue(LARGE)) return BlockPos.ZERO.relative(right, index % 2).above(index / 2);
        if (index < 9) return BlockPos.ZERO.relative(right, index % 3).above(index / 3);
        return BlockPos.ZERO.relative(right, state.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.LEFT ? 0 : 2)
                .above((index - 9) % 3).relative(state.getValue(FACING), 1 + (index - 9) / 3);
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) return null;
        Direction right = state.getValue(FACING).getClockWise();
        double side = (context.getClickLocation().x - context.getClickedPos().getX() - .5) * right.getStepX()
                + (context.getClickLocation().z - context.getClickedPos().getZ() - .5) * right.getStepZ();
        return state.setValue(LARGE, true).setValue(FORWARD, true).setValue(BlockStateProperties.DOOR_HINGE,
                side > 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
    }
    private InteractionResult toggle(Level level, BlockPos pos, Player player) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) return InteractionResult.PASS;
        if (!current.getValue(LARGE))
            return CabinetInteraction.toggleLinked(this, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
        BlockPos origin = structureOrigin(current, pos);
        BlockState primary = level.getBlockState(origin);
        if (!primary.is(this) || !isStructureComplete(primary, level, origin)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockState target = primary.setValue(BlockStateProperties.OPEN, !primary.getValue(BlockStateProperties.OPEN));
        int cells = primary.getValue(FORWARD) ? 18 : 15;
        BlockPos[] positions = new BlockPos[cells];
        BlockState[] before = new BlockState[cells], after = new BlockState[cells];
        for (int i = 0; i < cells; i++) {
            BlockPos p = origin.offset(partOffset(primary, i)).immutable(); positions[i] = p;
            if (level.isOutsideBuildHeight(p) || !level.hasChunkAt(p) || !level.getWorldBorder().isWithinBounds(p)
                    || !level.mayInteract(player, p)) return InteractionResult.FAIL;
            before[i] = level.getBlockState(p);
            // Never displace water, decorations, or another fixture in the swing space.
            if (i >= partCount(primary) && !before[i].isAir()) return InteractionResult.FAIL;
            after[i] = i < partCount(target) ? stateForPart(target, i) : Blocks.AIR.defaultBlockState();
        }
        Set<TransitionCell> changing = CHANGING_WINGS.get();
        if (changing == null) { changing = new HashSet<>(); CHANGING_WINGS.set(changing); }
        for (int i = 9; i < cells; i++) changing.add(new TransitionCell(level, positions[i]));
        int written = 0;
        boolean valid = true;
        try {
            for (int i = 0; i < cells; i++) {
                if (level.getBlockState(positions[i]) != before[i] || !level.setBlock(positions[i], after[i], UPDATE_FLAGS)) {
                    valid = false; break;
                }
                written++;
                for (int j = 0; j < written; j++) if (level.getBlockState(positions[j]) != after[j]) { valid = false; break; }
                if (!valid) break;
            }
            if (!valid) for (int i = written - 1; i >= 0; i--)
                if (level.getBlockState(positions[i]) == after[i]) level.setBlock(positions[i], before[i], UPDATE_FLAGS);
        } finally {
            for (int i = 9; i < cells; i++) changing.remove(new TransitionCell(level, positions[i]));
            if (changing.isEmpty()) CHANGING_WINGS.remove();
        }
        if (!valid) return InteractionResult.PASS;
        com.craftdistrict.item.WrenchItem.usedIfHeld(player);
        boolean open = target.getValue(BlockStateProperties.OPEN);
        level.playSound(null, origin, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS, .8F, .7F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, origin);
        return InteractionResult.CONSUME;
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggle(level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? toggle(level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int p = partIndex(state);
        if (p >= partCount(state)) return false;
        if (state.getValue(LARGE) && p >= 9) return true; // Hanging leaf sections are supported by the doorway.
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = pos.below(p / (state.getValue(LARGE) ? 3 : 2) + 1);
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos other) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int i = 0; i < partCount(state); i++) if (origin.offset(partOffset(state, i)).equals(other)
                && (!neighbor.is(this) || neighbor.getValue(PART) != i || neighbor.getValue(FACING) != state.getValue(FACING)
                || neighbor.getValue(LARGE) != state.getValue(LARGE)
                || neighbor.getValue(FORWARD) != state.getValue(FORWARD)
                || neighbor.getValue(BlockStateProperties.DOOR_HINGE) != state.getValue(BlockStateProperties.DOOR_HINGE)))
            return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        Set<TransitionCell> changing = CHANGING_WINGS.get();
        // Closing/rolling back a temporary leaf cell must not tear down its stationary frame.
        if (changing != null && changing.contains(new TransitionCell(level, pos))) return;
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) return state;
        int p = partIndex(state);
        int mirrored = state.getValue(LARGE) ? p < 9 ? p / 3 * 3 + 2 - p % 3 : p : p ^ 1;
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)))
                .setValue(PART, mirrored).setValue(BlockStateProperties.DOOR_HINGE,
                        state.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
    }
    private static VoxelShape[][][][] createLargeShapes(boolean forward) {
        VoxelShape frame = Shapes.or(box(0, 0, 10, 2.5, 48, 16), box(45.5, 0, 10, 48, 48, 16),
                box(2.5, 46.5, 10, 45.5, 48, 16), box(2.5, 0, 10, 45.5, .375, 16));
        int cells = forward ? 18 : 15;
        VoxelShape[][][][] result = new VoxelShape[2][2][cells][];
        for (int open = 0; open < 2; open++) for (int right = 0; right < 2; right++) {
            VoxelShape leaf = open == 0 ? Shapes.or(box(2.5, .375, 10, 45.5, 46.5, 15.5),
                    box(8, 8, 7, 40, 40, 10), box(15, 15, 4.75, 33, 33, 7), box(19, 23, 15.5, 29, 27, 17.5))
                    : Shapes.or(box(right == 0 ? -.5 : 43, .375, -30, right == 0 ? 5 : 48.5, 46.5, 13),
                            box(right == 0 ? -3.5 : 48, 8, -23.5, right == 0 ? -.5 : 51.5, 40, 6.5),
                            box(right == 0 ? -6 : 51.5, 15, -17, right == 0 ? -3.5 : 54, 33, 0),
                            box(right == 0 ? 5 : 41, 23, -13.5, right == 0 ? 7 : 43, 27, -3.5));
            // Translate depth only: retain the front/back detailing and the existing hinge swing.
            VoxelShape whole = Shapes.or(frame, leaf).move(0, 0, forward ? -.625 : 0);
            for (int part = 0; part < cells; part++) {
                int x = part < 9 ? part % 3 : right * 2;
                int y = part < 9 ? part / 3 : (part - 9) % 3;
                int depth = part < 9 ? 0 : 1 + (part - 9) / 3;
                double minZ = forward && open == 0 && depth == 0 ? -1 : -depth;
                VoxelShape clip = Shapes.box(x == 0 ? -1 : x, y, minZ,
                        x == 2 ? 4 : x + 1, y + 1, depth == 0 ? 2 : 1 - depth);
                VoxelShape local = open == 0 && part >= 9 ? Shapes.empty()
                        : Shapes.join(whole, clip, BooleanOp.AND).move(-x, -y, depth).optimize();
                result[open][right][part] = RestaurantKitchenBlock.rotations(local);
            }
        }
        return result;
    }
    private static VoxelShape[][][][] createLegacyShapes() {
        VoxelShape frame = Shapes.or(box(0, 0, 10, 2, 32, 16), box(30, 0, 10, 32, 32, 16),
                box(2, 31, 10, 30, 32, 16), box(2, 0, 10, 30, .375, 16));
        VoxelShape[][][][] result = new VoxelShape[2][2][4][];
        for (int open = 0; open < 2; open++) for (int right = 0; right < 2; right++) {
            VoxelShape leaf = open == 0 ? Shapes.or(box(2, .375, 10.0, 30, 31, 15.5),
                    box(6.25, 6.25, 8.75, 25.75, 25.75, 10.0), box(10, 10, 6.25, 22, 22, 8.75),
                    box(13, 15, 15.5, 18.6, 17, 16.8))
                    : Shapes.or(box(right == 0 ? 0 : 27.5, .375, -15, right == 0 ? 4.5 : 32, 31, 13.75),
                            box(right == 0 ? -4.75 : 32, 10, -7, right == 0 ? 0 : 36.75, 22, 5),
                            box(right == 0 ? -2.25 : 32, 6.25, -10.75, right == 0 ? 0 : 34.25, 25.75, 8.75),
                            box(right == 0 ? 4.5 : 26.2, 15, -3.6, right == 0 ? 5.8 : 27.5, 17, 1.4));
            VoxelShape whole = Shapes.or(frame, leaf);
            for (int part = 0; part < 4; part++) {
                int x = part % 2, y = part / 2;
                // Preserve the projecting leaf/handwheel; clipping them to the original doorway would lose collision.
                VoxelShape clip = Shapes.box(x == 0 ? -1 : 1, y, -1, x == 0 ? 1 : 3, y + 1, 2);
                result[open][right][part] = RestaurantKitchenBlock.rotations(
                        Shapes.join(whole, clip, BooleanOp.AND).move(-x, -y, 0).optimize());
            }
        }
        return result;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (partIndex(state) >= partCount(state)) return Shapes.empty();
        return (state.getValue(LARGE) ? state.getValue(FORWARD) ? FORWARD_SHAPES : LARGE_SHAPES : LEGACY_SHAPES)
                [state.getValue(BlockStateProperties.OPEN) ? 1 : 0]
                [state.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.RIGHT ? 1 : 0]
                [state.getValue(PART)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
