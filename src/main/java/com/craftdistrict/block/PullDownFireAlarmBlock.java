package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A passive pull station using the existing clicked-wall placement rules. */
public final class PullDownFireAlarmBlock extends BakerySignageBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(5.8, 4.4, 14.6, 10.2, 11.6, 16));
    //? if >=1.21 {
    public static final MapCodec<PullDownFireAlarmBlock> CODEC = simpleCodec(PullDownFireAlarmBlock::new);
    //?}
    public PullDownFireAlarmBlock(Properties properties) { super(Variant.WALL_SHOP, properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
