package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One seat per block, with finished ends and no doubled supports at internal joins. */
public final class HotelFootBenchBlock extends HotelRoomSeatBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(0, 0, 1.5, 16, 8.5, 14.5));
    //? if >=1.21 {
    public static final MapCodec<HotelFootBenchBlock> CODEC = simpleCodec(HotelFootBenchBlock::new);
    //?}

    public HotelFootBenchBlock(Properties properties) {
        super(true, properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LEFT, RIGHT);
    }
    private boolean matches(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(FACING) == state.getValue(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        BlockPos left = context.getClickedPos().relative(state.getValue(FACING).getCounterClockWise());
        BlockPos right = context.getClickedPos().relative(state.getValue(FACING).getClockWise());
        return state.setValue(LEFT, context.getLevel().hasChunkAt(left) && matches(state, context.getLevel().getBlockState(left)))
                .setValue(RIGHT, context.getLevel().hasChunkAt(right) && matches(state, context.getLevel().getBlockState(right)));
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                            BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getCounterClockWise()) return state.setValue(LEFT, matches(state, neighbor));
        if (direction == state.getValue(FACING).getClockWise()) return state.setValue(RIGHT, matches(state, neighbor));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
