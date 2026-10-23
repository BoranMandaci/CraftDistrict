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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four local joins form square work surfaces; no ticking or arrangement traversal. */
public final class StainlessSteelPrepTableBlock extends RestaurantKitchenBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty FRONT = BooleanProperty.create("front");
    public static final BooleanProperty BACK = BooleanProperty.create("back");
    private static final VoxelShape[][] SHAPES = buildShapes();
    //? if >=1.21 {
    public static final MapCodec<StainlessSteelPrepTableBlock> CODEC = simpleCodec(StainlessSteelPrepTableBlock::new);
    //?}

    public StainlessSteelPrepTableBlock(Properties properties) {
        super(Equipment.PREP_TABLE, properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false)
                .setValue(FRONT, false).setValue(BACK, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEFT, RIGHT, FRONT, BACK);
    }

    private boolean matches(LevelReader level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState neighbor = level.getBlockState(pos);
        return neighbor.is(this);
    }

    BlockState connections(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, matches(level, pos.relative(facing.getCounterClockWise())))
                .setValue(RIGHT, matches(level, pos.relative(facing.getClockWise())))
                .setValue(FRONT, matches(level, pos.relative(facing)))
                .setValue(BACK, matches(level, pos.relative(facing.getOpposite())));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : connections(state, context.getLevel(), context.getClickedPos());
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState supported = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return supported.isAir() || !direction.getAxis().isHorizontal()
                ? supported : connections(state, level, pos);
    }

    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return direction.getAxis().isHorizontal() && neighbor.is(this)
                || super.skipRendering(state, neighbor, direction);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    private static VoxelShape[][] buildShapes() {
        VoxelShape[][] shapes = new VoxelShape[16][];
        for (int mask = 0; mask < 16; mask++) {
            boolean left = (mask & 1) != 0, right = (mask & 2) != 0;
            boolean front = (mask & 4) != 0, back = (mask & 8) != 0;
            VoxelShape shape = Shapes.or(Block.box(0, 12.75, 0, 16, 14.5, 16), Block.box(0, 3.5, 0, 16, 4.25, 16));
            if (!left && !front) shape = Shapes.or(shape, Block.box(1.125, 0, 1.125, 2.625, 13.5, 2.625));
            if (!right && !front) shape = Shapes.or(shape, Block.box(13.375, 0, 1.125, 14.875, 13.5, 2.625));
            if (!left && !back) shape = Shapes.or(shape, Block.box(1.125, 0, 13.375, 2.625, 13.5, 14.875));
            if (!right && !back) shape = Shapes.or(shape, Block.box(13.375, 0, 13.375, 14.875, 13.5, 14.875));
            shapes[mask] = rotations(shape);
        }
        return shapes;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = (state.getValue(LEFT) ? 1 : 0) | (state.getValue(RIGHT) ? 2 : 0)
                | (state.getValue(FRONT) ? 4 : 0) | (state.getValue(BACK) ? 8 : 0);
        return SHAPES[mask][turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
