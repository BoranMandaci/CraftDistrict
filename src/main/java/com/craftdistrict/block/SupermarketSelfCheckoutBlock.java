package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static kiosk, using the existing lower/upper linked structure lifecycle. */
public final class SupermarketSelfCheckoutBlock extends MultiBlockDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<SupermarketSelfCheckoutBlock> CODEC = simpleCodec(SupermarketSelfCheckoutBlock::new);
    //?}

    public SupermarketSelfCheckoutBlock(Properties properties) { super(properties); }

    @Override
    protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SupermarketShoppingShapes.get(3, partIndex(state), state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
