package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The existing three-column workstation layout, with separately cached fire-desk shapes. */
public final class FireDispatchDeskBlock extends PoliceWorkroomBlock {
    public static final BooleanProperty END = BooleanProperty.create("end");
    //? if >=1.21 {
    public static final MapCodec<FireDispatchDeskBlock> CODEC = simpleCodec(FireDispatchDeskBlock::new);
    //?}

    public FireDispatchDeskBlock(Properties properties) { super(Variant.DISPATCH, properties); }

    @Override
    protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(END);
    }

    @Override
    protected int partCount() { return 3; }

    @Override
    protected int partIndex(BlockState state) {
        return state.getValue(PRIMARY) ? 0 : state.getValue(END) ? 2 : 1;
    }

    @Override
    protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PRIMARY, index == 0).setValue(END, index == 2);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FireInteriorShapes.get(3, partIndex(state), state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
