package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import com.craftdistrict.entity.Seating;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Three linked seats; old primary states keep their first/second-part meaning. */
public final class PoliceWaitingBenchBlock extends PoliceEntranceBlock implements SittableBlock {
    public static final BooleanProperty END = BooleanProperty.create("end");
    //? if >=1.21 {
    public static final MapCodec<PoliceWaitingBenchBlock> CODEC = simpleCodec(PoliceWaitingBenchBlock::new);
    //?}

    public PoliceWaitingBenchBlock(Properties properties) {
        super(Variant.WAITING_BENCH, properties);
    }

    @Override
    protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(END);
    }

    @Override
    protected int partCount() {
        return 3;
    }

    @Override
    protected int partIndex(BlockState state) {
        return state.getValue(PRIMARY) ? 0 : state.getValue(END) ? 2 : 1;
    }

    @Override
    protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PRIMARY, index == 0).setValue(END, index == 2);
    }

    @Override public double seatHeight() { return 8.0 / 16; }
    @Override public double forwardOffset() { return 1.0 / 16; }

    // The clicked segment owns its own passenger anchor: three seats, no shared occupancy.
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
        // Shared linked removal calls this hook on all three segments.
        super.onRemove(state, level, pos, replacement, moving);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
