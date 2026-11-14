package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import com.craftdistrict.entity.Seating;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One seat per two-high column; saved two-seat alcoves keep their four-cell footprint. */
public final class CasinoVipBoothBlock extends WideTallDecorationBlock implements SittableBlock {
    public enum Corner implements StringRepresentable {
        STRAIGHT, LEFT, RIGHT;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final BooleanProperty MODULAR = BooleanProperty.create("modular");
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    public static final EnumProperty<Corner> SHAPE = EnumProperty.create("shape", Corner.class);
    private static final int JOIN_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final VoxelShape[][] LEGACY_SHAPES = createLegacyShapes();
    private static final VoxelShape[][][][] MODULAR_SHAPES = createModularShapes();
    //? if >=1.21 {
    public static final MapCodec<CasinoVipBoothBlock> CODEC = simpleCodec(CasinoVipBoothBlock::new);
    //?}
    public CasinoVipBoothBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(MODULAR, false).setValue(SHAPE, Corner.STRAIGHT)
                .setValue(LEFT, false).setValue(RIGHT, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(MODULAR, SHAPE, LEFT, RIGHT);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(MODULAR) ? 2 : 4; }
    @Override protected int partIndex(BlockState state) {
        return state.getValue(MODULAR) ? state.getValue(PART) / 2 : state.getValue(PART);
    }
    @Override protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PART, state.getValue(MODULAR) ? index * 2 : index);
    }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        return state.getValue(MODULAR) ? BlockPos.ZERO.above(index) : super.partOffset(state, index);
    }
    @Override public double seatHeight() { return 9.25 / 16; }
    @Override public double forwardOffset() { return 1.0 / 16; }
    @Override public boolean isSittable(BlockState state) {
        return state.getValue(MODULAR) ? state.getValue(PART) == 0 : state.getValue(PART) < 2;
    }
    private boolean complete(LevelReader level, BlockPos origin, BlockPos removed) {
        if (origin.equals(removed) || !level.hasChunkAt(origin) || !level.hasChunkAt(origin.above())) return false;
        BlockState a = level.getBlockState(origin), b = level.getBlockState(origin.above());
        return a.is(this) && b.is(this) && a.getValue(MODULAR) && b.getValue(MODULAR)
                && a.getValue(PART) == 0 && b.getValue(PART) == 2 && a.getValue(FACING) == b.getValue(FACING);
    }
    private BlockState matching(LevelReader level, BlockPos origin, BlockPos removed) {
        return complete(level, origin, removed) ? level.getBlockState(origin) : null;
    }
    private boolean aligned(LevelReader level, BlockPos origin, Direction facing, BlockPos removed) {
        BlockState other = matching(level, origin, removed);
        return other != null && other.getValue(FACING) == facing;
    }
    // A perpendicular front neighbor turns an inside lounge corner. Competing branches keep a straight run.
    private Corner corner(LevelReader level, BlockPos origin, Direction facing, BlockPos removed) {
        BlockState front = matching(level, origin.relative(facing), removed);
        if (front != null) {
            Direction other = front.getValue(FACING);
            if (other.getAxis() != facing.getAxis()
                    && !aligned(level, origin.relative(other.getOpposite()), facing, removed))
                return other == facing.getClockWise() ? Corner.LEFT : Corner.RIGHT;
        }
        return Corner.STRAIGHT;
    }
    private static Direction leftPort(Corner corner, Direction facing) {
        return corner == Corner.LEFT ? facing : facing.getCounterClockWise();
    }
    private static Direction rightPort(Corner corner, Direction facing) {
        return corner == Corner.RIGHT ? facing : facing.getClockWise();
    }
    private boolean joins(LevelReader level, BlockPos origin, Direction facing, Direction edge, BlockPos removed) {
        BlockPos next = origin.relative(edge);
        BlockState neighbor = matching(level, next, removed);
        if (neighbor == null) return false;
        Direction other = neighbor.getValue(FACING);
        if (other == facing.getOpposite() || other == facing && edge.getAxis() == facing.getAxis()) return false;
        Corner shape = corner(level, next, other, removed);
        return leftPort(shape, other) == edge.getOpposite() || rightPort(shape, other) == edge.getOpposite();
    }
    private BlockState layout(BlockState state, LevelReader level, BlockPos origin, BlockPos removed) {
        Direction facing = state.getValue(FACING);
        Corner shape = corner(level, origin, facing, removed);
        return state.setValue(SHAPE, shape).setValue(LEFT, joins(level, origin, facing, leftPort(shape, facing), removed))
                .setValue(RIGHT, joins(level, origin, facing, rightPort(shape, facing), removed));
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context).setValue(MODULAR, true).setValue(PART, 0);
        return layout(state, context.getLevel(), context.getClickedPos(), null);
    }
    private void refresh(Level level, BlockPos origin, BlockPos removed) {
        if (!complete(level, origin, removed)) return;
        BlockState lower = level.getBlockState(origin), upper = level.getBlockState(origin.above());
        BlockState joined = layout(lower, level, origin, removed), joinedUpper = stateForPart(joined, 1);
        if (lower != joined) level.setBlock(origin, joined, JOIN_FLAGS);
        if (upper != joinedUpper && level.getBlockState(origin) == joined && level.getBlockState(origin.above()) == upper)
            level.setBlock(origin.above(), joinedUpper, JOIN_FLAGS);
    }
    private void refreshAround(Level level, BlockPos origin, BlockPos removed) {
        // Corner selection uses one neighbor and its ports: a fixed two-cell radius covers every affected unit.
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            if (Math.abs(x) + Math.abs(z) <= 2) refresh(level, origin.offset(x, 0, z), removed);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(MODULAR) && state.getValue(PART) == 0 && level.getBlockState(pos).is(this))
            refreshAround(level, pos, null);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos otherPos, boolean moving) {
        super.neighborChanged(state, level, pos, source, otherPos, moving);
        if (!level.isClientSide && state.getValue(MODULAR) && !isRemovingLinkedStructure(state, level, pos))
            refreshAround(level, structureOrigin(state, pos), null);
    }
    private InteractionResult sit(BlockState state, Level level, BlockPos pos, Player player) {
        int raw = state.getValue(PART);
        BlockPos seat = pos.below(raw / 2);
        BlockState lower = level.getBlockState(seat);
        int target = state.getValue(MODULAR) ? 0 : raw % 2;
        return lower.is(this) && lower.getValue(PART) == target && lower.getValue(MODULAR) == state.getValue(MODULAR)
                && lower.getValue(FACING) == state.getValue(FACING)
                ? Seating.sit(lower, level, seat, player) : InteractionResult.PASS;
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return sit(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? sit(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(MODULAR) && (state.getValue(PART) & 1) != 0) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = pos.below(state.getValue(PART) / 2 + 1);
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int index = 0; index < partCount(state); index++) {
            BlockPos expected = origin.offset(partOffset(state, index));
            if (!expected.equals(otherPos)) continue;
            if (!neighbor.is(this) || neighbor.getValue(FACING) != state.getValue(FACING)
                    || neighbor.getValue(MODULAR) != state.getValue(MODULAR)
                    || neighbor.getValue(PART) != stateForPart(state, index).getValue(PART))
                return Blocks.AIR.defaultBlockState();
        }
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean nested = isRemovingLinkedStructure(state, level, pos);
        BlockPos origin = structureOrigin(state, pos);
        if (!replacement.is(this) && !nested) {
            Seating.removeAt(level, origin);
            if (!state.getValue(MODULAR)) Seating.removeAt(level, origin.relative(state.getValue(FACING).getClockWise()));
        }
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && !nested && !replacement.is(this) && state.getValue(MODULAR))
            refreshAround(level, origin, origin);
    }
    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        if (state.getValue(MODULAR) && neighbor.is(this) && neighbor.getValue(MODULAR)
                && neighbor.getValue(PART) == state.getValue(PART)) {
            Corner a = state.getValue(SHAPE), b = neighbor.getValue(SHAPE);
            Direction f = state.getValue(FACING), nf = neighbor.getValue(FACING);
            boolean ours = state.getValue(LEFT) && leftPort(a, f) == side || state.getValue(RIGHT) && rightPort(a, f) == side;
            boolean theirs = neighbor.getValue(LEFT) && leftPort(b, nf) == side.getOpposite()
                    || neighbor.getValue(RIGHT) && rightPort(b, nf) == side.getOpposite();
            if (ours && theirs) return true;
        }
        return super.skipRendering(state, neighbor, side);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (!state.getValue(MODULAR)) return super.mirror(state, mirror);
        BlockState result = state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
        if (mirror == Mirror.NONE) return result;
        Corner opposite = switch (state.getValue(SHAPE)) {
            case LEFT -> Corner.RIGHT; case RIGHT -> Corner.LEFT; default -> Corner.STRAIGHT;
        };
        return result.setValue(SHAPE, opposite).setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    private static VoxelShape[][] createLegacyShapes() {
        VoxelShape whole = Shapes.or(box(0, 0, 1, 32, 9.25, 16), box(0, 9.25, 12, 32, 26.5, 16),
                box(0, 9.25, 1.5, 2.75, 23.5, 12), box(29.25, 9.25, 1.5, 32, 23.5, 12)).optimize();
        VoxelShape[][] result = new VoxelShape[4][];
        for (int part = 0; part < 4; part++) result[part] = RestaurantKitchenBlock.rotations(
                Shapes.join(whole.move(-(part % 2), -(part / 2), 0), Shapes.block(), BooleanOp.AND).optimize());
        return result;
    }
    private static VoxelShape[][][][] createModularShapes() {
        VoxelShape[][][][] result = new VoxelShape[3][4][2][];
        for (Corner corner : Corner.values()) for (int mask = 0; mask < 4; mask++) {
            VoxelShape whole = Shapes.or(box(0, 0, 0, 16, 9.25, 16), box(0, 9.25, 12.5, 16, 26.5, 16));
            if (corner == Corner.LEFT) whole = Shapes.or(whole, box(0, 9.25, 0, 3.5, 26.5, 16));
            if (corner == Corner.RIGHT) whole = Shapes.or(whole, box(12.5, 9.25, 0, 16, 26.5, 16));
            if ((mask & 1) == 0) whole = Shapes.or(whole, corner == Corner.LEFT
                    ? box(0, 9.25, 0, 16, 20, 2.5) : box(0, 9.25, 0, 2.5, 20, 12.5));
            if ((mask & 2) == 0) whole = Shapes.or(whole, corner == Corner.RIGHT
                    ? box(0, 9.25, 0, 16, 20, 2.5) : box(13.5, 9.25, 0, 16, 20, 12.5));
            for (int part = 0; part < 2; part++) result[corner.ordinal()][mask][part] = RestaurantKitchenBlock.rotations(
                    Shapes.join(whole.move(0, -part, 0), Shapes.block(), BooleanOp.AND).optimize());
        }
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(MODULAR)) return LEGACY_SHAPES[state.getValue(PART)][RestaurantKitchenBlock.turn(state)];
        if ((state.getValue(PART) & 1) != 0) return Shapes.empty();
        int mask = (state.getValue(LEFT) ? 1 : 0) | (state.getValue(RIGHT) ? 2 : 0);
        return MODULAR_SHAPES[state.getValue(SHAPE).ordinal()][mask][state.getValue(PART) / 2][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
