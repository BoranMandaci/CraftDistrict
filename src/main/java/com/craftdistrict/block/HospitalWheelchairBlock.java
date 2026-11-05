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

/** Stationary wheelchair; sitting shares the existing seat lifecycle and cleanup. */
public final class HospitalWheelchairBlock extends RestaurantSeatBlock {
    //? if >=1.21 {
    public static final MapCodec<HospitalWheelchairBlock> CODEC = simpleCodec(HospitalWheelchairBlock::new);
    //?}
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1, 0, 2, 3.5, 11, 14.5), box(12.5, 0, 2, 15, 11, 14.5),
            box(3.5, 7, 4, 12.5, 9, 12), box(3.5, 9, 11, 12.5, 15, 13),
            box(4, 2, .5, 12, 3, 4)).optimize());
    public HospitalWheelchairBlock(Properties properties) { super(Design.MODERN, properties); }
    @Override public double seatHeight() { return 8.5 / 16; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
