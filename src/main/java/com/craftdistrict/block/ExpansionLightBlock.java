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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Passive fixtures: mount selection only, with ordinary support-loss loot. */
public class ExpansionLightBlock extends RotatableDecorationBlock {
    public static final BooleanProperty WALL_MOUNTED = SurfaceLightBlock.WALL_MOUNTED;

    public enum Fixture implements StringRepresentable {
        BEACON(true, true, Block.box(4.5, 4.5, 10, 11.5, 11.5, 16), Block.box(4.5, 10, 4.5, 11.5, 16, 11.5)),
        FAN(false, true, Shapes.or(Block.box(.25, 11.75, .25, 15.75, 12.75, 15.75), Block.box(5.25, 9.5, 5.25, 10.75, 16, 10.75))),
        SCONCE(true, false, Shapes.or(Block.box(6.5, 4, 15, 9.5, 11, 16), Block.box(1, 4, 9, 15, 12, 15))),
        GARDEN(false, false, Shapes.or(Block.box(7, 0, 7, 9, 5, 9), Block.box(5.25, 5, 5.25, 10.75, 8, 10.75))),
        WARNING(true, true, Block.box(4, 4, 9, 12, 12, 16), Block.box(4, 9, 4, 12, 16, 12));

        final boolean wall, ceiling;
        final VoxelShape[] wallShapes, ceilingShapes;
        Fixture(boolean wall, boolean ceiling, VoxelShape shape) { this(wall, ceiling, shape, shape); }
        Fixture(boolean wall, boolean ceiling, VoxelShape shape, VoxelShape ceilingShape) {
            this.wall = wall; this.ceiling = ceiling;
            wallShapes = RestaurantKitchenBlock.rotations(shape);
            ceilingShapes = RestaurantKitchenBlock.rotations(ceilingShape);
        }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<ExpansionLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Fixture::values).fieldOf("fixture").forGetter((ExpansionLightBlock b) -> b.fixture),
            propertiesCodec()).apply(instance, ExpansionLightBlock::new));
    //?}
    protected final Fixture fixture;

    public ExpansionLightBlock(Fixture fixture, Properties properties) {
        super(properties);
        this.fixture = fixture;
        registerDefaultState(defaultBlockState().setValue(WALL_MOUNTED, fixture.wall));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WALL_MOUNTED);
    }

    private Direction support(BlockState state) {
        return state.getValue(WALL_MOUNTED) ? state.getValue(FACING).getOpposite()
                : fixture.ceiling ? Direction.UP : Direction.DOWN;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        boolean wall = face.getAxis().isHorizontal();
        if (wall ? !fixture.wall : face == Direction.DOWN ? !fixture.ceiling : fixture.wall || fixture.ceiling) return null;
        BlockState state = defaultBlockState().setValue(WALL_MOUNTED, wall)
                .setValue(FACING, wall ? face : context.getHorizontalDirection().getOpposite());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = support(state);
        BlockPos anchor = pos.relative(direction);
        return level.getBlockState(anchor).isFaceSturdy(level, anchor, direction.getOpposite());
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return side == support(state) && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(WALL_MOUNTED) ? fixture.wallShapes : fixture.ceilingShapes)[RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
