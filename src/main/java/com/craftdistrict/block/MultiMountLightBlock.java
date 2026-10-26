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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Adds upright placement while keeping the original wall/ceiling state values. */
public final class MultiMountLightBlock extends ExpansionLightBlock {
    public static final BooleanProperty FLOOR_MOUNTED = BooleanProperty.create("floor_mounted");
    //? if >=1.21 {
    public static final MapCodec<MultiMountLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Fixture::values).fieldOf("fixture")
                    .forGetter((MultiMountLightBlock b) -> b.fixture),
            propertiesCodec()).apply(instance, MultiMountLightBlock::new));
    //?}
    private final VoxelShape[] floorShapes;

    public MultiMountLightBlock(Fixture fixture, Properties properties) {
        super(fixture, properties);
        if (fixture != Fixture.BEACON && fixture != Fixture.WARNING) throw new IllegalArgumentException("Unsupported fixture");
        registerDefaultState(defaultBlockState().setValue(FLOOR_MOUNTED, false));
        VoxelShape floor = Shapes.empty();
        for (AABB box : fixture.ceilingShapes[0].toAabbs()) {
            floor = Shapes.or(floor, Shapes.box(box.minX, 1-box.maxY, 1-box.maxZ,
                    box.maxX, 1-box.minY, 1-box.minZ));
        }
        floorShapes = RestaurantKitchenBlock.rotations(floor);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FLOOR_MOUNTED);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.UP) return super.getStateForPlacement(context);
        BlockState state = defaultBlockState().setValue(WALL_MOUNTED, false).setValue(FLOOR_MOUNTED, true)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(FLOOR_MOUNTED)) return super.canSurvive(state, level, pos);
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.getValue(FLOOR_MOUNTED)) return super.updateShape(state, side, neighbor, level, pos, neighborPos);
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FLOOR_MOUNTED) ? floorShapes[RestaurantKitchenBlock.turn(state)]
                : super.getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
