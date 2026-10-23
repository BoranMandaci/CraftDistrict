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

/** Wall-facing placement and support removal are shared with existing wall decorations. */
public final class KitchenExhaustHoodBlock extends BakerySignageBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            Block.box(0, 3.75, 2.75, 16, 8, 16), Block.box(3, 8, 9, 13, 10, 16),
            Block.box(5, 10, 12, 11, 16, 16)));
    //? if >=1.21 {
    public static final MapCodec<KitchenExhaustHoodBlock> CODEC = simpleCodec(KitchenExhaustHoodBlock::new);
    //?}

    public KitchenExhaustHoodBlock(Properties properties) { super(Variant.WALL_SHOP, properties); }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
