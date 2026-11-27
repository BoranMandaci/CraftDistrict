package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BoardGameShelfBlock extends GymRackBlock {
    private static final VoxelShape[][] SHAPES = createShapes();

    public BoardGameShelfBlock(Properties properties) {
        super(properties, SHAPES);
    }

    private static VoxelShape[][] createShapes() {
        VoxelShape body = Shapes.or(
                box(0, 0, 0, 16, 1.28, 16), box(0, 14.75, 0, 16, 16, 16),
                box(0, 7.25, 0, 16, 8.10, 14.82), box(0, 1.28, 14.82, 16, 14.75, 16),
                box(1.61, 1.28, 2.97, 7.28, 4.75, 12.15),
                box(7.85, 1.28, 2.50, 14.34, 6.87, 12.38),
                box(1.65, 8.10, 2.48, 14.32, 13.94, 12.69));
        VoxelShape[][] shapes = new VoxelShape[4][];
        for (int joins = 0; joins < shapes.length; joins++) {
            boolean left = (joins & 1) != 0;
            boolean right = (joins & 2) != 0;
            // One narrow divider belongs to the module on the right of each join.
            VoxelShape shape = Shapes.or(body, box(0, 1.28, left ? .82 : 0, left ? .48 : 1.10, 14.75, 14.82));
            if (!right) {
                shape = Shapes.or(shape, box(14.90, 1.28, 0, 16, 14.75, 14.82));
            }
            shapes[joins] = RestaurantKitchenBlock.rotations(shape);
        }
        return shapes;
    }

    //? if >=1.21 {
    public static final MapCodec<BoardGameShelfBlock> CODEC = simpleCodec(BoardGameShelfBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
