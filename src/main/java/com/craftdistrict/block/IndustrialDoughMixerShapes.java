package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cached machinery envelopes; bowl and controls do not fragment targeting. */
final class IndustrialDoughMixerShapes {
    static final VoxelShape[][] SHAPES = {
            rotations(Shapes.or(
                Block.box(1, 0, 1, 15, 4, 15),
                Block.box(4.625, 4, 11.5, 11.375, 16, 15.1875),
                Block.box(6, 4, 6, 10, 9.75, 12),
                Block.box(3.5, 9.75, 2, 12.75, 16, 13.5)).optimize()),
            rotations(Shapes.or(
                Block.box(4, 0, 0.75, 12, 7.5, 15.1875),
                Block.box(2.25, 0, 2.5, 13.75, 5.75, 10.5),
                Block.box(3, 7.5, 1.8125, 13, 15, 15.25)).optimize())
    };

    private IndustrialDoughMixerShapes() {
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] shapes = new VoxelShape[4];
        shapes[0] = north;
        for (int turn = 1; turn < 4; turn++) {
            VoxelShape rotated = Shapes.empty();
            for (AABB box : shapes[turn - 1].toAabbs()) {
                rotated = Shapes.or(rotated, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            shapes[turn] = rotated.optimize();
        }
        return shapes;
    }
}
