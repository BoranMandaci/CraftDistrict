package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.UnaryOperator;

/** A mesh panel with extensions to frame tubes and adjoining perpendicular panels. */
public final class FootballGoalNetBlock extends Block {
    private static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    private static final BooleanProperty[] EDGES = {
            BooleanProperty.create("left"), BooleanProperty.create("right"),
            BooleanProperty.create("bottom"), BooleanProperty.create("top")
    };
    private static final List<EnumProperty<Corner>> CORNERS = List.of(
            EnumProperty.create("bottom_left", Corner.class), EnumProperty.create("bottom_right", Corner.class),
            EnumProperty.create("top_left", Corner.class), EnumProperty.create("top_right", Corner.class));
    private static final List<Direction> Z_EDGES = List.of(Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP);
    private static final List<Direction> X_EDGES = List.of(Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP);
    private static final List<Direction> Y_EDGES = List.of(Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH);
    private static final VoxelShape[][] OUTLINES = buildOutlines();

    private enum Corner implements StringRepresentable {
        PLAIN("false"), PATCH("true"), TRIMMED("trimmed");

        private final String name;

        Corner(String name) { this.name = name; }

        // Keep the two original serialized values for panels saved before trimming was added.
        @Override public String getSerializedName() { return name; }
    }

    //? if >=1.21 {
    public static final MapCodec<FootballGoalNetBlock> CODEC = simpleCodec(FootballGoalNetBlock::new);
    @Override protected MapCodec<FootballGoalNetBlock> codec() { return CODEC; }
    //?}

    public FootballGoalNetBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(AXIS, Direction.Axis.Z);
        for (BooleanProperty property : EDGES) state = state.setValue(property, false);
        for (var property : CORNERS) state = state.setValue(property, Corner.PLAIN);
        registerDefaultState(state);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS).add(EDGES);
        for (var property : CORNERS) builder.add(property);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Direction.Axis axis = placementAxis(context, pos);
        return withConnections(defaultBlockState().setValue(AXIS, axis), context.getLevel(), pos);
    }

    private Direction.Axis placementAxis(BlockPlaceContext context, BlockPos pos) {
        var level = context.getLevel();
        Direction face = context.getClickedFace();
        Direction.Axis faceAxis = face.getAxis();
        BlockState clicked = level.getBlockState(pos.relative(face.getOpposite()));
        boolean frame = isFrame(clicked);
        boolean followNeighbors = !context.isSecondaryUseActive();
        if (followNeighbors && clicked.is(this) && clicked.getValue(AXIS) != faceAxis) return clicked.getValue(AXIS);
        Direction.Axis aimed = context.getNearestLookingDirection().getAxis();
        if (!followNeighbors && (!frame || aimed != faceAxis)) return aimed;

        int[] neighbors = new int[3];
        if (followNeighbors) {
            for (Direction side : Direction.values()) {
                BlockState neighbor = level.getBlockState(pos.relative(side));
                if (!neighbor.is(this)) continue;
                Direction.Axis axis = neighbor.getValue(AXIS);
                if (axis != side.getAxis()) neighbors[axis.ordinal()]++;
            }
        }

        Direction.Axis best = aimed;
        int bestScore = -1;
        for (Direction look : context.getNearestLookingDirections()) {
            Direction.Axis axis = look.getAxis();
            if (frame && axis == faceAxis) continue;
            if (!followNeighbors) return axis;
            int score = neighbors[axis.ordinal()] * 2;
            if (frame && followsTube(clicked, faceAxis, axis)) score++;
            if (score > bestScore || (score == bestScore && axis == aimed)) {
                bestScore = score;
                best = axis;
            }
        }
        return best;
    }

    private static boolean followsTube(BlockState frame, Direction.Axis face, Direction.Axis panel) {
        for (Direction side : edges(panel)) {
            if (side.getAxis() != face && frame.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) return true;
        }
        return false;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return withConnections(state, level, pos);
    }

    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction.Axis axis = state.getValue(AXIS);
        List<Direction> edges = edges(axis);
        Direction horizontalCut = cutSide(level, pos, axis, edges.get(0), edges.get(1), edges.get(2).getAxis());
        Direction verticalCut = cutSide(level, pos, axis, edges.get(2), edges.get(3), edges.get(0).getAxis());
        int extendedEdges = 0;
        for (int i = 0; i < EDGES.length; i++) {
            Direction side = edges.get(i);
            BlockState neighbor = level.getBlockState(pos.relative(side));
            boolean extended = side != horizontalCut && side != verticalCut
                    && (isFrame(neighbor) || joinsPerpendicular(neighbor, side));
            if (extended) extendedEdges |= 1 << i;
            state = state.setValue(EDGES[i], extended);
        }
        for (int i = 0; i < CORNERS.size(); i++) {
            int horizontal = i % 2, vertical = 2 + i / 2;
            Direction across = edges.get(horizontal), up = edges.get(vertical);
            if (across == horizontalCut || up == verticalCut) {
                state = state.setValue(CORNERS.get(i), Corner.TRIMMED);
                continue;
            }
            int pair = (1 << horizontal) | (1 << vertical);
            boolean fill = (extendedEdges & pair) == pair
                    && closesCorner(level.getBlockState(pos.relative(across).relative(up)), across, up);
            state = state.setValue(CORNERS.get(i), fill ? Corner.PATCH : Corner.PLAIN);
        }
        return state;
    }

    private Direction cutSide(BlockGetter level, BlockPos pos, Direction.Axis plane, Direction first,
                              Direction second, Direction.Axis otherAxis) {
        Direction.Axis split = first.getAxis();
        Direction contact = meetingPanel(level, pos, plane, split);
        if (contact != null) return outsideHalf(level, pos, plane, first, second, contact);

        // At a three-panel corner, continue the adjoining sheet's cut along their shared edge.
        Direction otherContact = meetingPanel(level, pos, plane, otherAxis);
        if (otherContact == null) return null;
        BlockPos adjoining = pos.relative(otherContact);
        Direction next = meetingPanel(level, adjoining, otherAxis, split);
        if (next == null) return null;
        Direction cut = outsideHalf(level, adjoining, otherAxis, first, second, next);
        return cut != null && !continues(level, pos, plane, cut) ? cut : null;
    }

    private Direction meetingPanel(BlockGetter level, BlockPos pos, Direction.Axis plane, Direction.Axis other) {
        Direction first = Direction.fromAxisAndDirection(plane, Direction.AxisDirection.NEGATIVE);
        BlockState neighbor = level.getBlockState(pos.relative(first));
        if (neighbor.is(this) && neighbor.getValue(AXIS) == other) return first;
        Direction opposite = first.getOpposite();
        neighbor = level.getBlockState(pos.relative(opposite));
        return neighbor.is(this) && neighbor.getValue(AXIS) == other ? opposite : null;
    }

    private Direction outsideHalf(BlockGetter level, BlockPos pos, Direction.Axis plane,
                                  Direction first, Direction second, Direction incoming) {
        boolean firstContinues = continues(level, pos, plane, first);
        boolean secondContinues = continues(level, pos, plane, second);
        if (firstContinues && secondContinues) return null;
        if (firstContinues) return second;
        if (secondContinues) return first;

        // At a free end, let the roof cap the side below it and give bare wall turns a consistent handedness.
        if (first.getAxis() == Direction.Axis.Y) return Direction.UP;
        return incoming.getAxis().isHorizontal() ? incoming.getCounterClockWise() : second;
    }

    private boolean continues(BlockGetter level, BlockPos pos, Direction.Axis plane, Direction side) {
        BlockState neighbor = level.getBlockState(pos.relative(side));
        return isFrame(neighbor) || (neighbor.is(this) && neighbor.getValue(AXIS) == plane)
                || joinsPerpendicular(neighbor, side);
    }

    private boolean joinsPerpendicular(BlockState neighbor, Direction side) {
        // The neighbor's sheet crosses the center of that block, where the extension ends.
        return neighbor.is(this) && neighbor.getValue(AXIS) == side.getAxis();
    }

    private boolean closesCorner(BlockState diagonal, Direction across, Direction up) {
        // A diagonal sheet in this plane already fills the corner itself.
        return isFrame(diagonal) || joinsPerpendicular(diagonal, across) || joinsPerpendicular(diagonal, up);
    }

    private static boolean isFrame(BlockState state) {
        return state.getBlock() instanceof FootballGoalPostBlock;
    }

    private static List<Direction> edges(Direction.Axis axis) {
        return switch (axis) { case X -> X_EDGES; case Y -> Y_EDGES; case Z -> Z_EDGES; };
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return transform(state, rotation::rotate);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return transform(state, mirror::mirror);
    }

    private static BlockState transform(BlockState state, UnaryOperator<Direction> direction) {
        Direction.Axis axis = state.getValue(AXIS);
        Direction.Axis newAxis = direction.apply(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE)).getAxis();
        List<Direction> original = edges(axis), rotated = edges(newAxis);
        BlockState result = state.setValue(AXIS, newAxis);
        int[] slots = new int[EDGES.length];
        for (int i = 0; i < EDGES.length; i++) {
            slots[i] = rotated.indexOf(direction.apply(original.get(i)));
            result = result.setValue(EDGES[slots[i]], state.getValue(EDGES[i]));
        }
        for (int i = 0; i < CORNERS.size(); i++) {
            int a = slots[i % 2], b = slots[2 + i / 2];
            int corner = Math.min(a, b) + 2 * (Math.max(a, b) - 2);
            result = result.setValue(CORNERS.get(corner), state.getValue(CORNERS.get(i)));
        }
        return result;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int visible = 0;
        for (int i = 0; i < CORNERS.size(); i++) {
            if (state.getValue(CORNERS.get(i)) != Corner.TRIMMED) visible |= 1 << i;
        }
        return OUTLINES[state.getValue(AXIS).ordinal()][visible];
    }

    private static VoxelShape[][] buildOutlines() {
        VoxelShape[][] result = new VoxelShape[3][16];
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (int visible = 0; visible < 16; visible++) {
                VoxelShape shape = Shapes.empty();
                for (int i = 0; i < CORNERS.size(); i++) {
                    if ((visible & (1 << i)) == 0) continue;
                    int u = (i % 2) * 8, v = (i / 2) * 8;
                    VoxelShape quadrant = switch (axis) {
                        case X -> box(7.625, v, u, 8.375, v + 8, u + 8);
                        case Y -> box(u, 7.625, 8 - v, u + 8, 8.375, 16 - v);
                        case Z -> box(u, v, 7.625, u + 8, v + 8, 8.375);
                    };
                    shape = Shapes.or(shape, quadrant);
                }
                result[axis.ordinal()][visible] = shape;
            }
        }
        return result;
    }
}
