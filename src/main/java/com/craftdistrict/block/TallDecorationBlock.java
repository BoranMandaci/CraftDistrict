package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Two-block-tall decorations: primary is the lower part, its partner is above it. */
public final class TallDecorationBlock extends MultiBlockDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<TallDecorationBlock> CODEC = simpleCodec(TallDecorationBlock::new);
    //?}

    public TallDecorationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return Direction.UP;
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
