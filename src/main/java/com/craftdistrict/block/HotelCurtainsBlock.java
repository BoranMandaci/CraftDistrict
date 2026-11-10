package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-high curtain columns commit shared end states together, without ticking or animated cloth. */
public final class HotelCurtainsBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(0, .25, 12.5, 16, 16, 15.75)),
            RestaurantKitchenBlock.rotations(box(0, 0, 12.5, 16, 16, 16))};
    //? if >=1.21 {
    public static final MapCodec<HotelCurtainsBlock> CODEC = simpleCodec(HotelCurtainsBlock::new);
    //?}
    public HotelCurtainsBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(LEFT, RIGHT);
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        return context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : super.getPlacementFacing(context);
    }
    private boolean complete(LevelReader level, BlockPos pos, Direction facing) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) return false;
        BlockState a = level.getBlockState(pos), b = level.getBlockState(pos.above());
        return a.is(this) && b.is(this) && a.getValue(PRIMARY) && !b.getValue(PRIMARY)
                && a.getValue(FACING) == facing && b.getValue(FACING) == facing;
    }
    private BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, complete(level, pos.relative(facing.getCounterClockWise()), facing))
                .setValue(RIGHT, complete(level, pos.relative(facing.getClockWise()), facing));
    }
    private void refresh(Level level, BlockPos origin, Direction facing) {
        if (!complete(level, origin, facing)) return;
        BlockState lower = level.getBlockState(origin), upper = level.getBlockState(origin.above());
        BlockState joined = layout(lower, level, origin), other = stateForPart(joined, 1);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        if (lower != joined) level.setBlock(origin, joined, flags);
        if (upper != other && level.getBlockState(origin) == joined && level.getBlockState(origin.above()) == upper)
            level.setBlock(origin.above(), other, flags);
    }
    private void refreshRow(Level level, BlockPos origin, Direction facing) {
        refresh(level, origin, facing);
        refresh(level, origin.relative(facing.getCounterClockWise()), facing);
        refresh(level, origin.relative(facing.getClockWise()), facing);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(PRIMARY)) refreshRow(level, pos, state.getValue(FACING));
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos otherPos, boolean moving) {
        super.neighborChanged(state, level, pos, source, otherPos, moving);
        if (!level.isClientSide && !isRemovingLinkedStructure(state, level, pos))
            refresh(level, structureOrigin(state, pos), state.getValue(FACING));
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos origin = structureOrigin(state, pos), floor = origin.below(), ceiling = origin.above(2);
        BlockPos wall = origin.above().relative(state.getValue(FACING).getOpposite());
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                || level.hasChunkAt(ceiling) && level.getBlockState(ceiling).isFaceSturdy(level, ceiling, Direction.DOWN)
                || level.hasChunkAt(wall) && level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING));
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && (!neighbor.is(this) || neighbor.getValue(PRIMARY) == state.getValue(PRIMARY)
                || neighbor.getValue(FACING) != state.getValue(FACING))) return Blocks.AIR.defaultBlockState();
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean nested = isRemovingLinkedStructure(state, level, pos);
        BlockPos origin = structureOrigin(state, pos); Direction facing = state.getValue(FACING);
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && !nested && !replacement.is(this)) refreshRow(level, origin, facing);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
