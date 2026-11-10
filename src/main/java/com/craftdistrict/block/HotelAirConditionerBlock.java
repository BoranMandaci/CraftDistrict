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

/** New AC units occupy two wall cells; previously saved single-cell units retain their footprint. */
public final class HotelAirConditionerBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty WIDE = BooleanProperty.create("wide");
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(1, 7, 11.5, 15, 14, 16)),
            RestaurantKitchenBlock.rotations(box(1, 6.125, 11.125, 16, 14, 16)),
            RestaurantKitchenBlock.rotations(box(0, 6.125, 11.125, 15, 14, 16))};
    //? if >=1.21 {
    public static final MapCodec<HotelAirConditionerBlock> CODEC = simpleCodec(HotelAirConditionerBlock::new);
    //?}
    public HotelAirConditionerBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(WIDE, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(WIDE);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(WIDE) ? 2 : 1; }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getClockWise(); }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        return context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null;
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(WIDE, true);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(WIDE) && !state.getValue(PRIMARY)) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = state.getValue(FACING); BlockPos wall = pos.relative(face.getOpposite());
        return level.hasChunkAt(wall) && level.getBlockState(wall).isFaceSturdy(level, wall, face);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (state.getValue(WIDE)) {
            Direction partner = state.getValue(PRIMARY) ? getExtensionDirection(state) : getExtensionDirection(state).getOpposite();
            if (side == partner && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        }
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE || !state.getValue(WIDE) ? result : result.setValue(PRIMARY, !state.getValue(PRIMARY));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(WIDE) ? state.getValue(PRIMARY) ? 1 : 2 : 0][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
