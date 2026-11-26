package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.UnaryOperator;

/** A frame tube that joins neighboring tubes along all three axes. */
public final class FootballGoalPostBlock extends PipeBlock {
    private static final float HALF_WIDTH = 1.5625F;
    private static final VoxelShape STANDALONE = box(8 - HALF_WIDTH, 0, 8 - HALF_WIDTH, 8 + HALF_WIDTH, 16, 8 + HALF_WIDTH);
    //? if >=1.21 {
    public static final MapCodec<FootballGoalPostBlock> CODEC = simpleCodec(FootballGoalPostBlock::new);
    @Override protected MapCodec<? extends PipeBlock> codec() { return CODEC; }
    //?}

    public FootballGoalPostBlock(Properties properties) {
        super(HALF_WIDTH / 16, properties);
        registerDefaultState(defaultBlockState().setValue(DOWN, false).setValue(UP, false)
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DOWN, UP, NORTH, EAST, SOUTH, WEST);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (var side : PROPERTY_BY_DIRECTION.entrySet()) {
            BlockPos neighbor = pos.relative(side.getKey());
            state = state.setValue(side.getValue(), connects(side.getKey(), level.getBlockState(neighbor), level, neighbor));
        }
        return state;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(side), connects(side, neighbor, level, otherPos));
    }

    private boolean connects(Direction side, BlockState neighbor, BlockGetter level, BlockPos pos) {
        return neighbor.is(this) || (side == Direction.DOWN && neighbor.isFaceSturdy(level, pos, Direction.UP));
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return transform(state, rotation::rotate);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return transform(state, mirror::mirror);
    }

    private static BlockState transform(BlockState state, UnaryOperator<Direction> direction) {
        BlockState result = state;
        for (var side : PROPERTY_BY_DIRECTION.entrySet()) {
            result = result.setValue(PROPERTY_BY_DIRECTION.get(direction.apply(side.getKey())), state.getValue(side.getValue()));
        }
        return result;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(UP) && !state.getValue(NORTH) && !state.getValue(EAST)
                && !state.getValue(SOUTH) && !state.getValue(WEST)) return STANDALONE;
        return super.getShape(state, level, pos, context);
    }
}
