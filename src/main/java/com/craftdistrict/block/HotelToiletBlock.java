package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Uses the same server-owned seat and cleanup as the other decorative chairs. */
public final class HotelToiletBlock extends RestaurantSeatBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(4, 0, 3, 12, 5.5, 15.5), box(2.625, 5.5, 1.625, 13.375, 9.875, 12.375),
            box(5.75, 5.5, 12.25, 10.25, 9.25, 13), box(3.75, 4.25, 13, 12.25, 15.75, 16),
            box(3.25, 10.25, 12.125, 12.75, 19.5, 12.625)).optimize());
    //? if >=1.21 {
    public static final MapCodec<HotelToiletBlock> CODEC = simpleCodec(HotelToiletBlock::new);
    //?}
    public HotelToiletBlock(Properties properties) { super(Design.CUSHIONED, properties); }
    @Override public double seatHeight() { return 9.875 / 16; }
    @Override public double forwardOffset() { return 1.0 / 16; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
