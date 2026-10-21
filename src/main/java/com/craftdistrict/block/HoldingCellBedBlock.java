package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vanilla sleeping/respawn behavior with static JSON visuals and no block entity. */
public final class HoldingCellBedBlock extends BedBlock {
    // Old saves used a sideways furniture facing and primary=true for the head.
    public static final BooleanProperty PRIMARY = MultiBlockDecorationBlock.PRIMARY;
    public static final BooleanProperty LEGACY_LAYOUT = BooleanProperty.create("legacy_layout");
    //? if >=1.21 {
    public static final MapCodec<BedBlock> CODEC = simpleCodec(HoldingCellBedBlock::new);

    @Override
    public MapCodec<BedBlock> codec() {
        return CODEC;
    }
    //?}

    public HoldingCellBedBlock(Properties properties) {
        super(DyeColor.GRAY, properties);
        // Missing properties in old saves resolve to these compatible legacy defaults.
        registerDefaultState(defaultBlockState().setValue(PRIMARY, true).setValue(LEGACY_LAYOUT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PRIMARY, LEGACY_LAYOUT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        state = state.setValue(LEGACY_LAYOUT, false).setValue(PRIMARY, false).setValue(PART, BedPart.FOOT);
        Level level = context.getLevel();
        BlockPos foot = context.getClickedPos();
        BlockPos head = foot.relative(state.getValue(FACING));
        CollisionContext collision = context.getPlayer() == null
                ? CollisionContext.empty() : CollisionContext.of(context.getPlayer());
        if (!validPosition(level, foot) || !validPosition(level, head)
                || !level.getBlockState(foot.below()).isFaceSturdy(level, foot.below(), Direction.UP)
                || !level.getBlockState(head.below()).isFaceSturdy(level, head.below(), Direction.UP)
                || !level.isUnobstructed(state, foot, collision)
                || !level.isUnobstructed(state.setValue(PART, BedPart.HEAD).setValue(PRIMARY, true), head, collision)) {
            return null;
        }
        return state;
    }

    private static boolean validPosition(Level level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;
        BlockPos headPos = pos.relative(state.getValue(FACING));
        BlockState head = state.setValue(PART, BedPart.HEAD).setValue(PRIMARY, true);
        CollisionContext collision = placer == null ? CollisionContext.empty() : CollisionContext.of(placer);
        // Recheck after item-placement callbacks; never overwrite a new obstruction.
        if (level.getBlockState(pos) != state) return;
        if (!validPosition(level, headPos) || !level.getBlockState(headPos).canBeReplaced()
                || !level.getBlockState(headPos.below()).isFaceSturdy(level, headPos.below(), Direction.UP)
                || !level.isUnobstructed(head, headPos, collision) || !level.setBlock(headPos, head, Block.UPDATE_ALL)) {
            if (level.getBlockState(pos) == state) level.removeBlock(pos, false);
            return;
        }
        level.blockUpdated(pos, Blocks.AIR);
        state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
    }

    private boolean legacyPartner(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(LEGACY_LAYOUT)
                && other.getValue(FACING) == state.getValue(FACING)
                && other.getValue(PRIMARY) != state.getValue(PRIMARY);
    }

    private static BlockPos legacyPartnerPos(BlockPos pos, BlockState state) {
        Direction width = state.getValue(FACING).getClockWise();
        return pos.relative(state.getValue(PRIMARY) ? width : width.getOpposite());
    }

    private BlockState upgradeLegacy(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(LEGACY_LAYOUT) || level.isClientSide) return state;
        BlockPos otherPos = legacyPartnerPos(pos, state);
        BlockState other = level.getBlockState(otherPos);
        if (!legacyPartner(state, other)) return state;
        Direction headDirection = state.getValue(FACING).getCounterClockWise();
        BlockState converted = state.setValue(LEGACY_LAYOUT, false).setValue(FACING, headDirection)
                .setValue(PART, state.getValue(PRIMARY) ? BedPart.HEAD : BedPart.FOOT);
        BlockState convertedOther = other.setValue(LEGACY_LAYOUT, false).setValue(FACING, headDirection)
                .setValue(PART, other.getValue(PRIMARY) ? BedPart.HEAD : BedPart.FOOT);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        // Write both states before neighbor validation; neither occupied position moves.
        if (!level.setBlock(pos, converted, flags) || !level.setBlock(otherPos, convertedOther, flags)) {
            if (level.getBlockState(pos) == converted) level.setBlock(pos, state, flags);
            if (level.getBlockState(otherPos) == convertedOther) level.setBlock(otherPos, other, flags);
            return state;
        }
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(otherPos, this);
        return level.getBlockState(pos);
    }

    //? if >=1.21 {
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        state = upgradeLegacy(level, pos, state);
        return state.is(this) && !state.getValue(LEGACY_LAYOUT)
                ? super.useWithoutItem(state, level, pos, player, hit) : InteractionResult.CONSUME;
    }
    //?} else {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        state = upgradeLegacy(level, pos, state);
        return state.is(this) && !state.getValue(LEGACY_LAYOUT)
                ? super.use(state, level, pos, player, hand, hit) : InteractionResult.CONSUME;
    }
    *///?}

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (!state.getValue(LEGACY_LAYOUT)) return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        if (neighborPos.equals(legacyPartnerPos(pos, state)) && !legacyPartner(state, neighbor)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && state.getValue(LEGACY_LAYOUT) && !replacement.is(this)) {
            BlockPos otherPos = legacyPartnerPos(pos, state);
            if (level.hasChunkAt(otherPos) && legacyPartner(state, level.getBlockState(otherPos))) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    //? if >=1.21 {
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        return super.playerWillDestroy(level, pos, state.getValue(LEGACY_LAYOUT) ? state.setValue(PART, BedPart.HEAD) : state, player);
    }
    //?} else {
    /*@Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state.getValue(LEGACY_LAYOUT) ? state.setValue(PART, BedPart.HEAD) : state, player);
    }
    *///?}

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean legacy = state.getValue(LEGACY_LAYOUT);
        int part = (legacy ? state.getValue(PRIMARY) : state.getValue(PART) == BedPart.HEAD) ? 0 : 1;
        BlockState visual = legacy ? state : state.setValue(FACING, state.getValue(FACING).getClockWise());
        return PoliceDetentionShapes.get(0, part, visual);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null; // Sleep/respawn uses blockstates; the vanilla bed entity is only for rendering.
    }
}
