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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Single-cell lights attached to a sturdy downward-facing ceiling surface. */
public final class CeilingLightBlock extends RotatableDecorationBlock {
    public enum Fixture implements StringRepresentable {
        PANEL(0), PENDANT(1), SPOTLIGHT(LightingShapes.SPOTLIGHT),
        EMERGENCY(LightingShapes.EMERGENCY), STRIP(LightingShapes.STRIP);

        private final int shape;

        Fixture(int shape) { this.shape = shape; }

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<CeilingLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Fixture::values).fieldOf("fixture")
                    .forGetter((CeilingLightBlock block) -> block.fixture),
            propertiesCodec()).apply(instance, CeilingLightBlock::new));
    //?}

    private final Fixture fixture;

    public CeilingLightBlock(Fixture fixture, Properties properties) {
        super(properties);
        this.fixture = fixture;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos ceiling = pos.above();
        return level.getBlockState(ceiling).isFaceSturdy(level, ceiling, Direction.DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.DOWN) return null;
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LightingShapes.get(fixture.shape, state);
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
