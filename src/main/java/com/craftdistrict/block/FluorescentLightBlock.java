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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One continuous two-cell ceiling fixture with the existing linked removal rules. */
public class FluorescentLightBlock extends MultiBlockDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<FluorescentLightBlock> CODEC = simpleCodec(FluorescentLightBlock::new);
    //?}

    public FluorescentLightBlock(Properties properties) { super(properties); }

    @Override
    protected Direction getPlacementFacing(BlockPlaceContext context) {
        return context.getClickedFace() == Direction.DOWN ? super.getPlacementFacing(context) : null;
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos ceiling = pos.above();
        return level.getBlockState(ceiling).isFaceSturdy(level, ceiling, Direction.DOWN);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LightingShapes.get(5 + partIndex(state), state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
