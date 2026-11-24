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

/** Wall panels update only the edge touched by a same-facing neighbor on the same plane. */
public final class GymMirrorBlock extends RotatableDecorationBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(0, 0, 14.96, 16, 16, 16));

    public GymMirrorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEFT, RIGHT, UP, DOWN);
    }

    private boolean connects(BlockState neighbor, Direction facing) {
        return neighbor.is(this) && neighbor.getValue(FACING) == facing;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        if (!facing.getAxis().isHorizontal()) return null;
        BlockState state = defaultBlockState().setValue(FACING, facing);
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!canSurvive(state, level, pos)) return null;
        return state.setValue(LEFT, connectsAt(level, pos.relative(facing.getCounterClockWise()), facing))
                .setValue(RIGHT, connectsAt(level, pos.relative(facing.getClockWise()), facing))
                .setValue(UP, connectsAt(level, pos.above(), facing))
                .setValue(DOWN, connectsAt(level, pos.below(), facing));
    }

    private boolean connectsAt(LevelReader level, BlockPos pos, Direction facing) {
        return level.hasChunkAt(pos) && connects(level.getBlockState(pos), facing);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction facing = state.getValue(FACING);
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.hasChunkAt(wall) && level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (side == facing.getCounterClockWise()) return state.setValue(LEFT, connects(neighbor, facing));
        if (side == facing.getClockWise()) return state.setValue(RIGHT, connects(neighbor, facing));
        if (side == Direction.UP) return state.setValue(UP, connects(neighbor, facing));
        if (side == Direction.DOWN) return state.setValue(DOWN, connects(neighbor, facing));
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        Direction facing = state.getValue(FACING);
        return side.getAxis() != facing.getAxis() && connects(neighbor, facing)
                || super.skipRendering(state, neighbor, side);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? mirrored : mirrored.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<GymMirrorBlock> CODEC = simpleCodec(GymMirrorBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
