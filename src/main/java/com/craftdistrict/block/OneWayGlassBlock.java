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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Full collision cube; outward model faces and translucent back-face culling provide one-way visibility. */
public final class OneWayGlassBlock extends RotatableDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<OneWayGlassBlock> CODEC = simpleCodec(OneWayGlassBlock::new);
    //?}

    public OneWayGlassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        // Every face is on the cell boundary. Cull internal faces only when both
        // cubes have the same one-way orientation, including depth-stacked cubes.
        return adjacent.is(this) && adjacent.getValue(FACING) == state.getValue(FACING)
                || super.skipRendering(state, adjacent, direction);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
