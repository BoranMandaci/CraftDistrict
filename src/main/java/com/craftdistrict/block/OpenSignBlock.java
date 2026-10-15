package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Existing clicked-wall mounting, plus a suspended model when clicking a ceiling. */
public final class OpenSignBlock extends BakerySignageBlock {
    public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
    //? if >=1.21 {
    public static final MapCodec<OpenSignBlock> CODEC = simpleCodec(OpenSignBlock::new);
    //?}

    public OpenSignBlock(Properties properties) {
        super(Variant.WALL_SHOP, properties);
        // Existing saved OPEN signs without the property remain wall-mounted.
        registerDefaultState(defaultBlockState().setValue(HANGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HANGING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.DOWN) return super.getStateForPlacement(context);
        BlockState state = defaultBlockState().setValue(HANGING, true)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(HANGING)) return super.canSurvive(state, level, pos);
        BlockPos support = pos.above();
        return level.getBlockState(support).isFaceSturdy(level, support, Direction.DOWN);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.getValue(HANGING)) return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        return direction == Direction.UP && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LightingShapes.get(state.getValue(HANGING) ? LightingShapes.OPEN_HANGING : LightingShapes.NEON, state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
