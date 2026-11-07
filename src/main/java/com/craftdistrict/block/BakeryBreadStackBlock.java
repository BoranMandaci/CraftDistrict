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

/** Loose bread reuses the lightweight, sturdy-countertop placement lifecycle. */
public final class BakeryBreadStackBlock extends CashRegisterBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1.5, 0, 2, 14.5, 2.25, 14),
            box(2.75, 2.25, 4.5, 9.75, 4.5, 7.5)).optimize());
    //? if >=1.21 {
    public static final MapCodec<BakeryBreadStackBlock> CODEC = simpleCodec(BakeryBreadStackBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}

    public BakeryBreadStackBlock(Properties properties) { super(properties); }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
}
