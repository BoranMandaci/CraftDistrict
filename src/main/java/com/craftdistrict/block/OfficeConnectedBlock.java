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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Finished table Ends join at their rear edge; Centers extend the meeting surface. */
public final class OfficeConnectedBlock extends CashRegisterBlock {
    public enum Design implements StringRepresentable {
        CONFERENCE, DIVIDER, CONFERENCE_CENTER;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty FRONT = BooleanProperty.create("front"), BACK = BooleanProperty.create("back");
    public static final BooleanProperty SUPPORT = BooleanProperty.create("support");
    public static final BooleanProperty CENTER_JOIN = BooleanProperty.create("center_join");
    public static final BooleanProperty LEG_NW = BooleanProperty.create("leg_nw"), LEG_NE = BooleanProperty.create("leg_ne");
    public static final BooleanProperty LEG_SE = BooleanProperty.create("leg_se"), LEG_SW = BooleanProperty.create("leg_sw");
    private static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final BooleanProperty[] LEGS = {LEG_NW, LEG_NE, LEG_SE, LEG_SW};
    private static final int SUPPORT_REACH = 4;
    //? if >=1.21 {
    public static final MapCodec<OfficeConnectedBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((OfficeConnectedBlock b) -> b.design),
            propertiesCodec()).apply(instance, OfficeConnectedBlock::new));
    //?}
    private static final VoxelShape[] DIVIDER_SHAPES = RestaurantKitchenBlock.rotations(
            Shapes.or(box(0, 2, 7, 16, 16, 9), box(7, 0, 3, 9, 2, 13)));
    private final Design design;
    private static final VoxelShape[][] CONFERENCE_SHAPES = conferenceShapes();
    public OfficeConnectedBlock(Design design, Properties properties) {
        super(properties); this.design = design;
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false)
                .setValue(FRONT, false).setValue(BACK, false)
                .setValue(SUPPORT, true).setValue(CENTER_JOIN, false)
                .setValue(LEG_NW, true).setValue(LEG_NE, true).setValue(LEG_SE, true).setValue(LEG_SW, true));
    }
    public boolean supportsTabletopProps() { return design != Design.DIVIDER; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LEFT, RIGHT, FRONT, BACK, SUPPORT, CENTER_JOIN, LEG_NW, LEG_NE, LEG_SE, LEG_SW);
    }
    private boolean joins(BlockState state, BlockState other, Direction side) {
        if (design == Design.DIVIDER) return other.is(this) && state.getValue(FACING) == other.getValue(FACING);
        if (!isConference(other)) return false;
        if (design == Design.CONFERENCE && side != state.getValue(FACING).getOpposite()) return false;
        OfficeConnectedBlock neighbor = (OfficeConnectedBlock) other.getBlock();
        return neighbor.design != Design.CONFERENCE || side == other.getValue(FACING);
    }
    private static boolean isConference(BlockState state) {
        return state.getBlock() instanceof OfficeConnectedBlock table && table.design != Design.DIVIDER;
    }
    private boolean connected(BlockState state, LevelReader level, BlockPos pos, Direction side, BlockPos removed) {
        BlockPos neighbor = pos.relative(side);
        return !neighbor.equals(removed) && level.hasChunkAt(neighbor) && joins(state, level.getBlockState(neighbor), side);
    }
    public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        return layout(state, level, pos, null);
    }
    BlockState layout(BlockState state, LevelReader level, BlockPos pos, BlockPos removed) {
        Direction facing = state.getValue(FACING);
        BlockPos a = pos.relative(facing.getCounterClockWise()), b = pos.relative(facing.getClockWise());
        state = state.setValue(LEFT, connected(state, level, pos, facing.getCounterClockWise(), removed))
                .setValue(RIGHT, connected(state, level, pos, facing.getClockWise(), removed));
        if (design != Design.DIVIDER) {
            BlockPos front = pos.relative(facing), back = pos.relative(facing.getOpposite());
            state = state.setValue(FRONT, connected(state, level, pos, facing, removed))
                    .setValue(BACK, connected(state, level, pos, facing.getOpposite(), removed));
            boolean centerJoin = state.getValue(BACK) && level.getBlockState(back).getBlock() instanceof OfficeConnectedBlock neighbor
                    && neighbor.design == Design.CONFERENCE_CENTER;
            state = state.setValue(CENTER_JOIN, design == Design.CONFERENCE && centerJoin);
            boolean[] joined = new boolean[4], extra = new boolean[4];
            for (int edge = 0; edge < 4; edge++) {
                joined[edge] = connected(state, level, pos, EDGES[edge], removed);
                if (!joined[edge]) extra[edge] = intermediate(state, level, pos, EDGES[edge], removed);
            }
            boolean support = false;
            for (int corner = 0; corner < 4; corner++) {
                int previous = (corner + 3) % 4;
                boolean leg = !joined[previous] && !joined[corner]
                        || joined[previous] && joined[corner]
                        && !same(level, pos.relative(EDGES[previous]).relative(EDGES[corner]), removed);
                if (corner == 0) leg |= extra[0] || extra[3];
                if (corner == 1) leg |= extra[1];
                if (corner == 3) leg |= extra[2];
                state = state.setValue(LEGS[corner], leg);
                support |= leg;
            }
            // Both End and Center sections share Restaurant-style support spacing.
            // Broad conference layouts also need sparse supports inside the surface.
            boolean interior = joined[0] && joined[1] && joined[2] && joined[3];
            support |= interior && Math.floorMod(pos.getX(), 3) == 0 && Math.floorMod(pos.getZ(), 3) == 0;
            state = state.setValue(SUPPORT, design == Design.CONFERENCE || support);
        }
        return state;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : layout(state, context.getLevel(), context.getClickedPos());
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        BlockState supported = super.updateShape(state, side, neighbor, level, pos, otherPos);
        if (supported.isAir()) return supported;
        if (design != Design.DIVIDER && side.getAxis().isHorizontal()) {
            Direction facing = state.getValue(FACING);
            BooleanProperty edge = side == facing ? FRONT : side == facing.getOpposite() ? BACK
                    : side == facing.getCounterClockWise() ? LEFT : RIGHT;
            boolean join = level.hasChunkAt(otherPos) && joins(state, neighbor, side);
            boolean centerChanged = design == Design.CONFERENCE && side == facing.getOpposite()
                    && supported.getValue(CENTER_JOIN) != (join && neighbor.getBlock() instanceof OfficeConnectedBlock other
                    && other.design == Design.CONFERENCE_CENTER);
            if (supported.getValue(edge) == join && !centerChanged) return supported;
            return layout(supported, level, pos);
        }
        Direction facing = state.getValue(FACING);
        if (side == facing.getCounterClockWise()) return supported.setValue(LEFT, level.hasChunkAt(otherPos) && joins(state, neighbor, side));
        if (side == facing.getClockWise()) return supported.setValue(RIGHT, level.hasChunkAt(otherPos) && joins(state, neighbor, side));
        return supported;
    }
    @Override public boolean skipRendering(BlockState state, BlockState other, Direction side) {
        Direction facing = state.getValue(FACING);
        return joins(state, other, side) && (design != Design.DIVIDER ? side.getAxis().isHorizontal()
                : side == facing.getClockWise() || side == facing.getCounterClockWise())
                || super.skipRendering(state, other, side);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        if (mirror != Mirror.NONE) result = result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
        return transformLegs(state, result, mirror::mirror);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return transformLegs(state, super.rotate(state, rotation), rotation::rotate);
    }
    private BlockState transformLegs(BlockState state, BlockState result, java.util.function.UnaryOperator<Direction> transform) {
        for (int i = 0; i < 4; i++) {
            Direction a = transform.apply(EDGES[(i + 3) % 4]), b = transform.apply(EDGES[i]);
            for (int j = 0; j < 4; j++) {
                Direction c = EDGES[(j + 3) % 4], d = EDGES[j];
                if (a == c && b == d || a == d && b == c) result = result.setValue(LEGS[j], state.getValue(LEGS[i]));
            }
        }
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (design != Design.DIVIDER) {
            int mask = (state.getValue(LEFT) ? 1 : 0) | (state.getValue(RIGHT) ? 2 : 0)
                    | (state.getValue(FRONT) ? 4 : 0) | (state.getValue(BACK) ? 8 : 0)
                    | (state.getValue(SUPPORT) ? 16 : 0);
            return CONFERENCE_SHAPES[mask][RestaurantKitchenBlock.turn(state)];
        }
        return DIVIDER_SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    private static VoxelShape[][] conferenceShapes() {
        VoxelShape[][] shapes = new VoxelShape[32][];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = Shapes.or(box(0,13.25,0,16,14.5,16),
                    box(5.5,12.75,5.5,10.5,13.25,10.5));
            if ((mask & 1) != 0) shape = Shapes.or(shape,box(0,12.75,7,5.5,13.25,9));
            if ((mask & 2) != 0) shape = Shapes.or(shape,box(10.5,12.75,7,16,13.25,9));
            if ((mask & 4) != 0) shape = Shapes.or(shape,box(7,12.75,0,9,13.25,5.5));
            if ((mask & 8) != 0) shape = Shapes.or(shape,box(7,12.75,10.5,9,13.25,16));
            if ((mask & 16) != 0) shape = Shapes.or(shape,box(6.25,0,6.25,9.75,12.75,9.75));
            shapes[mask] = RestaurantKitchenBlock.rotations(shape.optimize());
        }
        return shapes;
    }

    private boolean same(LevelReader level, BlockPos pos, BlockPos removed) {
        return !pos.equals(removed) && level.hasChunkAt(pos) && isConference(level.getBlockState(pos));
    }
    private int boundaryRun(BlockState state, LevelReader level, BlockPos pos, Direction along, Direction outside, BlockPos removed) {
        BlockState previous = state;
        for (int step = 1; step <= SUPPORT_REACH; step++) {
            BlockPos sample = pos.relative(along, step);
            if (!same(level, sample, removed)) return step - 1;
            BlockState next = level.getBlockState(sample);
            OfficeConnectedBlock owner = (OfficeConnectedBlock) previous.getBlock();
            if (!owner.joins(previous, next, along)
                    || ((OfficeConnectedBlock) next.getBlock()).connected(next, level, sample, outside, removed)) return step - 1;
            previous = next;
        }
        return SUPPORT_REACH;
    }
    private boolean intermediate(BlockState state, LevelReader level, BlockPos pos, Direction outside, BlockPos removed) {
        Direction negative = outside.getAxis() == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
        int before = boundaryRun(state, level, pos, negative, outside, removed);
        int after = boundaryRun(state, level, pos, negative.getOpposite(), outside, removed);
        if (before < 2 || after < 2) return false;
        int coordinate = negative == Direction.WEST ? pos.getX() : pos.getZ();
        if (Math.floorMod(coordinate, 3) == 0) return true;
        int length = before + after + 1;
        if (length >= 7) return false;
        for (int offset = 2 - before; offset <= after - 2; offset++) {
            if (Math.floorMod(coordinate + offset, 3) == 0) return false;
        }
        return before == length / 2;
    }
    private void refresh(Level level, BlockPos changed, BlockPos removed) {
        if (level.isClientSide || design == Design.DIVIDER) return;
        // Same bounded edge dependency as Restaurant tables; no world/component scan.
        for (int x = -SUPPORT_REACH; x <= SUPPORT_REACH; x++) {
            for (int z = -SUPPORT_REACH; z <= SUPPORT_REACH; z++) {
                if (Math.abs(x) > 1 && Math.abs(z) > 1) continue;
                BlockPos pos = changed.offset(x, 0, z);
                if (pos.equals(removed) || !level.hasChunkAt(pos)) continue;
                BlockState old = level.getBlockState(pos);
                if (!isConference(old)) continue;
                BlockState next = ((OfficeConnectedBlock) old.getBlock()).layout(old, level, pos, removed);
                if (old != next) level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!old.is(this)) refresh(level, pos, null);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!next.is(this)) refresh(level, pos, pos);
        super.onRemove(state, level, pos, next, moving);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
