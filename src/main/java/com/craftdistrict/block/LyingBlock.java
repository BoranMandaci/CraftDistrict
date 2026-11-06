package com.craftdistrict.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Static medical furniture that reuses the player's synchronized sleeping pose. */
public interface LyingBlock {
    BlockPos lyingHead(BlockState state, BlockPos pos);
    double lyingHeight();
}
