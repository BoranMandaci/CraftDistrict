package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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

/** New signs span two wall cells; saved single-cell signs retain their original footprint. */
public final class HospitalDepartmentSignBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    //? if >=1.21 {
    public static final MapCodec<HospitalDepartmentSignBlock> CODEC = simpleCodec(HospitalDepartmentSignBlock::new);
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(1, 5, 14.75, 15, 12, 16)),
            RestaurantKitchenBlock.rotations(box(1, 5, 14.75, 16, 12, 16)),
            RestaurantKitchenBlock.rotations(box(0, 5, 14.75, 15, 12, 16))};
    public HospitalDepartmentSignBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(EXPANDED, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(EXPANDED);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 2 : 1; }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getClockWise(); }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        return context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null;
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(EXPANDED, true);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(EXPANDED) && !state.getValue(PRIMARY)) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction facing = state.getValue(FACING); BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (state.getValue(EXPANDED)) {
            Direction along = getExtensionDirection(state);
            Direction partner = state.getValue(PRIMARY) ? along : along.getOpposite();
            if (side == partner && other != stateForPart(state, 1 - partIndex(state))) return Blocks.AIR.defaultBlockState();
        }
        return side == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror != Mirror.NONE && state.getValue(EXPANDED)
                ? result.setValue(PRIMARY, !state.getValue(PRIMARY)) : result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(EXPANDED) ? (state.getValue(PRIMARY) ? 1 : 2) : 0][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
