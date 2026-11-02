package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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

/** Same-facing, complete units share their exposed ends; lifecycle remains atomic. */
public final class OfficeModularFurnitureBlock extends OfficeFurnitureBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    //? if >=1.21 {
    public static final MapCodec<OfficeModularFurnitureBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((OfficeModularFurnitureBlock b) -> b.design),
            propertiesCodec()).apply(instance, OfficeModularFurnitureBlock::new));
    //?}
    private static final int JOIN_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final VoxelShape[][][] TURNSTILE_SHAPES = turnstileShapes();

    public OfficeModularFurnitureBlock(Design design, Properties properties) {
        super(design, properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(LEFT, RIGHT);
    }
    private int stride() { return design == Design.TURNSTILE ? 2 : 1; }
    private boolean matches(BlockState state, Direction facing, int part) {
        return state.is(this) && state.getValue(FACING) == facing && state.getValue(PART) == part
                && state.getValue(PRIMARY) == (part == 0);
    }
    private boolean complete(LevelReader level, BlockPos origin, Direction facing) {
        if (!level.hasChunkAt(origin)) return false;
        BlockState first = level.getBlockState(origin);
        if (!matches(first, facing, 0)) return false;
        BlockPos partner = origin.offset(partOffset(first, 1));
        return level.hasChunkAt(partner) && matches(level.getBlockState(partner), facing, 1);
    }
    public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos origin = structureOrigin(state, pos); Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, complete(level, origin.relative(facing.getCounterClockWise(), stride()), facing))
                .setValue(RIGHT, complete(level, origin.relative(facing.getClockWise(), stride()), facing));
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : layout(state, context.getLevel(), context.getClickedPos());
    }
    private void refresh(Level level, BlockPos origin, Direction facing) {
        if (!complete(level, origin, facing)) return;
        BlockState first = level.getBlockState(origin);
        BlockPos partnerPos = origin.offset(partOffset(first, 1));
        BlockState partner = level.getBlockState(partnerPos), joined = layout(first, level, origin);
        BlockState joinedPartner = stateForPart(joined, 1);
        // Joins depend only on identity/facing, never neighboring join flags. No row scan.
        if (first != joined) level.setBlock(origin, joined, JOIN_FLAGS);
        if (partner != joinedPartner && level.getBlockState(origin) == joined && level.getBlockState(partnerPos) == partner)
            level.setBlock(partnerPos, joinedPartner, JOIN_FLAGS);
    }
    private void refreshRow(Level level, BlockPos origin, Direction facing) {
        refresh(level, origin, facing);
        refresh(level, origin.relative(facing.getCounterClockWise(), stride()), facing);
        refresh(level, origin.relative(facing.getClockWise(), stride()), facing);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(PART) == 0 && level.getBlockState(pos).is(this))
            refreshRow(level, pos, state.getValue(FACING));
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving) {
        super.neighborChanged(state, level, pos, source, sourcePos, moving);
        if (!level.isClientSide && !isRemovingLinkedStructure(state, level, pos))
            refresh(level, structureOrigin(state, pos), state.getValue(FACING));
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        int partner = 1 - state.getValue(PART);
        if (partner < 0) return Blocks.AIR.defaultBlockState();
        BlockPos origin = structureOrigin(state, pos);
        return neighborPos.equals(origin.offset(partOffset(state, partner)))
                && !matches(neighbor, state.getValue(FACING), partner) ? Blocks.AIR.defaultBlockState() : state;
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
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (design != Design.TURNSTILE) return super.getShape(state, level, pos, context);
        int joins = (state.getValue(LEFT) ? 1 : 0) | (state.getValue(RIGHT) ? 2 : 0);
        return TURNSTILE_SHAPES[joins][Math.min(state.getValue(PART), 1)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    private static VoxelShape[][][] turnstileShapes() {
        VoxelShape[][][] result = new VoxelShape[4][2][];
        for (int joins = 0; joins < 4; joins++) {
            double leftEnd = (joins & 1) != 0 ? 2 : 4, rightStart = (joins & 2) != 0 ? 14 : 12;
            result[joins][0] = RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 2.5, leftEnd, 16, 13.5),
                    box(leftEnd, 3, 7, 16, 11, 13)));
            result[joins][1] = RestaurantKitchenBlock.rotations(Shapes.or(box(rightStart, 0, 2.5, 16, 16, 13.5),
                    box(0, 9.25, 7.625, 10, 10, 8.375)));
        }
        return result;
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
