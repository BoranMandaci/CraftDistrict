package com.craftdistrict.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

final class RotaryEntranceShapes {
    private static final VoxelShape LOWER_WING = Block.box(0, 1.55, 0, 16, 16, 16);
    private static final VoxelShape UPPER_WING = Block.box(0, 0, 0, 16, 42.55 - 32, 16);
    static final VoxelShape[][] REVOLVING = revolving();

    private RotaryEntranceShapes() { }

    private static VoxelShape[][] revolving() {
        List<AABB> frame = new ArrayList<>();
        // A stepped circular deck and canopy, with openings toward north and south.
        double[] edge = {15, 10, 7, 4.5, 2.5, 1, 1, 1, 2.5, 4.5, 7, 10, 15};
        double[] depth = {1, 3.5, 6, 8.5, 11, 14, 18, 30, 34, 37, 39.5, 42, 44.5, 47};
        for (int i = 0; i < depth.length - 1; i++) {
            add(frame, edge[i], 0, depth[i], 48 - edge[i], 1.25, depth[i + 1]);
            add(frame, edge[i], 43.25, depth[i], 48 - edge[i], 46.9, depth[i + 1]);
            add(frame, edge[i] + .75, 46.9, Math.max(1.75, depth[i]), 47.25 - edge[i], 48,
                    Math.min(46.25, depth[i + 1]));
        }
        add(frame, 22.9, 1.25, 22.9, 25.1, 43.25, 25.1);
        add(frame, 22.2, 1.25, 22.2, 25.8, 1.9, 25.8);
        double radius = 21.8;
        double half = radius * Math.tan(Math.PI / 16);
        for (int facet = 0; facet < 16; facet++) {
            if (facet >= 3 && facet <= 5 || facet >= 11 && facet <= 13) continue;
            double angle = facet * Math.PI / 8;
            double cos = Math.cos(angle), sin = Math.sin(angle);
            // Short collision segments approximate the curved glazing without closing either entrance.
            for (int segment = 0; segment < 4; segment++) {
                double start = half * (segment * .5 - 1);
                double end = start + half * .5;
                double x1 = 24 + radius * cos - start * sin, x2 = 24 + radius * cos - end * sin;
                double z1 = 24 + radius * sin + start * cos, z2 = 24 + radius * sin + end * cos;
                add(frame, Math.min(x1, x2) - .65, 1.25, Math.min(z1, z2) - .65,
                        Math.max(x1, x2) + .65, 43.25, Math.max(z1, z2) + .65);
            }
        }
        add(frame, 0, 0, 23.35, 2.24, 48, 24.65);
        add(frame, 45.76, 0, 23.35, 48, 48, 24.65);
        return split(frame, 3, 3, 3);
    }

    private static void add(List<AABB> frame, double x0, double y0, double z0, double x1, double y1, double z1) {
        frame.add(new AABB(x0, y0, z0, x1, y1, z1));
    }

    static VoxelShape[] parked(float angle, Direction facing) {
        List<AABB> wings = new ArrayList<>(78);
        for (int blade = 0; blade < 3; blade++) {
            double radians = Math.toRadians(angle + 180 - facing.toYRot() + blade * 120);
            double axisX = Math.cos(radians), axisZ = -Math.sin(radians);
            double padX = .45 * Math.abs(axisZ), padZ = .45 * Math.abs(axisX);
            // Short, touching slices enclose the glass and its stiles at the actual angle.
            for (double start = .95; start < 20.35; start += .75) {
                double end = Math.min(start + .75, 20.35);
                add(wings, 24 + Math.min(start * axisX, end * axisX) - padX, 0,
                        24 + Math.min(start * axisZ, end * axisZ) - padZ,
                        24 + Math.max(start * axisX, end * axisX) + padX, 16,
                        24 + Math.max(start * axisZ, end * axisZ) + padZ);
            }
        }

        Direction right = facing.getClockWise();
        VoxelShape[] result = new VoxelShape[REVOLVING.length];
        int turn = (facing.get2DDataValue() + 2) % 4;
        for (int cell = 0; cell < 9; cell++) {
            int across = cell % 3 - 1, depth = cell / 3 - 1;
            int x = 1 + across * right.getStepX() - depth * facing.getStepX();
            int z = 1 + across * right.getStepZ() - depth * facing.getStepZ();
            // Build the shared horizontal cross-section once, then trim it to the glass height.
            VoxelShape panel = section(wings, x * 16, 0, z * 16);
            for (int layer = 0; layer < 3; layer++) {
                int part = cell + layer * 9;
                VoxelShape frame = REVOLVING[part][turn];
                if (panel.isEmpty()) {
                    result[part] = frame;
                    continue;
                }
                VoxelShape glass = switch (layer) {
                    case 0 -> Shapes.joinUnoptimized(panel, LOWER_WING, BooleanOp.AND);
                    case 2 -> Shapes.joinUnoptimized(panel, UPPER_WING, BooleanOp.AND);
                    default -> panel;
                };
                result[part] = Shapes.or(frame, glass);
            }
        }
        return result;
    }

    private static VoxelShape[][] split(List<AABB> frame, int width, int depth, int height) {
        VoxelShape[][] result = new VoxelShape[width * depth * height][];
        for (int index = 0; index < result.length; index++) {
            int x = index % width * 16, y = index / (width * depth) * 16, z = index / width % depth * 16;
            result[index] = RestaurantKitchenBlock.rotations(section(frame, x, y, z).optimize());
        }
        return result;
    }

    private static VoxelShape section(List<AABB> pieces, int x, int y, int z) {
        VoxelShape shape = Shapes.empty();
        for (AABB piece : pieces) {
            double x0 = Math.max(0, piece.minX - x), x1 = Math.min(16, piece.maxX - x);
            double y0 = Math.max(0, piece.minY - y), y1 = Math.min(16, piece.maxY - y);
            double z0 = Math.max(0, piece.minZ - z), z1 = Math.min(16, piece.maxZ - z);
            // The caller merges the boxes once per cell, after all slices have been joined.
            if (x1 > x0 && y1 > y0 && z1 > z0)
                shape = Shapes.joinUnoptimized(shape, Block.box(x0, y0, z0, x1, y1, z1), BooleanOp.OR);
        }
        return shape;
    }
}
