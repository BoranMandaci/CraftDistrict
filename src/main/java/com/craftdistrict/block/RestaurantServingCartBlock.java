package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A single fixed two-cell trolley, sharing the standard validated footprint and removal lifecycle. */
public final class RestaurantServingCartBlock extends MultiBlockDecorationBlock {
    private static final VoxelShape[][] SHAPES = createShapes();
    //? if >=1.21 {
    public static final MapCodec<RestaurantServingCartBlock> CODEC = simpleCodec(RestaurantServingCartBlock::new);
    //?}

    public RestaurantServingCartBlock(Properties properties) { super(properties); }

    @Override protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(FACING).getClockWise();
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return side == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    private static VoxelShape[][] createShapes() {
        VoxelShape[][] result = new VoxelShape[2][];
        for (int part = 0; part < 2; part++) {
            double start = part == 0 ? 1 : 0, end = part == 0 ? 16 : 15;
            double outer = part == 0 ? .5 : 13.5;
            VoxelShape shape = Shapes.or(Block.box(start, 3, 1.5, end, 3.5, 14.5),
                    Block.box(start, 7.5, 1.5, end, 8, 14.5), Block.box(start, 12, 1.5, end, 12.5, 14.5),
                    Block.box(3, 3.5, 4, 13, 6, 12), Block.box(3, 8, 4, 13, 11, 12),
                    Block.box(3, 12.5, 4, 13, 16, 12), Block.box(outer, 0, 1.5, outer + 2, 17, 14.5));
            result[part] = RestaurantKitchenBlock.rotations(shape);
        }
        return result;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
