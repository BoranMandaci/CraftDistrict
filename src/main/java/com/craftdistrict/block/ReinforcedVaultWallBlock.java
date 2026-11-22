package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class ReinforcedVaultWallBlock extends Block {
    private static final Direction[] SIDES = Direction.values();

    public ReinforcedVaultWallBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (Direction side : SIDES) state = state.setValue(connection(side), false);
        registerDefaultState(state);
    }

    private static BooleanProperty connection(Direction side) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(side);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction side : SIDES) builder.add(connection(side));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        var level = context.getLevel();
        for (Direction side : SIDES) {
            BlockPos neighbor = context.getClickedPos().relative(side);
            state = state.setValue(connection(side), level.hasChunkAt(neighbor) && level.getBlockState(neighbor).is(this));
        }
        return state;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(connection(side), neighbor.is(this));
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            result = result.setValue(connection(rotation.rotate(side)), state.getValue(connection(side)));
        }
        return result;
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            result = result.setValue(connection(mirror.mirror(side)), state.getValue(connection(side)));
        }
        return result;
    }

    //? if >=1.21 {
    public static final MapCodec<ReinforcedVaultWallBlock> CODEC = simpleCodec(ReinforcedVaultWallBlock::new);
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    //?}
}
