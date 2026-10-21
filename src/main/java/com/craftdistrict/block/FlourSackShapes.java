package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One conservative envelope per sack, cached for all four facings. */
final class FlourSackShapes {
    static final VoxelShape[][] SHAPES = {
            // small: one envelope per sack
            rotations(Shapes.or(
                Block.box(1, 0, 5.5, 7.75, 10.191313, 12),
                Block.box(8.25, 0, 3.5, 14.5, 8.69988, 9.5)).optimize()),
            // stacked: one envelope per sack
            rotations(Shapes.or(
                Block.box(2, 0, 1.25, 13.5, 3.75, 6.75),
                Block.box(2.5, 0, 8.5, 13.5, 3.6, 14),
                Block.box(2.19813, 3.45, 3.929957, 13.80187, 6.88, 12.070043)).optimize()),
            // pile: one envelope per sack
            rotations(Shapes.or(
                Block.box(1.5, 0, 0.5, 13.5, 3.3, 4.8),
                Block.box(2, 0, 5.85, 14, 3.15, 10.15),
                Block.box(1.25, 0, 11.2, 13.25, 3.3, 15.5),
                Block.box(2.488651, 2.95, 0.874598, 12.911349, 6.429, 7.725402),
                Block.box(2.881039, 3, 8.312867, 13.118961, 6.332, 15.087133)).optimize()),
    };

    private FlourSackShapes() {
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
