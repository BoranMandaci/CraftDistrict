package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vertical two-part machine; all placement and linked removal stay in the shared system. */
public final class IndustrialDoughMixerBlock extends MultiBlockDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<IndustrialDoughMixerBlock> CODEC = simpleCodec(IndustrialDoughMixerBlock::new);
    //?}

    public IndustrialDoughMixerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return Direction.UP;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return IndustrialDoughMixerShapes.SHAPES[partIndex(state)][turn];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
