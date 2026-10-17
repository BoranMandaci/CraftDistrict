package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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

/** Panel and strip lights choose the mounting model from the clicked surface. */
public class SurfaceLightBlock extends CeilingLightBlock {
    public static final BooleanProperty WALL_MOUNTED = BooleanProperty.create("wall_mounted");

    //? if >=1.21 {
    public static final MapCodec<SurfaceLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Fixture::values).fieldOf("fixture")
                    .forGetter((SurfaceLightBlock block) -> block.surfaceFixture),
            propertiesCodec()).apply(instance, SurfaceLightBlock::new));
    //?}

    private final Fixture surfaceFixture;
    private final int wallShape;

    public SurfaceLightBlock(Fixture fixture, Properties properties) {
        super(fixture, properties);
        if (fixture != Fixture.PANEL && fixture != Fixture.STRIP) {
            throw new IllegalArgumentException("Only panels and strips support wall mounting");
        }
        surfaceFixture = fixture;
        wallShape = fixture == Fixture.PANEL ? LightingShapes.PANEL_WALL : LightingShapes.STRIP_WALL;
        // Existing states without the new property retain their ceiling mount.
        registerDefaultState(defaultBlockState().setValue(WALL_MOUNTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WALL_MOUNTED);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(WALL_MOUNTED)) return super.canSurvive(state, level, pos);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face == Direction.DOWN) return super.getStateForPlacement(context);
        if (!face.getAxis().isHorizontal()) return null;
        BlockState state = defaultBlockState().setValue(WALL_MOUNTED, true)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, face);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.getValue(WALL_MOUNTED)) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
        Direction support = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        return direction == support && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(WALL_MOUNTED) ? LightingShapes.get(wallShape, state)
                : super.getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
