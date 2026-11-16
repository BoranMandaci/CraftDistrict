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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Six local module boundaries; the shared two-cell lifecycle still owns placement and removal. */
public final class StockRackBlock extends MultiBlockDecorationBlock {
    // Keep the original saved property names; new depth connections default to disconnected.
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty FRONT = BooleanProperty.create("front"), BACK = BooleanProperty.create("back");
    public static final BooleanProperty UP = BlockStateProperties.UP, DOWN = BlockStateProperties.DOWN;
    private static final VoxelShape SHAPE = Shapes.block();
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    //? if >=1.21 {
    public static final MapCodec<StockRackBlock> CODEC = simpleCodec(StockRackBlock::new);
    //?}
    public StockRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false)
                .setValue(FRONT, false).setValue(BACK, false).setValue(UP, false).setValue(DOWN, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(LEFT, RIGHT, FRONT, BACK, UP, DOWN);
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }

    private Direction moduleFacing(LevelReader level, BlockPos origin) {
        if (!level.hasChunkAt(origin) || !level.hasChunkAt(origin.above())) return null;
        BlockState lower = level.getBlockState(origin), upper = level.getBlockState(origin.above());
        return lower.is(this) && lower.getValue(PRIMARY) && upper.is(this) && !upper.getValue(PRIMARY)
                && lower.getValue(FACING) == upper.getValue(FACING) ? lower.getValue(FACING) : null;
    }
    private boolean connects(LevelReader level, BlockPos origin) {
        // The square frame is compatible in every horizontal orientation; facing only rotates its local edges/stock.
        return moduleFacing(level, origin) != null;
    }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        Level level = context.getLevel(); BlockPos origin = context.getClickedPos();
        // A growing stack must inherit its base, not depend on which side the player looks from.
        Direction below = moduleFacing(level, origin.below(2));
        if (below != null) return below;
        Direction above = moduleFacing(level, origin.above(2));
        if (above != null) return above;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            Direction neighbor = moduleFacing(level, origin.relative(side));
            if (neighbor != null) return neighbor;
        }
        return super.getPlacementFacing(context);
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return layout(super.getPlacementState(context), context.getLevel(), context.getClickedPos());
    }
    public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos origin = structureOrigin(state, pos); Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, connects(level, origin.relative(facing.getCounterClockWise())))
                .setValue(RIGHT, connects(level, origin.relative(facing.getClockWise())))
                .setValue(FRONT, connects(level, origin.relative(facing)))
                .setValue(BACK, connects(level, origin.relative(facing.getOpposite())))
                .setValue(UP, connects(level, origin.above(2)))
                .setValue(DOWN, connects(level, origin.below(2)));
    }
    private void refresh(Level level, BlockPos origin) {
        if (level.isClientSide || moduleFacing(level, origin) == null) return;
        BlockState lower = level.getBlockState(origin), upper = level.getBlockState(origin.above());
        BlockState next = layout(lower, level, origin), nextUpper = stateForPart(next, 1);
        if (lower == next && upper == nextUpper) return;
        // No neighbor recursion or half-by-half shape notifications while the pair is being committed.
        boolean written = lower == next || level.setBlock(origin, next, FLAGS);
        if (written && level.getBlockState(origin) == next && level.getBlockState(origin.above()) == upper
                && (upper == nextUpper || level.setBlock(origin.above(), nextUpper, FLAGS))
                && level.getBlockState(origin) == next && level.getBlockState(origin.above()) == nextUpper) return;
        if (level.getBlockState(origin.above()) == nextUpper) level.setBlock(origin.above(), upper, FLAGS);
        if (level.getBlockState(origin) == next) level.setBlock(origin, lower, FLAGS);
    }
    private void refreshAround(Level level, BlockPos origin) {
        if (level.isClientSide) return;
        refresh(level, origin);
        for (Direction side : Direction.Plane.HORIZONTAL) refresh(level, origin.relative(side));
        refresh(level, origin.above(2)); refresh(level, origin.below(2));
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // All connection changes wait until the shared placement validator has finished both cells.
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(PRIMARY)) refreshAround(level, pos);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos other, boolean moving) {
        super.neighborChanged(state, level, pos, source, other, moving);
        if (!level.isClientSide && !isRemovingLinkedStructure(state, level, pos))
            refresh(level, structureOrigin(state, pos));
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos origin = structureOrigin(state, pos), floor = origin.below();
        return connects(level, origin.below(2))
                || level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos other) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && (!neighbor.is(this) || neighbor.getValue(PRIMARY) == state.getValue(PRIMARY)
                || neighbor.getValue(FACING) != state.getValue(FACING))) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean outer = !replacement.is(this) && !isRemovingLinkedStructure(state, level, pos);
        BlockPos origin = structureOrigin(state, pos);
        super.onRemove(state, level, pos, replacement, moving);
        if (!outer || level.isClientSide) return;
        // Old/incompletely updated connection flags must not leave a structural partner orphaned.
        BlockPos peer = state.getValue(PRIMARY) ? origin.above() : origin;
        BlockState remaining = level.getBlockState(peer);
        if (remaining.is(this) && remaining.getValue(PRIMARY) != state.getValue(PRIMARY)
                && remaining.getValue(FACING) == state.getValue(FACING)) level.setBlock(peer, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        refreshAround(level, origin);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        if (mirror == Mirror.NONE) return result;
        return result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        if (!adjacent.is(this)) return super.skipRendering(state, adjacent, side);
        if (!side.getAxis().isHorizontal()) return adjacent.getValue(PRIMARY) != state.getValue(PRIMARY);
        if (state.getValue(PRIMARY) != adjacent.getValue(PRIMARY)) return false;
        Direction facing = state.getValue(FACING);
        BooleanProperty join = side == facing ? FRONT : side == facing.getOpposite() ? BACK
                : side == facing.getClockWise() ? RIGHT : LEFT;
        return state.getValue(join);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
