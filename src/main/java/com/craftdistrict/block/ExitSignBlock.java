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

/** One item per arrow direction; the clicked surface chooses the mounting style. */
public final class ExitSignBlock extends RotatableDecorationBlock {
    public static final BooleanProperty HANGING = BooleanProperty.create("hanging");

    public enum Arrow implements StringRepresentable {
        RIGHT, LEFT;

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<ExitSignBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Arrow::values).fieldOf("arrow")
                    .forGetter((ExitSignBlock block) -> block.arrow),
            propertiesCodec()).apply(instance, ExitSignBlock::new));
    //?}

    private final Arrow arrow;

    public ExitSignBlock(Arrow arrow, Properties properties) {
        super(properties);
        this.arrow = arrow;
        // Old palettes contain only facing: the new property defaults to wall mounting.
        registerDefaultState(defaultBlockState().setValue(HANGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HANGING);
    }

    private Direction supportDirection(BlockState state) {
        return state.getValue(HANGING) ? Direction.UP
                : state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction support = supportDirection(state);
        BlockPos supportPos = pos.relative(support);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, support.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockState state;
        if (face == Direction.DOWN) {
            state = super.getStateForPlacement(context).setValue(HANGING, true);
        } else if (face.getAxis().isHorizontal()) {
            state = defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, face);
        } else {
            return null;
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == supportDirection(state) && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LightingShapes.get(state.getValue(HANGING) ? 7 : 4, state);
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
