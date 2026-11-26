package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Parts zero through four retain the two wall-mounted layouts in saved worlds. */
public final class BilliardCueRackBlock extends MultiBlockDecorationBlock {
    private static final int FLOOR_START = 5;
    private static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    private static final VoxelShape[][] WALL_SHAPES = rackShapes(Shapes.or(
            box(1.30, 2.65, 12.38, 30.70, 4.2, 16), box(1.30, 25.60, 13.17, 30.70, 29.40, 16),
            box(1.65, 4.2, 14.48, 30.35, 25.6, 16), box(4.0, 4.2, 13.86, 28, 25.6, 14.48),
            box(12.08, 7.83, 12.78, 19.92, 15.9, 13.86)));
    private static final VoxelShape[][] FLOOR_SHAPES = rackShapes(Shapes.or(
            box(2.55, 0, 3.35, 5.75, 2.12, 12.65), box(26.25, 0, 3.35, 29.45, 2.12, 12.65),
            box(1.65, 1.88, 4.43, 30.35, 3.43, 11.55), box(2.55, 2.12, 9.25, 4.2, 28.72, 10.82),
            box(27.80, 2.12, 9.25, 29.45, 28.72, 10.82), box(2.75, 1.01, 9.58, 4.0, 7.25, 12.65),
            box(28.0, 1.01, 9.58, 29.25, 7.25, 12.65), box(3.03, 27.38, 9.33, 28.97, 28.72, 10.68),
            box(3.46, 5.84, 9.37, 28.54, 7.18, 10.66), box(3.78, 25.05, 7.26, 20.85, 26.35, 10.72),
            box(4.67, 3.43, 7.27, 19.57, 29.46, 8.12), box(21.56, 24.65, 8.84, 28.85, 25.51, 10.72),
            box(22.55, 16.49, 6.56, 29.41, 24.83, 7.41), box(25.73, 24.81, 6.79, 26.23, 25.13, 8.90)));

    //? if >=1.21 {
    public static final MapCodec<BilliardCueRackBlock> CODEC = simpleCodec(BilliardCueRackBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}

    public BilliardCueRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }
    @Override protected int partCount(BlockState state) { return state.getValue(PART) == 0 ? 1 : 4; }
    @Override protected int partIndex(BlockState state) {
        int part = state.getValue(PART);
        return part >= FLOOR_START ? part - FLOOR_START : Math.max(0, part - 1);
    }

    @Override protected BlockState stateForPart(BlockState state, int index) {
        int part = state.getValue(PART);
        if (part == 0) return state;
        return state.setValue(PART, (part >= FLOOR_START ? FLOOR_START : 1) + index);
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state.setValue(PART, FLOOR_START);
    }

    @Override protected BlockPos partOffset(BlockState state, int index) {
        return BlockPos.ZERO.relative(state.getValue(FACING).getClockWise(), index % 2).above(index / 2);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        if (state.getValue(PART) >= FLOOR_START) {
            BlockPos floor = pos.below(1 + partIndex(state) / 2);
            return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
        }
        Direction facing = state.getValue(FACING);
        BlockPos behind = pos.relative(facing.getOpposite());
        return level.hasChunkAt(behind) && level.getBlockState(behind).isFaceSturdy(level, behind, facing);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (state.getValue(PART) != 0) {
            int index = partIndex(state);
            Direction right = state.getValue(FACING).getClockWise();
            int partner = -1;
            if (side == Direction.UP && index < 2) partner = index + 2;
            else if (side == Direction.DOWN && index >= 2) partner = index - 2;
            else if (side == right && index % 2 == 0) partner = index + 1;
            else if (side == right.getOpposite() && index % 2 == 1) partner = index - 1;
            if (partner >= 0 && neighbor != stateForPart(state, partner) && !isRemovingLinkedStructure(state, level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, side, neighbor, level, pos, otherPos);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState rotated = super.mirror(state, mirror);
        return mirror == Mirror.NONE || state.getValue(PART) == 0 ? rotated : stateForPart(rotated, partIndex(state) ^ 1);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = state.getValue(PART);
        if (part == 0) return CollectionAccentBlock.shapeFor(CollectionAccentBlock.Design.BILLIARD_CUE_RACK, state);
        VoxelShape[][] shapes = part >= FLOOR_START ? FLOOR_SHAPES : WALL_SHAPES;
        return shapes[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static VoxelShape[][] rackShapes(VoxelShape rack) {
        VoxelShape[][] result = new VoxelShape[4][];
        for (int part = 0; part < result.length; part++) {
            VoxelShape local = Shapes.join(rack.move(-(part % 2), -(part / 2), 0), Shapes.block(), BooleanOp.AND);
            result[part] = RestaurantKitchenBlock.rotations(local);
        }
        return result;
    }
}
