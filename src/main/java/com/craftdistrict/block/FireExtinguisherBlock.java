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

/** Compact extinguisher using the existing clicked-wall and support-loss rules. */
public final class FireExtinguisherBlock extends BakerySignageBlock {
    //? if >=1.21 {
    public static final MapCodec<FireExtinguisherBlock> CODEC = simpleCodec(FireExtinguisherBlock::new);
    //?}

    public FireExtinguisherBlock(Properties properties) {
        super(Variant.WALL_SHOP, properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FireStationShapes.get(2, 0, state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
