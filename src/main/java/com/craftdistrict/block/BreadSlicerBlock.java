package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Reuses the established solid-top mounting rules, with its own machine shape. */
public final class BreadSlicerBlock extends CashRegisterBlock {
    //? if >=1.21 {
    public static final MapCodec<BreadSlicerBlock> CODEC = simpleCodec(BreadSlicerBlock::new);
    //?}

    public BreadSlicerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return BreadSlicerShapes.SHAPES[turn];
    }

    // Inherited collision dispatches here; placement and support loss use the
    // same solid upper-face checks as the existing countertop decorations.
    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
