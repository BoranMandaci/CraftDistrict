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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** New tables occupy two cells; existing single-cell tables never claim or remove a neighbor. */
public final class HotelCoffeeTableBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(3, 0, 5, 13, 1.25, 11),
                    box(5, 1.25, 5, 11, 8, 11), box(1, 8, 2.5, 15, 9.5, 13.5)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(9, 0, 3, 16, 2, 13),
                    box(10.5, 2, 4.75, 16, 13.875, 11.25), box(0, 13.875, 0, 16, 16, 16)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 3, 7, 2, 13),
                    box(0, 2, 4.75, 5.5, 13.875, 11.25), box(0, 13.875, 0, 16, 16, 16)).optimize())};
    //? if >=1.21 {
    public static final MapCodec<HotelCoffeeTableBlock> CODEC = simpleCodec(HotelCoffeeTableBlock::new);
    //?}

    public HotelCoffeeTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(EXPANDED, false).setValue(PRIMARY, true));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(EXPANDED);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 2 : 1; }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getClockWise(); }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return super.getPlacementState(context).setValue(EXPANDED, true);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(EXPANDED) && !state.getValue(PRIMARY)) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (state.getValue(EXPANDED)) {
            Direction along = getExtensionDirection(state);
            Direction partner = state.getValue(PRIMARY) ? along : along.getOpposite();
            if (side == partner && other != stateForPart(state, 1 - partIndex(state))) return Blocks.AIR.defaultBlockState();
        }
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
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
