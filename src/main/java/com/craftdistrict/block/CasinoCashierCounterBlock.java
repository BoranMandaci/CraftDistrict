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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** New cashier bays occupy one floor cell; saved three-cell counters retain their original lifecycle. */
public final class CasinoCashierCounterBlock extends MultiBlockDecorationBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty MODULAR = BooleanProperty.create("modular");
    private static final int JOIN_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(0, 0, 0, 16, 16, 16)),
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 5, 2.75, 16, 16, 4.5),
                    box(0, 0, 2.75, .75, 5, 4.5), box(15.25, 0, 2.75, 16, 5, 4.5),
                    box(4.6, 0, 0, 11.4, .5, 6)).optimize()),
            RestaurantKitchenBlock.rotations(box(0, 0, 0, 16, 16, 16))};
    private static final VoxelShape[][] MODULAR_SHAPES = {
            RestaurantKitchenBlock.rotations(box(0, 0, 0, 16, 16, 16)),
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 4.5, 12.5, 16, 15.75, 15),
                    box(0, 0, 12.5, 5.25, 4.5, 15), box(10.75, 0, 12.5, 16, 4.5, 15),
                    box(4.75, 0, 9, 11.25, .5, 15.5)).optimize())};
    //? if >=1.21 {
    public static final MapCodec<CasinoCashierCounterBlock> CODEC = simpleCodec(CasinoCashierCounterBlock::new);
    //?}
    public CasinoCashierCounterBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false).setValue(MODULAR, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART, LEFT, RIGHT, MODULAR); }
    @Override protected int partCount() { return 3; }
    @Override protected int partCount(BlockState state) { return state.getValue(MODULAR) ? 2 : 3; }
    @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        return index == 1 ? BlockPos.ZERO.above() : index == 2
                ? BlockPos.ZERO.relative(state.getValue(FACING).getOpposite()) : BlockPos.ZERO;
    }
    private boolean matches(BlockState state, Direction facing, int part, boolean modular) {
        return state.is(this) && state.getValue(FACING) == facing && partIndex(state) == part && state.getValue(MODULAR) == modular;
    }
    private boolean complete(LevelReader level, BlockPos origin, Direction facing, boolean modular) {
        BlockState layout = defaultBlockState().setValue(FACING, facing).setValue(MODULAR, modular);
        for (int i = 0; i < partCount(layout); i++) {
            BlockPos pos = origin.offset(partOffset(layout, i));
            if (!level.hasChunkAt(pos) || !matches(level.getBlockState(pos), facing, i, modular)) return false;
        }
        return true;
    }
    private BlockState layout(BlockState state, LevelReader level, BlockPos origin) {
        Direction facing = state.getValue(FACING);
        boolean modular = state.getValue(MODULAR);
        return state.setValue(LEFT, complete(level, origin.relative(facing.getCounterClockWise()), facing, modular))
                .setValue(RIGHT, complete(level, origin.relative(facing.getClockWise()), facing, modular));
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : layout(state.setValue(MODULAR, true), context.getLevel(), context.getClickedPos());
    }
    private void refresh(Level level, BlockPos origin, Direction facing, boolean modular) {
        if (!complete(level, origin, facing, modular)) return;
        BlockState first = level.getBlockState(origin), joined = layout(first, level, origin);
        int count = partCount(first);
        BlockPos[] positions = new BlockPos[count];
        BlockState[] before = new BlockState[count];
        for (int i = 0; i < count; i++) {
            positions[i] = origin.offset(partOffset(first, i)); before[i] = level.getBlockState(positions[i]);
        }
        for (int i = 0; i < count; i++) {
            if (i > 0 && level.getBlockState(origin) != joined || level.getBlockState(positions[i]) != before[i]) return;
            BlockState next = stateForPart(joined, i);
            if (before[i] != next) level.setBlock(positions[i], next, JOIN_FLAGS);
        }
    }
    private void refreshRow(Level level, BlockPos origin, Direction facing, boolean modular) {
        refresh(level, origin, facing, modular);
        refresh(level, origin.relative(facing.getCounterClockWise()), facing, modular);
        refresh(level, origin.relative(facing.getClockWise()), facing, modular);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && partIndex(state) == 0 && level.getBlockState(pos).is(this))
            refreshRow(level, pos, state.getValue(FACING), state.getValue(MODULAR));
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos otherPos, boolean moving) {
        super.neighborChanged(state, level, pos, source, otherPos, moving);
        if (!level.isClientSide && !isRemovingLinkedStructure(state, level, pos))
            refresh(level, structureOrigin(state, pos), state.getValue(FACING), state.getValue(MODULAR));
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (partIndex(state) >= partCount(state)) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos below = (partIndex(state) == 1 ? pos.below() : pos).below();
        return level.hasChunkAt(below) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int i = 0; i < partCount(state); i++)
            if (origin.offset(partOffset(state, i)).equals(otherPos) && !matches(neighbor, state.getValue(FACING), i, state.getValue(MODULAR)))
                return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean nested = isRemovingLinkedStructure(state, level, pos);
        BlockPos origin = structureOrigin(state, pos); Direction facing = state.getValue(FACING);
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && !nested && !replacement.is(this)) refreshRow(level, origin, facing, state.getValue(MODULAR));
    }
    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        Direction facing = state.getValue(FACING);
        return side.getAxis().isHorizontal() && side.getAxis() != facing.getAxis()
                && matches(neighbor, facing, partIndex(state), state.getValue(MODULAR)) || super.skipRendering(state, neighbor, side);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(MODULAR)) return partIndex(state) < 2
                ? MODULAR_SHAPES[partIndex(state)][RestaurantKitchenBlock.turn(state)] : Shapes.empty();
        return SHAPES[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
