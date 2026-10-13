package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Two columns by two rows; origin is part 0, with width clockwise from facing. */
public class WideTallDecorationBlock extends MultiBlockDecorationBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    //? if >=1.21 {
    public static final MapCodec<WideTallDecorationBlock> CODEC = simpleCodec(WideTallDecorationBlock::new);
    //?}

    public WideTallDecorationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected int partCount() {
        return 4;
    }

    @Override
    protected int partIndex(BlockState state) {
        return state.getValue(PART);
    }

    @Override
    protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PART, index);
    }

    @Override
    protected BlockPos partOffset(BlockState state, int index) {
        return BlockPos.ZERO.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise(), index % 2)
                .above(index / 2);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        // Reflection reverses the width direction as well as the front facing.
        return mirror == Mirror.NONE ? mirrored : mirrored.setValue(PART, partIndex(state) ^ 1);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
