package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A three-cell operating table, sharing validated placement and single-drop removal. */
public final class SurgicalTableBlock extends MultiBlockDecorationBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    //? if >=1.21 {
    public static final MapCodec<SurgicalTableBlock> CODEC = simpleCodec(SurgicalTableBlock::new);
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(1.5, 9.75, 2, 14.5, 12.25, 16)),
            RestaurantKitchenBlock.rotations(Shapes.or(box(2.75, 0, 1, 13.25, 2, 16),
                    box(5, 2, 4, 11, 9.75, 12), box(1.5, 9.75, 0, 14.5, 12.25, 16)).optimize()),
            RestaurantKitchenBlock.rotations(box(1.5, 9.75, 0, 14.5, 12.25, 14))};

    public SurgicalTableBlock(Properties properties) { super(properties); }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        // Reverse new placement without changing the offsets of existing saved parts.
        return context.getHorizontalDirection();
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }
    @Override protected int partCount() { return 3; }
    @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        return BlockPos.ZERO.relative(state.getValue(FACING).getOpposite(), index);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction along = state.getValue(FACING).getOpposite();
        int part = partIndex(state), adjacent = side == along ? part + 1 : side == along.getOpposite() ? part - 1 : -1;
        if (adjacent >= 0 && adjacent < 3 && neighbor != stateForPart(state, adjacent)) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
