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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Aligned neighbors hide internal end panels; only the changed edge is refreshed. */
public final class NurseStationCounterBlock extends CashRegisterBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    //? if >=1.21 {
    public static final MapCodec<NurseStationCounterBlock> CODEC = simpleCodec(NurseStationCounterBlock::new);
    //?}
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(0, 0, 0, 16, 13.25, 16), box(3.5, 13.25, 4, 13.75, 19.25, 14)).optimize());
    public NurseStationCounterBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LEFT, RIGHT);
    }
    private boolean matches(BlockState state, BlockState neighbor) {
        return neighbor.is(this) && neighbor.getValue(FACING) == state.getValue(FACING);
    }
    private boolean connected(BlockState state, LevelReader level, BlockPos pos) {
        return level.hasChunkAt(pos) && matches(state, level.getBlockState(pos));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, connected(state, context.getLevel(), context.getClickedPos().relative(facing.getCounterClockWise())))
                .setValue(RIGHT, connected(state, context.getLevel(), context.getClickedPos().relative(facing.getClockWise())));
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        BlockState result = super.updateShape(state, side, neighbor, level, pos, otherPos);
        if (result.isAir()) return result;
        Direction facing = state.getValue(FACING);
        if (side == facing.getCounterClockWise()) return result.setValue(LEFT, level.hasChunkAt(otherPos) && matches(state, neighbor));
        if (side == facing.getClockWise()) return result.setValue(RIGHT, level.hasChunkAt(otherPos) && matches(state, neighbor));
        return result;
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
