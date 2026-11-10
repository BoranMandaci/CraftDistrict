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
            box(.5, 0, .5, 15.5, 9.5, 15.5), box(.5, 9.5, 12, 15.5, 16, 15.5),
            box(.5, 9.5, 1, 3, 13, 12), box(13, 9.5, 1, 15.5, 13, 12)).optimize());
    //? if >=1.21 {
    public static final MapCodec<HotelLobbyArmchairBlock> CODEC = simpleCodec(HotelLobbyArmchairBlock::new);
    //?}
    public HotelLobbyArmchairBlock(Properties properties) { super(Design.CUSHIONED, properties); }
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
