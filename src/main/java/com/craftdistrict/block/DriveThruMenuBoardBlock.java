package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DriveThruMenuBoardBlock extends WideTallDecorationBlock {
    public static final BooleanProperty WIDE = BooleanProperty.create("wide");
    private static final VoxelShape[] LOWER = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1.5, 0, 5.35, 14.5, 1.45, 13.05),
            box(3.1, 1.45, 8, 4.85, 16, 10.15), box(11.15, 1.45, 8, 12.9, 16, 10.15)));
    private static final VoxelShape[] UPPER = RestaurantKitchenBlock.rotations(Shapes.or(
            box(3.1, 0, 8, 4.85, 2.5, 10.15), box(11.15, 0, 8, 12.9, 2.5, 10.15),
            box(.75, 2.5, 6.14, 15.25, 13.8, 11.24)));

    private static final VoxelShape[][] WIDE_SHAPES = {
            wideLower(3.4), wideLower(10.65), wideUpper(3.4, .64, 16), wideUpper(10.65, 0, 15.36)
    };

    public DriveThruMenuBoardBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WIDE, false));
    }

    private static VoxelShape[] wideLower(double x) {
        return RestaurantKitchenBlock.rotations(Shapes.or(box(x - 1.5, 0, 6.25, x + 3.45, 1.1, 12.4),
                box(x, 1.1, 8, x + 1.95, 16, 10.15)));
    }

    private static VoxelShape[] wideUpper(double x, double minX, double maxX) {
        return RestaurantKitchenBlock.rotations(Shapes.or(box(x, 0, 8, x + 1.95, 2, 10.15),
                box(minX, 2, 5.92, maxX, 14.25, 11.53)));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(WIDE, PRIMARY);
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return super.getPlacementState(context).setValue(WIDE, true);
    }

    @Override protected int partCount(BlockState state) {
        return state.getValue(WIDE) ? 4 : 2;
    }

    @Override protected int partIndex(BlockState state) {
        return state.getValue(WIDE) ? super.partIndex(state) : state.getValue(PRIMARY) ? 0 : 1;
    }

    @Override protected BlockState stateForPart(BlockState state, int index) {
        return super.stateForPart(state, state.getValue(WIDE) ? index : 0).setValue(PRIMARY, index == 0);
    }

    @Override protected BlockPos partOffset(BlockState state, int index) {
        return state.getValue(WIDE) ? super.partOffset(state, index) : BlockPos.ZERO.above(index);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (!state.getValue(WIDE)) return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        BlockState mirrored = super.mirror(state, mirror);
        return mirrored.setValue(PRIMARY, mirrored.getValue(PART) == 0);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        int index = partIndex(state);
        int height = state.getValue(WIDE) ? index / 2 : index;
        BlockPos floor = pos.below(height + 1);
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean wide = state.getValue(WIDE);
        int index = partIndex(state);
        Direction vertical = (wide ? index < 2 : index == 0) ? Direction.UP : Direction.DOWN;
        Direction width = state.getValue(FACING).getClockWise();
        int expected = -1;
        if (side == vertical) expected = index ^ (wide ? 2 : 1);
        else if (wide && side == (index % 2 == 0 ? width : width.getOpposite())) expected = index ^ 1;
        if (expected >= 0 && !isRemovingLinkedStructure(state, level, pos)
                && neighbor != stateForPart(state, expected)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape[] shape = state.getValue(WIDE) ? WIDE_SHAPES[partIndex(state)] : state.getValue(PRIMARY) ? LOWER : UPPER;
        return shape[RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<DriveThruMenuBoardBlock> CODEC = simpleCodec(DriveThruMenuBoardBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
