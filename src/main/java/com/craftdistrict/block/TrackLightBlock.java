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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Both mounting styles use the shared two-cell validation and linked removal. */
public final class TrackLightBlock extends FluorescentLightBlock {
    public static final BooleanProperty WALL_MOUNTED = BooleanProperty.create("wall_mounted");

    //? if >=1.21 {
    public static final MapCodec<TrackLightBlock> CODEC = simpleCodec(TrackLightBlock::new);
    //?}

    public TrackLightBlock(Properties properties) {
        super(properties);
        // Previously placed tracks lack this property and retain their ceiling style.
        registerDefaultState(defaultBlockState().setValue(WALL_MOUNTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WALL_MOUNTED);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face == Direction.DOWN) return super.getPlacementState(context);
        if (!face.getAxis().isHorizontal()) return null;
        return stateForPart(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, face)
                .setValue(WALL_MOUNTED, true), 0);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(WALL_MOUNTED)) return super.canSurvive(state, level, pos);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WALL_MOUNTED)
                && direction == state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()
                && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int layout = state.getValue(WALL_MOUNTED) ? LightingShapes.TRACK_WALL : LightingShapes.TRACK;
        return LightingShapes.get(layout + partIndex(state), state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
