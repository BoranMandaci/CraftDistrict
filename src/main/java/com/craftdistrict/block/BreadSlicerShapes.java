package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Simple base, machinery, and bread envelopes; no individual slice hitboxes. */
final class BreadSlicerShapes {
    static final VoxelShape[] SHAPES = rotations(Shapes.or(
                                           Block.box(1, 0, 0.75, 15, 3.25, 15.25),
                                           Block.box(1.625, 3.25, 5.3125, 14.625, 12.008277, 14.0625),
                                           Block.box(3.25, 3.25, 1, 10.5, 6.625, 6.5),
                                           Block.box(3.25, 3.25, 9.375, 10.5, 6.75, 15.25)).optimize());

    private BreadSlicerShapes() {
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] shapes = new VoxelShape[4];
        shapes[0] = north;
        for (int turn = 1; turn < 4; turn++) {
            VoxelShape result = Shapes.empty();
            for (AABB box : shapes[turn - 1].toAabbs()) {
                result = Shapes.or(result, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            shapes[turn] = result.optimize();
        }
        return shapes;
    }
}
