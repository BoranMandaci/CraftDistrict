package com.craftdistrict.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Neighbor joins for floor-mounted gym racks. */
abstract class GymRackBlock extends RotatableDecorationBlock {
    private static final BooleanProperty LEFT = BooleanProperty.create("left");
    private static final BooleanProperty RIGHT = BooleanProperty.create("right");
    private final VoxelShape[][] shapes;

    GymRackBlock(Properties properties, VoxelShape[][] shapes) {
        super(properties);
        this.shapes = shapes;
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEFT, RIGHT);
    }

    private boolean connects(BlockState neighbor, Direction facing) {
        return neighbor.is(this) && neighbor.getValue(FACING) == facing;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Direction facing = state.getValue(FACING);
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!canSurvive(state, level, pos)) return null;
        BlockPos left = pos.relative(facing.getCounterClockWise());
        BlockPos right = pos.relative(facing.getClockWise());
        return state.setValue(LEFT, level.hasChunkAt(left) && connects(level.getBlockState(left), facing))
                .setValue(RIGHT, level.hasChunkAt(right) && connects(level.getBlockState(right), facing));
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return DecorationSupport.isPlaced(state, level, pos) || GymEquipmentBlock.supported(level, pos.below(), Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (side == facing.getCounterClockWise()) return state.setValue(LEFT, connects(neighbor, facing));
        if (side == facing.getClockWise()) return state.setValue(RIGHT, connects(neighbor, facing));
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        Direction facing = state.getValue(FACING);
        return side.getAxis().isHorizontal() && side.getAxis() != facing.getAxis() && connects(neighbor, facing)
                || super.skipRendering(state, neighbor, side);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? mirrored : mirrored.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int joins = (state.getValue(LEFT) ? 1 : 0) | (state.getValue(RIGHT) ? 2 : 0);
        return shapes[joins][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
