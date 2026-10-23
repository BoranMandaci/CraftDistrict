package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Fixed two-cell service desk using the established placement and single-drop cleanup. */
public final class RestaurantServiceCounterBlock extends MultiBlockDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<RestaurantServiceCounterBlock> CODEC = simpleCodec(RestaurantServiceCounterBlock::new);
    //?}

    public RestaurantServiceCounterBlock(Properties properties) { super(properties); }

    @Override protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(FACING).getClockWise();
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
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

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
