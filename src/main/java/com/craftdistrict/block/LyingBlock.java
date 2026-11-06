package com.craftdistrict.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/** Static medical furniture that reuses the player's synchronized sleeping pose. */
public interface LyingBlock {
    BlockPos lyingHead(BlockState state, BlockPos pos);
    double lyingHeight();
    default Direction lyingDirection(BlockState state) { return state.getValue(BlockStateProperties.HORIZONTAL_FACING); }
    default BlockPos lyingExitAnchor(BlockState state, BlockPos head) { return head; }
    default AABB lyingClearance(BlockState state, BlockPos head) {
        return new AABB(head.above()).deflate(.15, 0, .15);
    }
}
