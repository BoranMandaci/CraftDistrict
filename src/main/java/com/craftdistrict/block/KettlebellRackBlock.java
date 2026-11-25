package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class KettlebellRackBlock extends GymRackBlock {
    private static final VoxelShape[][] SHAPES = createShapes();

    public KettlebellRackBlock(Properties properties) {
        super(properties, SHAPES);
    }

    private static VoxelShape[][] createShapes() {
        VoxelShape[][] result = new VoxelShape[4][];
        for (int joins = 0; joins < result.length; joins++) {
            boolean left = (joins & 1) != 0, right = (joins & 2) != 0;
            double min = left ? 0 : .75, max = right ? 16 : 15.25;
            VoxelShape shape = Shapes.or(
                    box(min, 3.3, 3.13, max, 4.5, 11.45),
                    box(min, 9.89, 4.13, max, 11.08, 11.87),
                    box(1.90, 4.18, 4.15, 5.91, 8.42, 7.46), box(4.31, 4.18, 7.51, 9.53, 9.59, 11.97),
                    box(8.11, 4.18, 4.01, 14.01, 9.77, 8.82),
                    box(.78, 10.76, 6.35, 4.37, 14.55, 9.31), box(4.75, 10.76, 6.21, 9.37, 15.58, 10.14),
                    box(9.72, 10.76, 6.07, 14.92, 15.86, 10.32),
                    support(left ? .7 : 1.45));
            if (!right) {
                shape = Shapes.or(shape, support(14.55));
            }
            result[joins] = RestaurantKitchenBlock.rotations(shape);
        }
        return result;
    }

    private static VoxelShape support(double center) {
        return Shapes.or(
                box(center - .61, 0, 2.6, center + .61, 1.03, 12.49),
                box(center - .46, .79, 3.33, center + .46, 4.74, 5.86),
                box(center - .46, 4.74, 4.8, center + .46, 9.36, 7.62),
                box(center - .46, 9.10, 6.59, center + .46, 10.46, 8.08),
                box(center - .43, .79, 9.05, center + .43, 4.75, 11.56),
                box(center - .43, 4.75, 7.57, center + .43, 8.72, 10.1),
                box(center - .43, 8.48, 7.11, center + .43, 9.82, 8.54),
                box(center - .55, 2.91, 4.19, center + .55, 3.46, 10.89),
                box(center - .56, 9.65, 6.37, center + .56, 10.18, 8.92));
    }

    //? if >=1.21 {
    public static final MapCodec<KettlebellRackBlock> CODEC = simpleCodec(KettlebellRackBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
