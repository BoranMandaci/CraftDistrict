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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Six reserved cells around a walkable arch; segment=0 retains the old two-cell scanner. */
public final class SecurityScannerBlock extends MultiBlockDecorationBlock {
    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 6);
    private static final VoxelShape[][] SHAPES = createShapes();
    private static final VoxelShape[][] LEGACY_SHAPES = {
            RestaurantKitchenBlock.rotations(legacyColumns()),
            RestaurantKitchenBlock.rotations(Shapes.or(legacyColumns(), Block.box(2.5, 13.25, 4.5, 13.5, 16, 11.5)))
    };
    //? if >=1.21 {
    public static final MapCodec<SecurityScannerBlock> CODEC = simpleCodec(SecurityScannerBlock::new);
    //?}

    public SecurityScannerBlock(Properties properties) { super(properties); }

    private static VoxelShape[][] createShapes() {
        VoxelShape[][] shapes = new VoxelShape[6][];
        for (int index = 0; index < 6; index++) {
            boolean left = index % 2 == 0;
            int row = index / 2;
            VoxelShape column = Block.box(left ? 4 : 9, 0, 4.5, left ? 7 : 12, row == 2 ? 1 : 16, 11.5);
            if (row == 2) column = Shapes.or(column, Block.box(left ? 4.5 : 0, 1, 4.5, left ? 16 : 11.5, 4, 11.5));
            shapes[index] = RestaurantKitchenBlock.rotations(column);
        }
        return shapes;
    }

    private static VoxelShape legacyColumns() {
        return Shapes.or(Block.box(0, 0, 4.5, 2.5, 16, 11.5), Block.box(13.5, 0, 4.5, 16, 16, 11.5));
    }

    public static boolean isLegacy(BlockState state) { return state.getValue(SEGMENT) == 0; }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PRIMARY, SEGMENT); }
    @Override protected int partCount() { return 6; }
    @Override protected int partCount(BlockState state) { return isLegacy(state) ? 2 : 6; }
    @Override protected int partIndex(BlockState state) { return isLegacy(state) ? super.partIndex(state) : state.getValue(SEGMENT) - 1; }
    @Override protected BlockState stateForPart(BlockState state, int index) {
        return isLegacy(state) ? super.stateForPart(state, index)
                : state.setValue(SEGMENT, index + 1).setValue(PRIMARY, index == 0);
    }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        return isLegacy(state) ? BlockPos.ZERO.above(index)
                : BlockPos.ZERO.relative(state.getValue(FACING).getClockWise(), index % 2).above(index / 2);
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(SEGMENT, 1).setValue(PRIMARY, true);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos origin = pos.subtract(partOffset(state, partIndex(state)));
        for (int column = 0; column < (isLegacy(state) ? 1 : 2); column++) {
            BlockPos floor = origin.offset(partOffset(state, column)).below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return false;
        }
        return true;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (!canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        BlockPos origin = pos.subtract(partOffset(state, partIndex(state)));
        for (int part = 0; part < partCount(state); part++) {
            if (origin.offset(partOffset(state, part)).equals(otherPos) && other != stateForPart(state, part)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return state;
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE || isLegacy(state) ? mirrored : stateForPart(mirrored, partIndex(state) ^ 1);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = RestaurantKitchenBlock.turn(state);
        return isLegacy(state) ? LEGACY_SHAPES[partIndex(state)][turn] : SHAPES[partIndex(state)][turn];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
