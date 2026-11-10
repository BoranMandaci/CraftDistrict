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

public final class HotelLobbyArmchairBlock extends RestaurantSeatBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(.5, 0, .5, 15.5, 8.75, 15.5), box(.5, 8.75, 12, 15.5, 15.25, 15.5),
            box(.5, 8.75, 1, 3.3125, 12.5, 12), box(12.6875, 8.75, 1, 15.5, 12.5, 12)).optimize());
    //? if >=1.21 {
    public static final MapCodec<HotelLobbyArmchairBlock> CODEC = simpleCodec(HotelLobbyArmchairBlock::new);
    //?}
    public HotelLobbyArmchairBlock(Properties properties) { super(Design.CUSHIONED, properties); }
    @Override public double seatHeight() { return 8.75 / 16; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
