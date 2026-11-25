package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DumbbellRackBlock extends GymRackBlock {
    private static final VoxelShape[][] SHAPES = createShapes();

    public DumbbellRackBlock(Properties properties) {
        super(properties, SHAPES);
    }

    private static VoxelShape[][] createShapes() {
        VoxelShape[][] shapes = new VoxelShape[4][];
        for (int joins = 0; joins < shapes.length; joins++) {
            boolean left = (joins & 1) != 0, right = (joins & 2) != 0;
            double min = left ? 0 : .51, max = right ? 16 : 15.49;
            VoxelShape shape = Shapes.or(
                    box(min, 1.68, 2.39, max, 5.32, 5.53), box(min, 2.96, 5.53, max, 6.47, 8.72),
                    box(min, 5.49, 4.18, max, 9.03, 7.29), box(min, 6.7, 7.29, max, 10.12, 10.48),
                    box(min, 9.37, 6.21, max, 12.77, 9.32), box(min, 10.6, 9.32, max, 13.8, 12.51),
                    sideSupport(left ? .57 : 1.12));
            if (!right) {
                shape = Shapes.or(shape, sideSupport(14.88));
            }
            shapes[joins] = RestaurantKitchenBlock.rotations(shape);
        }
        return shapes;
    }

    private static VoxelShape sideSupport(double center) {
        double min = center - .56, max = center + .56;
        return Shapes.or(
                box(min, 0, 1.96, max, .57, 3.62), box(min, .38, 2.48, max, 3.6, 4.61),
                box(min, 3.6, 3.71, max, 7.15, 6.11), box(min, 7.15, 5.22, max, 10.88, 7.54),
                box(min, 0, 12.21, max, .54, 14.26), box(min, .34, 11.46, max, 3.45, 13.68),
                box(min, 3.45, 10.04, max, 7.15, 12.59), box(min, 7.15, 8.3, max, 11.49, 11.02),
                box(min, 2.98, 3.91, max, 3.45, 12.19), box(min, 6.49, 5.15, max, 7.15, 10.93),
                box(min, 10.11, 6.63, max, 10.76, 9.56));
    }

    //? if >=1.21 {
    public static final MapCodec<DumbbellRackBlock> CODEC = simpleCodec(DumbbellRackBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
