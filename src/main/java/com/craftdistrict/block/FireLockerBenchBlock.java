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

/** Two floor-supported cells; decorative backless bench with no seating mechanics. */
public final class FireLockerBenchBlock extends PoliceEntranceBlock {
    //? if >=1.21 {
    public static final MapCodec<FireLockerBenchBlock> CODEC = simpleCodec(FireLockerBenchBlock::new);
    //?}

    public FireLockerBenchBlock(Properties properties) { super(Variant.RECEPTION_DESK,properties); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FireFixtureShapes.get(2,partIndex(state),state);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        Direction facing=state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        // Matching benches meet flush along their width; suppress only the joined end caps.
        return direction.getAxis()==facing.getClockWise().getAxis()
                && adjacentState.is(this)
                && adjacentState.getValue(BlockStateProperties.HORIZONTAL_FACING)==facing
                || super.skipRendering(state,adjacentState,direction);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
