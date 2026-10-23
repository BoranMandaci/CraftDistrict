package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Multipart tables: bounded, event-driven layout checks, never a component/world scan. */
public final class ModularDiningTableBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty LEG_NW = BooleanProperty.create("leg_nw");
    public static final BooleanProperty LEG_NE = BooleanProperty.create("leg_ne");
    public static final BooleanProperty LEG_SE = BooleanProperty.create("leg_se");
    public static final BooleanProperty LEG_SW = BooleanProperty.create("leg_sw");
    private static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final BooleanProperty[] JOINS = {NORTH, EAST, SOUTH, WEST};
    private static final BooleanProperty[] LEGS = {LEG_NW, LEG_NE, LEG_SE, LEG_SW};
    private static final VoxelShape[] SHAPES = makeShapes();
    private static final int REACH = 4;
    //? if >=1.21 {
    public static final MapCodec<ModularDiningTableBlock> CODEC = simpleCodec(ModularDiningTableBlock::new);
    //?}

    public ModularDiningTableBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (BooleanProperty property : JOINS) state = state.setValue(property, false);
        for (BooleanProperty property : LEGS) state = state.setValue(property, true);
        registerDefaultState(state);
    }

    private static VoxelShape[] makeShapes() {
        VoxelShape[] shapes = new VoxelShape[16];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = box(0, 13.25, 0, 16, 14.5, 16);
            for (int corner = 0; corner < 4; corner++) {
                if ((mask & 1 << corner) == 0) continue;
                double x = corner == 1 || corner == 2 ? 12.75 : 1.25;
                double z = corner >= 2 ? 12.75 : 1.25;
                shape = Shapes.or(shape, box(x, 0, z, x + 2, 13.25, z + 2));
            }
            shapes[mask] = shape.optimize();
        }
        return shapes;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, LEG_NW, LEG_NE, LEG_SE, LEG_SW);
    }

    private boolean same(BlockGetter level, BlockPos pos, BlockPos removed) {
        // Never load neighboring chunks just to choose a table leg.
        return !pos.equals(removed) && (!(level instanceof LevelReader reader) || reader.hasChunkAt(pos))
                && level.getBlockState(pos).is(this);
    }

    private int boundaryRun(BlockGetter level, BlockPos pos, Direction along, Direction outside, BlockPos removed) {
        for (int step = 1; step <= REACH; step++) {
            BlockPos sample = pos.relative(along, step);
            if (!same(level, sample, removed) || same(level, sample.relative(outside), removed)) return step - 1;
        }
        return REACH;
    }

    private boolean intermediate(BlockGetter level, BlockPos pos, Direction outside, BlockPos removed) {
        Direction negative = outside.getAxis() == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        int before = boundaryRun(level, pos, negative, outside, removed);
        int after = boundaryRun(level, pos, negative.getOpposite(), outside, removed);
        if (before < 2 || after < 2) return false;
        int coordinate = negative == Direction.WEST ? pos.getX() : pos.getZ();
        if (Math.floorMod(coordinate, 3) == 0) return true;
        int length = before + after + 1;
        if (length >= 7) return false; // Every interval of three has a support already.
        // A five/six-cell run may miss the world grid; give it one central pair instead.
        for (int offset = 2 - before; offset <= after - 2; offset++) {
            if (Math.floorMod(coordinate + offset, 3) == 0) return false;
        }
        return before == length / 2;
    }

    BlockState layout(BlockState state, BlockGetter level, BlockPos pos, BlockPos removed) {
        boolean[] joined = new boolean[4];
        boolean[] extra = new boolean[4];
        for (int edge = 0; edge < 4; edge++) {
            joined[edge] = same(level, pos.relative(EDGES[edge]), removed);
            state = state.setValue(JOINS[edge], joined[edge]);
            if (!joined[edge]) extra[edge] = intermediate(level, pos, EDGES[edge], removed);
        }
        for (int corner = 0; corner < 4; corner++) {
            int a = (corner + 3) % 4, b = corner;
            boolean leg = !joined[a] && !joined[b]
                    || joined[a] && joined[b]
                    && !same(level, pos.relative(EDGES[a]).relative(EDGES[b]), removed);
            // Intermediate legs belong only to the negative end of a cell, not both ends.
            if (corner == 0) leg |= extra[0] || extra[3];
            if (corner == 1) leg |= extra[1];
            if (corner == 3) leg |= extra[2];
            state = state.setValue(LEGS[corner], leg);
        }
        return state;
    }

    private void refresh(Level level, BlockPos changed, BlockPos removed) {
        if (level.isClientSide) return;
        // A dependency is at most four cells along an edge and one cell across it.
        for (int x = -REACH; x <= REACH; x++) {
            for (int z = -REACH; z <= REACH; z++) {
                if (Math.abs(x) > 1 && Math.abs(z) > 1) continue;
                BlockPos pos = changed.offset(x, 0, z);
                if (pos.equals(removed) || !level.hasChunkAt(pos)) continue;
                BlockState old = level.getBlockState(pos);
                if (!old.is(this)) continue;
                BlockState next = layout(old, level, pos, removed);
                if (old != next) level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = layout(defaultBlockState(), context.getLevel(), context.getClickedPos(), null);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!old.is(this)) refresh(level, pos, null);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!next.is(this)) refresh(level, pos, pos);
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState adjacent,
                                  LevelAccessor level, BlockPos pos, BlockPos adjacentPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        if (!direction.getAxis().isHorizontal()) return state;
        // Shape/lighting updates do not change occupancy. Diagonal and long-run changes
        // are already covered by the bounded onPlace/onRemove refresh.
        int edge = direction == Direction.NORTH ? 0 : direction == Direction.EAST ? 1 : direction == Direction.SOUTH ? 2 : 3;
        if (state.getValue(JOINS[edge]) == adjacent.is(this)) return state;
        return layout(state, level, pos, null);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (int i = 0; i < 4; i++) if (state.getValue(LEGS[i])) mask |= 1 << i;
        return SHAPES[mask];
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return direction.getAxis().isHorizontal() && adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    private BlockState transform(BlockState state, java.util.function.UnaryOperator<Direction> transform) {
        BlockState result = state;
        for (int i = 0; i < 4; i++) {
            Direction edge = transform.apply(EDGES[i]);
            int next = java.util.Arrays.asList(EDGES).indexOf(edge);
            result = result.setValue(JOINS[next], state.getValue(JOINS[i]));
            Direction a = transform.apply(EDGES[(i + 3) % 4]), b = edge;
            for (int j = 0; j < 4; j++) {
                Direction c = EDGES[(j + 3) % 4], d = EDGES[j];
                if (a == c && b == d || a == d && b == c) result = result.setValue(LEGS[j], state.getValue(LEGS[i]));
            }
        }
        return result;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) { return transform(state, rotation::rotate); }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) { return transform(state, mirror::mirror); }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }
    //?}
}
