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

/** Pinned notices use ordinary wall placement; there is no writable GUI or block entity. */
public final class WantedPostersBlock extends BakerySignageBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(1, 2, 14.375, 15, 14, 16));
    //? if >=1.21 {
    public static final MapCodec<WantedPostersBlock> CODEC = simpleCodec(WantedPostersBlock::new);
    //?}
    public WantedPostersBlock(Properties properties) { super(Variant.WALL_SHOP, properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
