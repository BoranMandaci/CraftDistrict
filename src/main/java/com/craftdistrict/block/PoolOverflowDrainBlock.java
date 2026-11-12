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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One shallow grate per cell; cached arms also support corners and junctions. */
public final class PoolOverflowDrainBlock extends RotatableDecorationBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north"), EAST = BooleanProperty.create("east"),
            SOUTH = BooleanProperty.create("south"), WEST = BooleanProperty.create("west");
    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    static {
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = box(6, 0, 6, 10, 1, 10);
            if ((mask & 1) != 0) shape = Shapes.or(shape, box(6, 0, 0, 10, 1, 6));
            if ((mask & 2) != 0) shape = Shapes.or(shape, box(10, 0, 6, 16, 1, 10));
            if ((mask & 4) != 0) shape = Shapes.or(shape, box(6, 0, 10, 10, 1, 16));
            if ((mask & 8) != 0) shape = Shapes.or(shape, box(0, 0, 6, 6, 1, 10));
            SHAPES[mask] = shape.optimize();
        }
    }
    //? if >=1.21 {
    public static final MapCodec<PoolOverflowDrainBlock> CODEC = simpleCodec(PoolOverflowDrainBlock::new);
    //?}
    public PoolOverflowDrainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(NORTH, EAST, SOUTH, WEST);
    }
    private static BooleanProperty property(Direction direction) {
        return switch (direction) { case NORTH -> NORTH; case EAST -> EAST; case SOUTH -> SOUTH; case WEST -> WEST;
            default -> throw new IllegalArgumentException("Horizontal drain connection required"); };
    }
    private static int mask(BlockState state) {
        int mask = (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0)
                | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
        if (mask == 0) return state.getValue(FACING).getAxis() == Direction.Axis.Z ? 5 : 10;
        return Integer.bitCount(mask) == 1 ? mask | ((mask << 2 | mask >> 2) & 15) : mask;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = context.getClickedPos().relative(side);
            state = state.setValue(property(side), context.getLevel().hasChunkAt(neighbor)
                    && context.getLevel().getBlockState(neighbor).is(this));
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return DecorationSupport.isPlaced(state, level, pos)
                || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side.getAxis().isHorizontal()) return state.setValue(property(side), neighbor.is(this));
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = super.rotate(state, rotation);
        for (Direction side : Direction.Plane.HORIZONTAL) result = result.setValue(property(rotation.rotate(side)), state.getValue(property(side)));
        return result;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        for (Direction side : Direction.Plane.HORIZONTAL) result = result.setValue(property(mirror.mirror(side)), state.getValue(property(side)));
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[mask(state)]; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
