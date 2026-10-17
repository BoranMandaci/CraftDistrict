package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four ordinary blocks; placement and removal belong to the existing 2x2 system. */
public final class SupermarketGondolaBlock extends WideTallDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<SupermarketGondolaBlock> CODEC = simpleCodec(SupermarketGondolaBlock::new);
    //?}

    // Symmetric front/back: only row and north/south versus east/west matter.
    // Broad structural envelopes deliberately omit individual merchandise.
    private static final VoxelShape[][] SHAPES = {
            {
                    Shapes.or(Block.box(0, 0, 1.5, 16, 1.5, 14.5),
                            Block.box(0, 1.5, 1.25, 16, 16, 14.75)).optimize(),
                    Shapes.or(Block.box(1.5, 0, 0, 14.5, 1.5, 16),
                            Block.box(1.25, 1.5, 0, 14.75, 16, 16)).optimize()
            },
            {
                    Block.box(0, 0, 1.25, 16, 15.25, 14.75),
                    Block.box(1.25, 0, 0, 14.75, 15.25, 16)
            }
    };

    public SupermarketGondolaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[partIndex(state) / 2][state.getValue(FACING).getAxis() == Direction.Axis.X ? 1 : 0];
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
