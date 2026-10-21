package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Filled tray envelopes, cached for each facing; products do not fragment targeting. */
final class PastryTrayShapes {
    static final VoxelShape[][] SHAPES = {
            // croissant_tray
            rotations(Shapes.or(
                Block.box(2, 0, 3, 14, 1.85, 13)).optimize()),
            // cookie_tray
            rotations(Shapes.or(
                Block.box(2, 0, 3, 14, 1.05, 13)).optimize()),
            // bread_rolls_tray
            rotations(Shapes.or(
                Block.box(2, 0, 3, 14, 1.8, 13)).optimize()),
            // empty_baking_tray
            rotations(Shapes.or(
                Block.box(2, 0, 3, 14, 0.85, 13)).optimize()),
    };

    private PastryTrayShapes() {
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] shapes = {north, Shapes.empty(), Shapes.empty(), Shapes.empty()};
        for (int turn = 1; turn < 4; turn++) {
            for (AABB box : shapes[turn - 1].toAabbs()) {
                shapes[turn] = Shapes.or(shapes[turn], Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            shapes[turn] = shapes[turn].optimize();
        }
        return shapes;
    }
}
