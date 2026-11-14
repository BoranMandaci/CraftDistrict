package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-high fence-style screens: four local joins, no row traversal or ticking. */
public final class CasinoVipDividerBlock extends MultiBlockDecorationBlock {
    public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 15);
    private static final int JOIN_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final VoxelShape[][][] SHAPES = createShapes();
    //? if >=1.21 {
    public static final MapCodec<CasinoVipDividerBlock> CODEC = simpleCodec(CasinoVipDividerBlock::new);
    //?}
    public CasinoVipDividerBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(CONNECTIONS, 0));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(CONNECTIONS);
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    private static Direction direction(Direction facing, int bit) {
        return switch (bit) { case 0 -> facing.getCounterClockWise(); case 1 -> facing.getClockWise();
            case 2 -> facing; default -> facing.getOpposite(); };
    }
    private boolean complete(LevelReader level, BlockPos origin) {
        if (!level.hasChunkAt(origin) || !level.hasChunkAt(origin.above())) return false;
        BlockState a = level.getBlockState(origin), b = level.getBlockState(origin.above());
        return a.is(this) && b.is(this) && a.getValue(PRIMARY) && !b.getValue(PRIMARY)
                && a.getValue(FACING) == b.getValue(FACING);
    }
    private BlockState layout(BlockState state, LevelReader level, BlockPos origin) {
        int mask = 0;
        for (int bit = 0; bit < 4; bit++)
            if (complete(level, origin.relative(direction(state.getValue(FACING), bit)))) mask |= 1 << bit;
        return state.setValue(CONNECTIONS, mask);
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : layout(state, context.getLevel(), context.getClickedPos());
    }
    private void refresh(Level level, BlockPos origin) {
        if (!complete(level, origin)) return;
        BlockState lower = level.getBlockState(origin), upper = level.getBlockState(origin.above());
        BlockState joined = layout(lower, level, origin), joinedUpper = stateForPart(joined, 1);
        if (lower != joined) level.setBlock(origin, joined, JOIN_FLAGS);
        if (upper != joinedUpper && level.getBlockState(origin) == joined && level.getBlockState(origin.above()) == upper)
            level.setBlock(origin.above(), joinedUpper, JOIN_FLAGS);
    }
    private void refreshArea(Level level, BlockPos origin) {
        refresh(level, origin);
        for (Direction side : Direction.Plane.HORIZONTAL) refresh(level, origin.relative(side));
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(PRIMARY) && level.getBlockState(pos).is(this)) refreshArea(level, pos);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos otherPos, boolean moving) {
        super.neighborChanged(state, level, pos, source, otherPos, moving);
        if (!level.isClientSide && !isRemovingLinkedStructure(state, level, pos)) refresh(level, structureOrigin(state, pos));
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && (!neighbor.is(this) || neighbor.getValue(PRIMARY) == state.getValue(PRIMARY)
                || neighbor.getValue(FACING) != state.getValue(FACING))) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean nested = isRemovingLinkedStructure(state, level, pos);
        BlockPos origin = structureOrigin(state, pos);
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && !nested && !replacement.is(this)) refreshArea(level, origin);
    }
    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        if (side.getAxis().isHorizontal() && neighbor.is(this) && neighbor.getValue(PRIMARY) == state.getValue(PRIMARY)) {
            for (int bit = 0; bit < 4; bit++)
                if (direction(state.getValue(FACING), bit) == side && (state.getValue(CONNECTIONS) & 1 << bit) != 0) return true;
        }
        return super.skipRendering(state, neighbor, side);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        int mask = state.getValue(CONNECTIONS);
        return mirror == Mirror.NONE ? result : result.setValue(CONNECTIONS, mask & 12 | (mask & 1) << 1 | (mask & 2) >> 1);
    }
    private static VoxelShape[][][] createShapes() {
        VoxelShape[][][] result = new VoxelShape[16][2][];
        for (int mask = 0; mask < 16; mask++) {
            int arms = mask == 0 ? 3 : mask;
            VoxelShape whole = box(6.9, 0, 6.9, 9.1, 32, 9.1);
            if ((arms & 1) != 0) whole = Shapes.or(whole, box(0, 0, 6.9, 7.1, 31.75, 9.1));
            if ((arms & 2) != 0) whole = Shapes.or(whole, box(8.9, 0, 6.9, 16, 31.75, 9.1));
            if ((arms & 4) != 0) whole = Shapes.or(whole, box(6.9, 0, 0, 9.1, 31.75, 7.1));
            if ((arms & 8) != 0) whole = Shapes.or(whole, box(6.9, 0, 8.9, 9.1, 31.75, 16));
            for (int part = 0; part < 2; part++) result[mask][part] = RestaurantKitchenBlock.rotations(
                    Shapes.join(whole.move(0, -part, 0), Shapes.block(), BooleanOp.AND).optimize());
        }
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(CONNECTIONS)][partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
