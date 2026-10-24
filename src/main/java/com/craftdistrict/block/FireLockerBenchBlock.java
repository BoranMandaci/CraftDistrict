package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import com.craftdistrict.entity.Seating;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two floor-supported cells, each with a sitting position on the existing slatted surface. */
public final class FireLockerBenchBlock extends PoliceEntranceBlock implements SittableBlock {
    //? if >=1.21 {
    public static final MapCodec<FireLockerBenchBlock> CODEC = simpleCodec(FireLockerBenchBlock::new);
    //?}

    public FireLockerBenchBlock(Properties properties) { super(Variant.RECEPTION_DESK,properties); }

    @Override public double seatHeight() { return 8.75 / 16; }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Seating.sit(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? Seating.sit(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) || replacement.getValue(FACING) != state.getValue(FACING)) Seating.removeAt(level, pos);
        super.onRemove(state, level, pos, replacement, moving);
    }

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
