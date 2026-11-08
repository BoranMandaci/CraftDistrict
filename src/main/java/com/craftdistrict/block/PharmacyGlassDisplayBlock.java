package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Complete two-cell vitrines share trim; saved facing/primary states retain their footprint. */
public final class PharmacyGlassDisplayBlock extends PharmacyFixtureBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP = BooleanProperty.create("up"), DOWN = BooleanProperty.create("down");
    private static final int JOIN_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    //? if >=1.21 {
    public static final MapCodec<PharmacyGlassDisplayBlock> CODEC = simpleCodec(PharmacyGlassDisplayBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
    public PharmacyGlassDisplayBlock(Properties properties) {
        super(Design.GLASS_DISPLAY, properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false)
                .setValue(UP, false).setValue(DOWN, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(LEFT, RIGHT, UP, DOWN);
    }
    private boolean matches(BlockState state, Direction facing, boolean primary) {
        return state.is(this) && state.getValue(FACING) == facing && state.getValue(PRIMARY) == primary;
    }
    private boolean complete(LevelReader level, BlockPos origin, Direction facing) {
        BlockPos partner = origin.relative(facing.getClockWise());
        return level.hasChunkAt(origin) && level.hasChunkAt(partner)
                && matches(level.getBlockState(origin), facing, true)
                && matches(level.getBlockState(partner), facing, false);
    }
    public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos origin = structureOrigin(state, pos); Direction facing = state.getValue(FACING);
        Direction along = facing.getClockWise();
        return state.setValue(LEFT, complete(level, origin.relative(along, -2), facing))
                .setValue(RIGHT, complete(level, origin.relative(along, 2), facing))
                .setValue(UP, complete(level, origin.above(), facing))
                .setValue(DOWN, complete(level, origin.below(), facing));
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : layout(state, context.getLevel(), context.getClickedPos());
    }
    private void refresh(Level level, BlockPos origin, Direction facing) {
        if (!complete(level, origin, facing)) return;
        BlockPos partner = origin.relative(facing.getClockWise());
        BlockState first = level.getBlockState(origin), second = level.getBlockState(partner);
        BlockState joined = layout(first, level, origin), joinedSecond = structurePartState(joined, 1);
        if (first != joined) level.setBlock(origin, joined, JOIN_FLAGS);
        if (second != joinedSecond && level.getBlockState(origin) == joined && level.getBlockState(partner) == second)
            level.setBlock(partner, joinedSecond, JOIN_FLAGS);
    }
    private void refreshNeighbors(Level level, BlockPos origin, Direction facing) {
        Direction along = facing.getClockWise();
        refresh(level, origin, facing);
        refresh(level, origin.relative(along, -2), facing);
        refresh(level, origin.relative(along, 2), facing);
        refresh(level, origin.above(), facing);
        refresh(level, origin.below(), facing);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(PRIMARY) && level.getBlockState(pos).is(this))
            refreshNeighbors(level, pos, state.getValue(FACING));
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos otherPos, boolean moving) {
        super.neighborChanged(state, level, pos, source, otherPos, moving);
        if (!level.isClientSide && !isRemovingLinkedStructure(state, level, pos))
            refresh(level, structureOrigin(state, pos), state.getValue(FACING));
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction along = getExtensionDirection(state);
        Direction partner = state.getValue(PRIMARY) ? along : along.getOpposite();
        // Ignore transient trim flags while both halves are updated, as other connected cabinets do.
        if (side == partner && !matches(neighbor, state.getValue(FACING), !state.getValue(PRIMARY)))
            return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean nested = isRemovingLinkedStructure(state, level, pos);
        BlockPos origin = structureOrigin(state, pos); Direction facing = state.getValue(FACING);
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && !nested && !replacement.is(this)) refreshNeighbors(level, origin, facing);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
}
