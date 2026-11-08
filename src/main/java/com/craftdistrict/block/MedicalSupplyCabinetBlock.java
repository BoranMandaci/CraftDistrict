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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Complete, aligned cabinets share their inner ends using the existing paired lifecycle. */
public class MedicalSupplyCabinetBlock extends HospitalEquipmentBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    private static final int JOIN_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final VoxelShape ENVELOPE = box(0, 0, 0, 16, 16, 16);
    //? if >=1.21 {
    public static final MapCodec<MedicalSupplyCabinetBlock> CODEC = simpleCodec(MedicalSupplyCabinetBlock::new);
    //?}

    public MedicalSupplyCabinetBlock(Properties properties) {
        this(Design.SUPPLY_CABINET, properties);
    }
    protected MedicalSupplyCabinetBlock(Design design, Properties properties) {
        super(design, properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(LEFT, RIGHT);
    }
    private boolean matches(BlockState state, Direction facing, boolean primary) {
        return state.is(this) && state.getValue(FACING) == facing && state.getValue(PRIMARY) == primary;
    }
    protected boolean connectsFacing(Direction facing, Direction otherFacing) { return facing == otherFacing; }
    private boolean complete(LevelReader level, BlockPos origin, Direction facing) {
        if (!level.hasChunkAt(origin) || !level.hasChunkAt(origin.above())) return false;
        BlockState lower = level.getBlockState(origin);
        if (!lower.is(this) || !lower.getValue(PRIMARY)) return false;
        Direction actualFacing = lower.getValue(FACING);
        return connectsFacing(facing, actualFacing)
                && matches(level.getBlockState(origin.above()), actualFacing, false);
    }
    public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos origin = structureOrigin(state, pos); Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, complete(level, origin.relative(facing.getCounterClockWise()), facing))
                .setValue(RIGHT, complete(level, origin.relative(facing.getClockWise()), facing));
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : layout(state, context.getLevel(), context.getClickedPos());
    }
    protected final void refresh(Level level, BlockPos origin, Direction facing) {
        if (!complete(level, origin, facing)) return;
        BlockState first = level.getBlockState(origin), second = level.getBlockState(origin.above());
        BlockState joined = layout(first, level, origin), joinedSecond = structurePartState(joined, 1);
        // Update both halves without recursive row notifications; removal still sees one paired state.
        if (first != joined) level.setBlock(origin, joined, JOIN_FLAGS);
        if (second != joinedSecond && level.getBlockState(origin) == joined && level.getBlockState(origin.above()) == second)
            level.setBlock(origin.above(), joinedSecond, JOIN_FLAGS);
    }
    protected void refreshRow(Level level, BlockPos origin, Direction facing) {
        refresh(level, origin, facing);
        refresh(level, origin.relative(facing.getCounterClockWise()), facing);
        refresh(level, origin.relative(facing.getClockWise()), facing);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(PRIMARY) && level.getBlockState(pos).is(this))
            refreshRow(level, pos, state.getValue(FACING));
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos otherPos, boolean moving) {
        super.neighborChanged(state, level, pos, source, otherPos, moving);
        if (!level.isClientSide && !isRemovingLinkedStructure(state, level, pos))
            refresh(level, structureOrigin(state, pos), state.getValue(FACING));
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean nested = isRemovingLinkedStructure(state, level, pos);
        BlockPos origin = structureOrigin(state, pos); Direction facing = state.getValue(FACING);
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && !nested && !replacement.is(this)) refreshRow(level, origin, facing);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return ENVELOPE; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return ENVELOPE; }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
