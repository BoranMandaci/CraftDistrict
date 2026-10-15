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

/** Rear/handle cell is primary; the front basket cell extends toward facing.
 * Legacy facing-only states load as primary, without writing into occupied neighbors.
 */
public final class SupermarketShoppingCartBlock extends MultiBlockDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<SupermarketShoppingCartBlock> CODEC = simpleCodec(SupermarketShoppingCartBlock::new);
    //?}

    public SupermarketShoppingCartBlock(Properties properties) { super(properties); }

    @Override
    protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SupermarketShoppingShapes.get(1, partIndex(state), state);
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
