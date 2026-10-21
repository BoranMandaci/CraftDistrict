package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared trolley envelopes; no per-rail or per-product targeting gaps. */
final class BakeryTrayRackShapes {
    static final VoxelShape[][][] SHAPES = {
        // bakery_tray_rack_empty
        {
            rotations(Shapes.or(
                Block.box(1.5, 0, 2.25, 14.5, 16, 13.75)).optimize()),
            rotations(Shapes.or(
                Block.box(1.5, 0, 2.25, 14.5, 15.25, 13.75)).optimize())
        },
        // bakery_tray_rack_baked
        {
            rotations(Shapes.or(
                Block.box(1.5, 0, 2.25, 14.5, 16, 13.75)).optimize()),
            rotations(Shapes.or(
                Block.box(1.5, 0, 2.25, 14.5, 15.25, 13.75)).optimize())
        }
    };

    private BakeryTrayRackShapes() {
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
