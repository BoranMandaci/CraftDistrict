package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Three linked seats; old primary states keep their first/second-part meaning. */
public final class PoliceWaitingBenchBlock extends PoliceEntranceBlock {
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

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
