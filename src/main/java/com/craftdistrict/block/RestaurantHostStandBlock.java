package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floor-supported podium; no interaction or storage. */
public final class RestaurantHostStandBlock extends CashRegisterBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            Block.box(1, 0, 2, 15, 16, 14), Block.box(3, 16, 4, 10, 16.5, 10.5),
            Block.box(10.75, 16, 4.25, 13.75, 18, 8.75)));
    //? if >=1.21 {
    public static final MapCodec<RestaurantHostStandBlock> CODEC = simpleCodec(RestaurantHostStandBlock::new);
    //?}
    public RestaurantHostStandBlock(Properties properties) { super(properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
