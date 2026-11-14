package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Reuse the established two-high cabinet row connections and paired placement/removal. */
public final class DepositBoxShelfBlock extends MedicalSupplyCabinetBlock {
    //? if >=1.21 {
    public static final MapCodec<DepositBoxShelfBlock> CODEC = simpleCodec(DepositBoxShelfBlock::new);
    //?}
    public DepositBoxShelfBlock(Properties properties) { super(properties); }
    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        return side.getAxis().isHorizontal() && side.getAxis() != state.getValue(FACING).getAxis()
                && neighbor.is(this) && neighbor.getValue(FACING) == state.getValue(FACING)
                && neighbor.getValue(PRIMARY) == state.getValue(PRIMARY) || super.skipRendering(state, neighbor, side);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
