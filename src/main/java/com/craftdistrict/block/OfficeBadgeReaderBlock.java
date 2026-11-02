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

/** Compact static reader; reuse the established wall-only mounting and support rules. */
public final class OfficeBadgeReaderBlock extends BakerySignageBlock {
    //? if >=1.21 {
    public static final MapCodec<OfficeBadgeReaderBlock> CODEC = simpleCodec(OfficeBadgeReaderBlock::new);
    //?}
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(5.5, 4.5, 14, 10.5, 12.5, 16));
    public OfficeBadgeReaderBlock(Properties properties) { super(Variant.WALL_SHOP, properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
