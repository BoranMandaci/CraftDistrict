package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class HotelSingleElevatorDoorBlock extends HotelElevatorDoorsBlock {
    private static final VoxelShape JAMBS = Shapes.or(box(0, 0, 13.5, 3, 16, 16),
            box(14.5, 0, 13.5, 16, 16, 16));
    private static final VoxelShape[] OPEN_LOWER =
            RestaurantKitchenBlock.rotations(Shapes.or(JAMBS, box(3, 0, 13.5, 14.5, .375, 16)));
    private static final VoxelShape[] OPEN_UPPER =
            RestaurantKitchenBlock.rotations(Shapes.or(JAMBS, box(3, 14.75, 13.5, 14.5, 16, 16)));

    public HotelSingleElevatorDoorBlock(Properties properties) {
        super(properties);
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PRIMARY, BlockStateProperties.OPEN);
    }

    @Override protected int partCount() { return 2; }
    @Override protected int partIndex(BlockState state) { return state.getValue(PRIMARY) ? 0 : 1; }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PRIMARY, index == 0); }
    @Override protected BlockPos partOffset(BlockState state, int index) { return BlockPos.ZERO.above(index); }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override protected VoxelShape[] openShapes(BlockState state) {
        return state.getValue(PRIMARY) ? OPEN_LOWER : OPEN_UPPER;
    }

    //? if >=1.21 {
    public static final MapCodec<HotelSingleElevatorDoorBlock> CODEC = simpleCodec(HotelSingleElevatorDoorBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
