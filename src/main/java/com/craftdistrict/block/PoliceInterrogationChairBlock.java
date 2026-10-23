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

/** Fixed institutional chair on the existing solid-surface lifecycle; shared sitting behavior. */
public final class PoliceInterrogationChairBlock extends RestaurantSeatBlock {
    //? if >=1.21 {
    public static final MapCodec<PoliceInterrogationChairBlock> CODEC = simpleCodec(PoliceInterrogationChairBlock::new);
    //?}

    public PoliceInterrogationChairBlock(Properties properties) {
        super(Design.DINER, properties);
    }

    @Override public double seatHeight() { return 8.5/16; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PolicePackage2Shapes.get(3, 0, state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
