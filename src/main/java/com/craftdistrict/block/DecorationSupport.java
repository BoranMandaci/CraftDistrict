package com.craftdistrict.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/** External supports constrain placement, not the lifetime of an installed decoration. */
final class DecorationSupport {
    private static final VoxelShape TOP = Block.box(0, 15.875, 0, 16, 16, 16);
    private static final Map<VoxelShape, VoxelShape> TOPS = Collections.synchronizedMap(new IdentityHashMap<>());
    private DecorationSupport() {
    }

    static boolean isPlaced(BlockState state, LevelReader level, BlockPos pos) {
        // Match the block, not the full state: connection/open/mount updates may be pending.
        return level.getBlockState(pos).is(state.getBlock());
    }

    /** Support checks only: retain the actual collision/outline and existing side support. */
    static VoxelShape withTop(VoxelShape shape) {
        return Block.isFaceFull(shape, Direction.UP) ? shape
                : TOPS.computeIfAbsent(shape, original -> Shapes.or(
                        Shapes.join(original, Shapes.block(), BooleanOp.AND), TOP).optimize());
    }
}
