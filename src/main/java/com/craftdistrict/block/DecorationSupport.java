package com.craftdistrict.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** External supports constrain placement, not the lifetime of an installed decoration. */
final class DecorationSupport {
    private DecorationSupport() {
    }

    static boolean isPlaced(BlockState state, LevelReader level, BlockPos pos) {
        // Match the block, not the full state: connection/open/mount updates may be pending.
        return level.getBlockState(pos).is(state.getBlock());
    }
}
