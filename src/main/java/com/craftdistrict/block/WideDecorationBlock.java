package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Two adjacent blocks across the front, joined clockwise from the primary's facing. */
public final class WideDecorationBlock extends MultiBlockDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<WideDecorationBlock> CODEC = simpleCodec(WideDecorationBlock::new);
    //?}

    public WideDecorationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise();
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
