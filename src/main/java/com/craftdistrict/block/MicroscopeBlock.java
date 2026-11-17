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

public final class MicroscopeBlock extends CashRegisterBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(3.5, 0, 3, 12.7, 1.86, 13), box(7.15, 1.86, 9.5, 12.3, 10.8, 12.4),
            box(4, 4.7, 3.7, 11.8, 5.66, 9.5), box(5.45, 8.5, 6.45, 10.8, 13.6, 12)));
    //? if >=1.21 {
    public static final MapCodec<MicroscopeBlock> CODEC = simpleCodec(MicroscopeBlock::new);
    //?}
    public MicroscopeBlock(Properties properties) { super(properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
