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

/** Fixed scanner geometry on the existing solid-surface decoration lifecycle. */
public final class PoliceFingerprintBlock extends CashRegisterBlock {
    //? if >=1.21 {
    public static final MapCodec<PoliceFingerprintBlock> CODEC = simpleCodec(PoliceFingerprintBlock::new);
    //?}

    public PoliceFingerprintBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PolicePackage2Shapes.get(2, 0, state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
