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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Local, acyclic connections: horizontal presence plus the immediately supporting panel. */
public final class AcousticDividerBlock extends RotatableDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<AcousticDividerBlock> CODEC = simpleCodec(AcousticDividerBlock::new);
    //?}
    public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 15);
    public static final BooleanProperty UP = BooleanProperty.create("up"), DOWN = BooleanProperty.create("down");
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final VoxelShape[][] SHAPES = buildShapes();

    public AcousticDividerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CONNECTIONS, 0).setValue(UP, false).setValue(DOWN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(CONNECTIONS, UP, DOWN);
    }

    public static int bit(Direction direction) {
        return switch (direction) { case NORTH -> 1; case EAST -> 2; case SOUTH -> 4; case WEST -> 8; default -> 0; };
    }

    public static int panelMask(BlockState state) {
        int mask = state.getValue(CONNECTIONS);
        return mask == 0 ? (state.getValue(FACING).getAxis() == Direction.Axis.Z ? 10 : 5) : mask;
    }

    /** Feet brace across a straight panel, or across the bisector of a corner. */
    public static int footAngle(int mask) {
        return switch (mask) {
            case 3, 12 -> 45;
            case 6, 9 -> -45;
            case 1, 4, 5, 7, 13 -> 90;
            default -> 0;
        };
    }

    private static VoxelShape footShape(int mask) {
        int angle = footAngle(mask);
        if (angle == 90) return Shapes.or(box(5, 0, 6.5, 11, .5, 9.5),
                box(7.5, .5, 7.5, 8.5, 3, 8.5));
        if (angle == 0) return Shapes.or(box(6.5, 0, 5, 9.5, .5, 11),
                box(7.5, .5, 7.5, 8.5, 3, 8.5));
        // One coarse bound per diagonal foot/stem keeps selection/collision lightweight.
        double half = 4.5 / Math.sqrt(2), stem = Math.sqrt(.5);
        return Shapes.or(box(8-half, 0, 8-half, 8+half, .5, 8+half),
                box(8-stem, .5, 8-stem, 8+stem, 3, 8+stem));
    }

    public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        int mask = 0;
        for (Direction direction : SIDES) {
            BlockPos other = pos.relative(direction);
            if (level.hasChunkAt(other) && level.getBlockState(other).is(this)) mask |= bit(direction);
        }
        BlockState below = level.getBlockState(pos.below());
        boolean down = below.is(this);
        // Carry the supporting profile upward so isolated upper corner panels stay aligned.
        if (down) mask |= panelMask(below);
        if (mask == 0) mask = state.getValue(FACING).getAxis() == Direction.Axis.Z ? 10 : 5;
        else if (Integer.bitCount(mask) == 1) mask |= ((mask << 2) | (mask >> 2)) & 15;
        return state.setValue(CONNECTIONS, mask).setValue(DOWN, down)
                .setValue(UP, level.getBlockState(pos.above()).is(this));
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.below();
        BlockState below = level.getBlockState(support);
        return below.is(this) || below.isFaceSturdy(level, support, Direction.UP);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && canSurvive(state, context.getLevel(), context.getClickedPos())
                ? layout(state, context.getLevel(), context.getClickedPos()) : null;
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                           LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        // No writes, scans, scheduled ticks, or recursive neighbor notification here.
        return layout(state, level, pos);
    }

    @Override public boolean skipRendering(BlockState state, BlockState other, Direction side) {
        if (!other.is(this)) return super.skipRendering(state, other, side);
        int mask = panelMask(state), next = panelMask(other);
        if (side.getAxis().isVertical()) return mask == next;
        return state.getValue(DOWN) == other.getValue(DOWN)
                && (mask & bit(side)) != 0 && (next & bit(side.getOpposite())) != 0;
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        int mask = 0;
        for (Direction side : SIDES) if ((state.getValue(CONNECTIONS) & bit(side)) != 0) mask |= bit(rotation.rotate(side));
        return super.rotate(state, rotation).setValue(CONNECTIONS, mask);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        int mask = 0;
        for (Direction side : SIDES) if ((state.getValue(CONNECTIONS) & bit(side)) != 0) mask |= bit(mirror.mirror(side));
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(CONNECTIONS, mask);
    }

    private static VoxelShape[][] buildShapes() {
        VoxelShape[][] shapes = new VoxelShape[16][2];
        for (int mask = 0; mask < 16; mask++) for (int down = 0; down < 2; down++) {
            double bottom = down == 1 ? 0 : 3;
            VoxelShape shape = box(7, bottom, 7, 9, 16, 9);
            if ((mask & 1) != 0) shape = Shapes.or(shape, box(7, bottom, 0, 9, 16, 7));
            if ((mask & 2) != 0) shape = Shapes.or(shape, box(9, bottom, 7, 16, 16, 9));
            if ((mask & 4) != 0) shape = Shapes.or(shape, box(7, bottom, 9, 9, 16, 16));
            if ((mask & 8) != 0) shape = Shapes.or(shape, box(0, bottom, 7, 7, 16, 9));
            if (down == 0) shape = Shapes.or(shape, footShape(mask));
            shapes[mask][down] = shape.optimize();
        }
        return shapes;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[panelMask(state)][state.getValue(DOWN) ? 1 : 0];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
