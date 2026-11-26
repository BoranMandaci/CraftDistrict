package com.craftdistrict.block;

import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static com.craftdistrict.block.RestaurantKitchenBlock.rotations;
import static net.minecraft.world.level.block.Block.box;

final class GameTableShapes {
    record Layout(int width, VoxelShape[][] collision, VoxelShape[][] outline) { }

    static final Layout FOOSBALL = foosball();
    static final Layout FOOSBALL_LEGACY = legacyFoosball();
    static final Layout BILLIARD = billiard();
    static final Layout TABLE_TENNIS = tableTennis();
    static final Layout AIR_HOCKEY = airHockey();
    static final Layout AIR_HOCKEY_LEGACY = legacyAirHockey();

    private GameTableShapes() { }

    private static Layout foosball() {
        VoxelShape collision = Shapes.or(
                box(6.30, 9.25, .48, 25.70, 12.63, 31.52),
                box(6.30, 12.63, .48, 7.99, 14.17, 31.52), box(24.01, 12.63, .48, 25.70, 14.17, 31.52),
                box(7.99, 12.63, .48, 24.01, 14.17, 2.74), box(7.99, 12.63, 29.26, 24.01, 14.17, 31.52),
                feet(7.17, 24.83, 2.30, 29.70, 2.9, 2.8, 9.25),
                box(9.81, 3.40, 3.17, 22.19, 4.25, 4.23), box(9.81, 3.40, 27.77, 22.19, 4.25, 28.83),
                box(8.21, 3.40, 4.81, 9.03, 4.25, 27.19), box(22.97, 3.40, 4.81, 23.79, 4.25, 27.19));
        VoxelShape details = Shapes.or(
                box(13.04, 10.25, .31, 18.96, 11.83, .48), box(13.04, 10.25, 31.52, 18.96, 11.83, 31.69),
                box(12.15, 14.17, 1.10, 19.85, 14.64, 1.58), box(12.15, 14.17, 30.42, 19.85, 14.64, 30.90));
        double[] rows = {4.24, 7.27, 10.37, 14.0, 18.0, 21.63, 24.73, 27.76};
        for (double row : rows) {
            details = Shapes.or(details, box(1.04, 14.10, row - .45, 30.96, 15.10, row + .45),
                    box(8.79, 12.66, row - .40, 23.21, 15.96, row + .40));
        }
        return layout(2, 2, collision, details);
    }

    private static Layout legacyFoosball() {
        VoxelShape collision = Shapes.or(
                box(1.44, 9.05, .64, 14.56, 12.86, 31.36),
                box(1.44, 12.86, .64, 2.80, 14.29, 31.36), box(13.20, 12.86, .64, 14.56, 14.29, 31.36),
                box(2.80, 12.86, .64, 13.20, 14.29, 2.75), box(2.80, 12.86, 29.25, 13.20, 14.29, 31.36),
                feet(2.14, 13.86, 2.44, 29.56, 2.24, 2.83, 9.10),
                box(3.26, 3.34, 3.03, 12.74, 4.20, 4.68), box(3.26, 3.34, 27.32, 12.74, 4.20, 28.97));
        VoxelShape rods = Shapes.empty();
        double[] rows = {4.25, 7.24, 10.35, 14.03, 17.97, 21.65, 24.76, 27.75};
        for (double row : rows) {
            rods = Shapes.or(rods, box(.24, 14.12, row - .45, 15.76, 15.02, row + .45),
                    box(3.56, 12.86, row - .40, 12.44, 15.96, row + .40));
        }
        return layout(1, 2, collision, rods);
    }

    private static Layout billiard() {
        VoxelShape collision = Shapes.or(
                box(1.8, 8.95, 1.55, 30.2, 12.40, 46.45),
                box(1.80, 12.40, 1.55, 3.12, 14.22, 46.45), box(28.88, 12.40, 1.55, 30.20, 14.22, 46.45),
                box(3.12, 12.40, 1.55, 28.88, 14.22, 2.86), box(3.12, 12.40, 45.14, 28.88, 14.22, 46.45),
                box(5.14, 12.40, 2.86, 26.86, 13.84, 4.20), box(5.14, 12.40, 43.80, 26.86, 13.84, 45.14),
                box(3.12, 12.40, 5.30, 4.65, 13.84, 22.59), box(3.12, 12.40, 25.41, 4.65, 13.84, 42.70),
                box(27.35, 12.40, 5.30, 28.88, 13.84, 22.59), box(27.35, 12.40, 25.41, 28.88, 13.84, 42.70),
                feet(3.08, 28.92, 4.15, 43.85, 2.82, 2.82, 8.95));
        VoxelShape details = Shapes.or(
                billiardBallOutline(9.06, 12.5), billiardBallOutline(24.1, 17.335),
                billiardBallOutline(14.618, 35.867), billiardBallOutline(17.44, 33.338),
                billiardBallOutline(17.946, 36.533), billiardBallOutline(20.742, 38.929),
                billiardBallOutline(20.769, 34.11), billiardBallOutline(23.404, 36.453));
        return layout(2, 3, collision, details);
    }

    private static VoxelShape billiardBallOutline(double x, double z) {
        double radius = 0.8925;
        return box(x - radius, 12.4, z - radius, x + radius, 14.185, z + radius);
    }

    private static Layout tableTennis() {
        VoxelShape collision = Shapes.or(
                box(1.94, 11.98, 1.48, 30.06, 13.35, 46.52),
                feet(4.32, 27.68, 4.62, 43.38, 2.17, 2.12, 11.98),
                box(5.15, 1.42, 4.92, 26.85, 2.38, 6.36), box(5.15, 1.42, 41.64, 26.85, 2.38, 43.08),
                box(4.73, 10.76, 5.6, 27.27, 11.98, 7.58), box(4.73, 10.76, 40.42, 27.27, 11.98, 42.4));
        VoxelShape details = Shapes.or(box(1.38, 12.94, 23.44, 30.62, 15.89, 24.56),
                box(6.57, 13.35, 8.66, 10.35, 13.78, 13.60), box(23.35, 13.35, 34.68, 26.64, 13.78, 39.76),
                box(10.84, 13.35, 13.47, 11.66, 14.16, 14.29));
        return layout(2, 3, collision, details);
    }

    private static Layout airHockey() {
        VoxelShape collision = Shapes.or(
                box(3.80, 8.70, 1.85, 28.20, 13.55, 46.15),
                box(3.80, 13.55, 1.85, 5.55, 15.65, 46.15), box(26.45, 13.55, 1.85, 28.20, 15.65, 46.15),
                box(5.55, 13.55, 1.85, 26.45, 15.88, 3.72), box(5.55, 13.55, 44.28, 26.45, 15.88, 46.15),
                feet(4.80, 27.20, 5.1, 42.90, 3.70, 3.80, 8.7),
                box(7.80, 2.6, 6.1, 24.20, 3.72, 7.9), box(7.80, 2.6, 40.1, 24.20, 3.72, 41.9));
        VoxelShape details = Shapes.or(box(7.81, 13.55, 11.63, 10.59, 15.06, 14.37),
                box(21.61, 13.55, 33.93, 24.39, 15.06, 36.67), box(17.03, 13.55, 23.63, 18.57, 13.86, 25.07));
        return layout(2, 3, collision, details);
    }

    private static Layout legacyAirHockey() {
        VoxelShape collision = Shapes.or(
                box(.85, 8.15, 1.25, 15.15, 11.98, 30.75),
                box(.85, 11.98, 1.25, 2.25, 13.25, 30.75), box(13.75, 11.98, 1.25, 15.15, 13.25, 30.75),
                box(2.25, 11.98, 1.25, 13.75, 13.45, 2.64), box(2.25, 11.98, 29.36, 13.75, 13.45, 30.75),
                feet(1.9, 14.1, 3.65, 28.35, 2.80, 2.76, 8.15),
                box(3.28, 2.85, 4.15, 12.72, 3.7, 5.5), box(3.28, 2.85, 26.5, 12.72, 3.7, 27.85));
        VoxelShape details = Shapes.or(box(3.4, 11.98, 7.25, 6.0, 13.23, 9.85), box(9.62, 11.98, 22.50, 12.23, 13.23, 25.10),
                box(8.28, 11.98, 16.78, 9.52, 12.31, 18.02));
        return layout(1, 2, collision, details);
    }

    private static VoxelShape feet(double left, double right, double front, double back, double width, double depth, double height) {
        return Shapes.or(box(left, 0, front, left + width, height, front + depth),
                box(right - width, 0, front, right, height, front + depth),
                box(left, 0, back - depth, left + width, height, back),
                box(right - width, 0, back - depth, right, height, back));
    }

    private static Layout layout(int width, int length, VoxelShape collision, VoxelShape details) {
        VoxelShape outline = Shapes.or(collision, details);
        VoxelShape[][] collisions = new VoxelShape[width * length][];
        VoxelShape[][] outlines = new VoxelShape[width * length][];
        for (int i = 0; i < collisions.length; i++) {
            int x = i % width;
            int z = i / width;
            collisions[i] = rotations(Shapes.join(collision.move(-x, 0, -z), Shapes.block(), BooleanOp.AND));
            outlines[i] = rotations(Shapes.join(outline.move(-x, 0, -z), Shapes.block(), BooleanOp.AND));
        }
        return new Layout(width, collisions, outlines);
    }
}
