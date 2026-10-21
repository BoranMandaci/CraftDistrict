package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Simple locker/rack envelopes, cached by decoration, part, and facing. */
final class FireStationShapes {
    private static final VoxelShape[][][] SHAPES = {
        {
            rotations(Shapes.or(
                Block.box(0.5, 0, 3.375, 16, 16, 15)).optimize()),
            rotations(Shapes.or(
                Block.box(0, 0, 3.375, 15.5, 16, 15)).optimize()),
            rotations(Shapes.or(
                Block.box(0.5, 0, 3.375, 16, 16, 15)).optimize()),
            rotations(Shapes.or(
                Block.box(0, 0, 3.375, 15.5, 16, 15)).optimize())
        },
        {
            rotations(Shapes.or(
                Block.box(0.5, 0, 3.5, 16, 16, 15)).optimize()),
            rotations(Shapes.or(
                Block.box(0, 0, 3.5, 15.5, 16, 15)).optimize()),
            rotations(Shapes.or(
                Block.box(1, 0, 6.75, 16, 16, 15)).optimize()),
            rotations(Shapes.or(
                Block.box(0, 0, 6.75, 15, 16, 15)).optimize())
        },
        {
            rotations(Shapes.or(
                Block.box(5.25, 2.5, 9.625, 10.75, 11.25, 15.125),
                Block.box(6, 11.25, 11.5, 9.75, 14.25, 13.125),
                Block.box(6.5, 3.5, 15.125, 9.5, 12.5, 16)).optimize())
        }
    };

    private FireStationShapes() { }

    static VoxelShape get(int decoration, int part, BlockState state) {
        int facing = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return SHAPES[decoration][part][facing];
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] result = new VoxelShape[4];
        result[0] = north;
        for (int turn = 1; turn < 4; turn++) {
            VoxelShape rotated = Shapes.empty();
            for (AABB box : result[turn - 1].toAabbs()) {
                rotated = Shapes.or(rotated, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            result[turn] = rotated.optimize();
        }
        return result;
    }
}
