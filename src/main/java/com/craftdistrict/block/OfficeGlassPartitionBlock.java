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
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four immediate joins, one edge-aligned plane; no component scans or ticking. */
public final class OfficeGlassPartitionBlock extends RotatableDecorationBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right"),
            UP = BooleanProperty.create("up"), DOWN = BooleanProperty.create("down");
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(0, 0, 14.5, 16, 16, 16));
    //? if >=1.21 {
    public static final MapCodec<OfficeGlassPartitionBlock> CODEC = simpleCodec(OfficeGlassPartitionBlock::new);
    //?}
    public OfficeGlassPartitionBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false).setValue(UP, false).setValue(DOWN, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LEFT, RIGHT, UP, DOWN);
    }
    private boolean matches(BlockState state, Direction facing) { return state.is(this) && state.getValue(FACING) == facing; }
    public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, matches(level.getBlockState(pos.relative(facing.getCounterClockWise())), facing))
                .setValue(RIGHT, matches(level.getBlockState(pos.relative(facing.getClockWise())), facing))
                .setValue(UP, matches(level.getBlockState(pos.above()), facing))
                .setValue(DOWN, matches(level.getBlockState(pos.below()), facing));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Match the corrected glass door placement; existing saved facings are unchanged.
        BlockState state = super.getStateForPlacement(context);
        return layout(state, context.getLevel(), context.getClickedPos());
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) { return layout(state, level, pos); }
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
