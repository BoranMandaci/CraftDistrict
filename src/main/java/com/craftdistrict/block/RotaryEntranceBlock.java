package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A fixed three-block frame with one non-ticking controller at its center. */
public final class RotaryEntranceBlock extends MultiBlockDecorationBlock implements EntityBlock {
    private static final int WIDTH = 3;
    private static final int LAYER_SIZE = WIDTH * WIDTH;
    private static final int PART_COUNT = LAYER_SIZE * WIDTH;
    private static final int CONTROLLER_PART = 13;
    private static final int ROTOR_MODEL_PART = 27;
    private static final IntegerProperty PART = IntegerProperty.create("part", 0, 37);

    private final BlockState rotorModel;

    //? if >=1.21 {
    public static final MapCodec<RotaryEntranceBlock> CODEC = simpleCodec(RotaryEntranceBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}

    public RotaryEntranceBlock(Properties properties) {
        super(properties);
        rotorModel = defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PART, ROTOR_MODEL_PART);
    }

    /** Loads a single canonical wing through the normal model bakery, including resource reloads. */
    public BlockState rotorModelState() { return rotorModel; }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }
    @Override protected int partCount() { return PART_COUNT; }
    @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }

    @Override protected BlockPos partOffset(BlockState state, int index) {
        Direction facing = state.getValue(FACING);
        return BlockPos.ZERO.relative(facing.getClockWise(), index % WIDTH)
                .relative(facing.getOpposite(), index / WIDTH % WIDTH).above(index / LAYER_SIZE);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return partIndex(state) == CONTROLLER_PART ? new RotaryEntranceBlockEntity(pos, state) : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int index = partIndex(state);
        return index < PART_COUNT && (DecorationSupport.isPlaced(state, level, pos)
                || Block.canSupportCenter(level, pos.below(index / LAYER_SIZE + 1), Direction.UP));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = partIndex(state);
        VoxelShape[][] sections = RotaryEntranceShapes.REVOLVING;
        return index < sections.length ? sections[index][RestaurantKitchenBlock.turn(state)] : Shapes.empty();
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Only the stationary frame participates in world collision.
        return getShape(state, level, pos, context);
    }
}
