package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Three horizontal cells; placement, rollback and single-drop removal stay shared. */
public final class SupermarketCheckoutBlock extends MultiBlockDecorationBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    //? if >=1.21 {
    public static final MapCodec<SupermarketCheckoutBlock> CODEC = simpleCodec(SupermarketCheckoutBlock::new);
    //?}

    public SupermarketCheckoutBlock(Properties properties) { super(properties); }

    @Override
    protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }

    @Override
    protected int partCount() { return 3; }

    @Override
    protected int partIndex(BlockState state) { return state.getValue(PART); }

    @Override
    protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }

    @Override
    protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getClockWise(); }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? mirrored : mirrored.setValue(PART, 2-partIndex(state));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SupermarketShoppingShapes.get(0, partIndex(state), state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
