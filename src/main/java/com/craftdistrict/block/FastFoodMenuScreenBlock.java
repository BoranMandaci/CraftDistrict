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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Three static wall panels share the existing atomic multiblock placement/drop lifecycle. */
public final class FastFoodMenuScreenBlock extends MultiBlockDecorationBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(0, 2.5, 14, 16, 13.5, 16));
    //? if >=1.21 {
    public static final MapCodec<FastFoodMenuScreenBlock> CODEC = simpleCodec(FastFoodMenuScreenBlock::new);
    //?}

    public FastFoodMenuScreenBlock(Properties properties) { super(properties); }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }
    @Override protected int partCount() { return 3; }
    @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getClockWise(); }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return face.getAxis().isHorizontal() ? face : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction back = state.getValue(FACING).getOpposite();
        BlockPos support = pos.relative(back);
        return level.getBlockState(support).isFaceSturdy(level, support, back.getOpposite());
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction across = getExtensionDirection(state);
        int index = partIndex(state);
        int expected = side == across ? index + 1 : side == across.getOpposite() ? index - 1 : -1;
        if (expected >= 0 && expected < 3 && (!neighbor.is(this)
                || neighbor.getValue(FACING) != state.getValue(FACING) || partIndex(neighbor) != expected))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? mirrored : mirrored.setValue(PART, 2 - partIndex(state));
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
