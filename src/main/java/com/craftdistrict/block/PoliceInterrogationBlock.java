package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Three ordinary floor cells extending lengthwise behind the front-facing end. */
public final class PoliceInterrogationBlock extends MultiBlockDecorationBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    //? if >=1.21 {
    public static final MapCodec<PoliceInterrogationBlock> CODEC = simpleCodec(PoliceInterrogationBlock::new);
    //?}

    public PoliceInterrogationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected int partCount() {
        return 3;
    }

    @Override
    protected int partIndex(BlockState state) {
        return state.getValue(PART);
    }

    @Override
    protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PART, index);
    }

    @Override
    protected BlockPos partOffset(BlockState state, int index) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return BlockPos.ZERO.relative(facing.getOpposite(), index);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.below();
        return level.getBlockState(support).isFaceSturdy(level, support, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PolicePackage2Shapes.get(1, partIndex(state), state);
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
