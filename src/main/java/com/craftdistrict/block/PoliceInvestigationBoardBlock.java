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

/** Reuses the two-part wall plaque's clicked-face placement and support checks. */
public final class PoliceInvestigationBoardBlock extends PoliceEntranceBlock {
    //? if >=1.21 {
    public static final MapCodec<PoliceInvestigationBoardBlock> CODEC = simpleCodec(PoliceInvestigationBoardBlock::new);
    //?}

    public PoliceInvestigationBoardBlock(Properties properties) {
        super(Variant.DEPARTMENT_SIGN, properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PolicePackage2Shapes.get(0, partIndex(state), state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
