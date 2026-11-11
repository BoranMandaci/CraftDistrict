package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-high row cabinets share end panels; saved compact placements keep their original footprint. */
public final class HotelCleaningCabinetBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty COMPACT = BooleanProperty.create("compact"), LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);
    //? if >=1.21 {
    public static final MapCodec<HotelCleaningCabinetBlock> CODEC = simpleCodec(HotelCleaningCabinetBlock::new);
    //?}
    public HotelCleaningCabinetBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(COMPACT, false).setValue(LEFT, false).setValue(RIGHT, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { super.addPartProperties(builder); builder.add(COMPACT, LEFT, RIGHT); }
    @Override protected int partCount(BlockState state) { return state.getValue(COMPACT) ? 1 : 2; }
    private boolean joins(BlockState state, LevelReader level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState other = level.getBlockState(pos);
        if (!other.is(this) || !other.getValue(PRIMARY) || other.getValue(COMPACT) != state.getValue(COMPACT)
                || other.getValue(FACING) != state.getValue(FACING)) return false;
        return other.getValue(COMPACT) || level.hasChunkAt(pos.above()) && level.getBlockState(pos.above()) == stateForPart(other, 1);
    }
    private BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, joins(state, level, pos.relative(facing.getCounterClockWise())))
                .setValue(RIGHT, joins(state, level, pos.relative(facing.getClockWise())));
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return layout(super.getPlacementState(context).setValue(COMPACT, false), context.getLevel(), context.getClickedPos());
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(COMPACT) && !state.getValue(PRIMARY)) return false;
        BlockPos floor = structureOrigin(state, pos).below();
        return DecorationSupport.isPlaced(state, level, pos) || level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos other) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (state.getValue(COMPACT)) {
            return canSurvive(state, level, pos) ? layout(state, level, pos) : Blocks.AIR.defaultBlockState();
        }
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        refreshAround(level, pos);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving) {
        super.neighborChanged(state, level, pos, source, sourcePos, moving);
        if (!level.isClientSide && !isRemovingLinkedStructure(state, level, pos)) refresh(level, pos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean refresh = !level.isClientSide && !replacement.is(this) && !isRemovingLinkedStructure(state, level, pos);
        BlockPos origin = structureOrigin(state, pos);
        super.onRemove(state, level, pos, replacement, moving);
        if (refresh) refreshAround(level, origin);
    }
    private void refreshAround(Level level, BlockPos origin) {
        if (level.isClientSide) return;
        refresh(level, origin);
        for (Direction side : Direction.Plane.HORIZONTAL) refresh(level, origin.relative(side));
    }
    /** Both halves receive the same topology together, preserving exact partner matching on removal. */
    private void refresh(Level level, BlockPos pos) {
        if (level.isClientSide || !level.hasChunkAt(pos)) return;
        BlockState clicked = level.getBlockState(pos);
        if (!clicked.is(this)) return;
        BlockPos origin = structureOrigin(clicked, pos);
        if (!level.hasChunkAt(origin)) return;
        BlockState lower = level.getBlockState(origin);
        if (!lower.is(this) || !lower.getValue(PRIMARY)) return;
        boolean compact = lower.getValue(COMPACT);
        if (!compact && !level.hasChunkAt(origin.above())) return;
        BlockState upper = compact ? lower : level.getBlockState(origin.above());
        if (!compact && upper != stateForPart(lower, 1)) return;
        BlockState next = layout(lower, level, origin), nextUpper = stateForPart(next, 1);
        if (next == lower) return;
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        if (!level.setBlock(origin, next, flags) || compact) return;
        if (level.getBlockState(origin) != next || level.getBlockState(origin.above()) != upper
                || !level.setBlock(origin.above(), nextUpper, flags)
                || level.getBlockState(origin) != next || level.getBlockState(origin.above()) != nextUpper) {
            if (level.getBlockState(origin.above()) == nextUpper) level.setBlock(origin.above(), upper, flags);
            if (level.getBlockState(origin) == next) level.setBlock(origin, lower, flags);
        }
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
