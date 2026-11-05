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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-wide workstations retain old single-cell placements and join at complete unit edges. */
public final class NurseStationCounterBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    private static final int JOIN_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    //? if >=1.21 {
    public static final MapCodec<NurseStationCounterBlock> CODEC = simpleCodec(NurseStationCounterBlock::new);
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 0, 16, 13.25, 16),
                    box(3.5, 13.25, 4, 13.75, 19.25, 14)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 0, 16, 15.25, 16),
                    box(7, 15.25, 6, 16, 22, 9)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 0, 16, 15.25, 16),
                    box(0, 15.25, 6, 14.5, 22, 9)).optimize())};

    public NurseStationCounterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false).setValue(EXPANDED, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(LEFT, RIGHT, EXPANDED);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 2 : 1; }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getClockWise(); }
    private boolean matches(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(FACING) == state.getValue(FACING);
    }
    private boolean complete(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(EXPANDED) && !state.getValue(PRIMARY)) return false;
        BlockPos origin = structureOrigin(state, pos);
        for (int i = 0; i < partCount(state); i++) {
            BlockPos p = origin.offset(partOffset(state, i));
            if (!level.hasChunkAt(p)) return false;
            BlockState other = level.getBlockState(p);
            if (!matches(state, other) || other.getValue(EXPANDED) != state.getValue(EXPANDED)
                    || other.getValue(PRIMARY) != (i == 0)) return false;
        }
        return true;
    }
    private boolean connected(BlockState state, LevelReader level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState other = level.getBlockState(pos);
        return matches(state, other) && complete(other, level, pos);
    }
    private BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos origin = structureOrigin(state, pos); Direction along = getExtensionDirection(state);
        return state.setValue(LEFT, connected(state, level, origin.relative(along.getOpposite())))
                .setValue(RIGHT, connected(state, level, origin.relative(along, partCount(state))));
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context).setValue(EXPANDED, true);
        return layout(state, context.getLevel(), context.getClickedPos());
    }
    private void refreshAt(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) || !complete(state, level, pos)) return;
        BlockPos origin = structureOrigin(state, pos);
        BlockState joined = layout(level.getBlockState(origin), level, origin);
        for (int i = 0; i < partCount(joined); i++) {
            BlockPos part = origin.offset(partOffset(joined, i)); BlockState next = stateForPart(joined, i);
            if (level.getBlockState(part) != next) level.setBlock(part, next, JOIN_FLAGS);
        }
    }
    private void refreshEdges(Level level, BlockState state, BlockPos origin) {
        Direction along = getExtensionDirection(state);
        refreshAt(level, origin);
        refreshAt(level, origin.relative(along.getOpposite()));
        refreshAt(level, origin.relative(along, partCount(state)));
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(PRIMARY) && level.getBlockState(pos).is(this))
            refreshEdges(level, state, pos);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos otherPos, boolean moving) {
        super.neighborChanged(state, level, pos, source, otherPos, moving);
        if (!level.isClientSide && !isRemovingLinkedStructure(state, level, pos)) refreshAt(level, pos);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(EXPANDED) && !state.getValue(PRIMARY)) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (state.getValue(EXPANDED)) {
            Direction along = getExtensionDirection(state);
            Direction partner = state.getValue(PRIMARY) ? along : along.getOpposite();
            if (side == partner && other != stateForPart(state, 1 - partIndex(state))) return Blocks.AIR.defaultBlockState();
        }
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean nested = isRemovingLinkedStructure(state, level, pos); BlockPos origin = structureOrigin(state, pos);
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && !nested && !replacement.is(this)) refreshEdges(level, state, origin);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(EXPANDED) ? (state.getValue(PRIMARY) ? 1 : 2) : 0][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
