package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated structural envelopes; cached once by layout, part and facing. */
final class SupermarketShapes {
    private static final VoxelShape[][][] SHAPES = {
        {
            rotations(Shapes.or(
                Block.box(0, 0, 1.5, 16, 2.75, 16),
                Block.box(0, 2.75, 3.25, 16, 16, 16),
                Block.box(0, 2.75, 1.5, 16, 16, 3.25)).optimize()),
            rotations(Shapes.or(
                Block.box(0, 0, 3.25, 16, 14.5, 16),
                Block.box(0, 0, 1.5, 16, 14.5, 3.25),
                Block.box(0, 14.5, 14.75, 16, 15.25, 16)).optimize())
        },
        {
            rotations(Shapes.or(
                Block.box(1, 0, 1.5, 15, 1.5, 14.5),
                Block.box(1, 1.5, 2, 15, 16, 13.5)).optimize()),
            rotations(Shapes.or(
                Block.box(1, 0, 2, 15, 8, 13.5),
                Block.box(1.5, 8, 11.75, 14.5, 11, 13.5),
                Block.box(1, 11, 11, 15, 15.75, 13.5)).optimize())
        },
        {
            rotations(Shapes.or(
                Block.box(0.5, 0, 3, 16, 6, 13.5),
                Block.box(0.5, 6, 2.5, 16, 10, 14),
                Block.box(0.5, 10, 8, 16, 14, 14)).optimize()),
            rotations(Shapes.or(
                Block.box(0, 0, 3, 15.5, 6, 13.5),
                Block.box(0, 6, 2.5, 15.5, 10, 14),
                Block.box(0, 10, 8, 15.5, 14, 14)).optimize())
        }
    };
    private SupermarketShapes() { }

    static VoxelShape get(int layout, int part, BlockState state) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return SHAPES[layout][part][turn];
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] result = new VoxelShape[4];
        result[0] = north;
        for (int turn = 1; turn < 4; turn++) {
            VoxelShape rotated = Shapes.empty();
            for (AABB box : result[turn-1].toAabbs()) {
                rotated = Shapes.or(rotated, Shapes.box(1-box.maxZ, box.minY, box.minX,
                        1-box.minZ, box.maxY, box.maxX));
            }
            result[turn] = rotated.optimize();
        }
        return result;
    }
}
