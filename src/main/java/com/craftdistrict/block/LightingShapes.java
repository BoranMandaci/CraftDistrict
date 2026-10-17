package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One or two conservative boxes per fixture, rotated only at class initialization. */
final class LightingShapes {
    static final int SPOTLIGHT = 8, STRIP = 9, NEON = 10,
            SECURITY = 11, DESK = 12, BOLLARD = 13, TRACK = 14, TRACK_WALL = 16, OPEN_HANGING = 18,
            PANEL_WALL = 19, STRIP_WALL = 20, STRIP_WALL_VERTICAL = 21;

    private static final VoxelShape[][] SHAPES = {
            rotations(Block.box(.5, 14.5, .5, 15.5, 16, 15.5)),
            rotations(Shapes.or(Block.box(3.5, 2.75, 3.5, 12.5, 7.5, 12.5),
                    Block.box(6.5, 7.5, 6.5, 9.5, 16, 9.5)).optimize()),
            rotations(Block.box(5.69, 5, 12.94, 10.31, 12, 16)),
            rotations(Shapes.or(Block.box(2.5, 5, 6, 13.5, 14, 11.5),
                    Block.box(6, 6, 11.5, 10, 12.5, 16)).optimize()),
            rotations(Block.box(1.5, 5, 13.875, 14.5, 11, 16)),
            rotations(Block.box(.75, 13, 4.5, 16, 16, 11.5)),
            rotations(Block.box(0, 13, 4.5, 15.25, 16, 11.5)),
            rotations(Shapes.or(Block.box(1.5, 5, 6.875, 14.5, 11, 9.125),
                    Block.box(3, 11, 7.75, 13, 16, 8.25)).optimize()),
            rotations(Block.box(4, 14.5, 4, 12, 16, 12)),
            rotations(Block.box(0, 15, 6.5, 16, 16, 9.5)),
            rotations(Block.box(1.5, 4.5, 14.375, 14.5, 11.5, 16)),
            rotations(Shapes.or(Block.box(4.75, 8, 8.5, 11.25, 13.5, 13.5),
                    Block.box(6.25, 5, 13.5, 9.75, 11.5, 16)).optimize()),
            rotations(Shapes.or(Block.box(5, 0, 5.5, 11, .75, 11.5),
                    Block.box(5.25, .75, 3.5, 10.75, 12, 12)).optimize()),
            rotations(Shapes.or(Block.box(5.3, 0, 5.3, 10.7, .9, 10.7),
                    Block.box(5.75, .9, 5.75, 10.25, 13.275, 10.25)).optimize()),
            rotations(Shapes.or(Block.box(.5, 14.75, 7.125, 16, 16, 8.875),
                    Block.box(3, 8, 4.5, 16, 14.75, 11.5)).optimize()),
            rotations(Shapes.or(Block.box(0, 14.75, 7.125, 15.5, 16, 8.875),
                    Block.box(0, 8, 4.5, 13, 14.75, 11.5)).optimize()),
            rotations(Shapes.or(Block.box(.5, 10.75, 15, 16, 12.75, 16),
                    Block.box(3, 6, 8.25, 16, 11, 15.5)).optimize()),
            rotations(Shapes.or(Block.box(0, 10.75, 15, 15.5, 12.75, 16),
                    Block.box(0, 6, 8.25, 13, 11, 15.5)).optimize()),
            rotations(Shapes.or(Block.box(1.5, 4.5, 7.125, 14.5, 11.5, 8.875),
                    Block.box(3, 11.5, 7.75, 13, 16, 8.25)).optimize()),
            rotations(Block.box(.5, .5, 14.5, 15.5, 15.5, 16)),
            rotations(Block.box(0, 6.5, 15, 16, 9.5, 16)),
            rotations(Block.box(6.5, 0, 15, 9.5, 16, 16))
    };

    private LightingShapes() { }

    static VoxelShape get(int index, BlockState state) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return SHAPES[index][turn];
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
