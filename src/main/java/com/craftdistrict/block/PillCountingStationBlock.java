package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A low countertop setup; thin tools and pills share a few practical collision envelopes. */
public final class PillCountingStationBlock extends CashRegisterBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1, 0, 1, 15, .5, 15), box(1.5, .5, 2, 10.25, 1.5, 13.5),
            box(10.75, .5, 2.75, 14.5, 5.75, 9.5), box(10.5, .5, 10, 15, 2.25, 14.5)).optimize());
    //? if >=1.21 {
    public static final MapCodec<PillCountingStationBlock> CODEC = simpleCodec(PillCountingStationBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
    public PillCountingStationBlock(Properties properties) { super(properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
}
