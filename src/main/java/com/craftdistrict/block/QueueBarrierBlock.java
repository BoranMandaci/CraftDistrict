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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One centered post and at most four half-span belts; sixteen cached, world-aligned states. */
public final class QueueBarrierBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
            SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
    private static final BooleanProperty[] JOINS = {NORTH, EAST, SOUTH, WEST};
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final VoxelShape[] COLLISION = shapes(.125), OUTLINE = shapes(.25);
    //? if >=1.21 {
    public static final MapCodec<QueueBarrierBlock> CODEC = simpleCodec(QueueBarrierBlock::new);
    //?}

    public QueueBarrierBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }
    private static int index(Direction side) {
        return switch (side) { case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3;
            default -> throw new IllegalArgumentException("Queue belts are horizontal"); };
    }
    private static int mask(BlockState state) {
        int mask = 0;
        for (int i = 0; i < 4; i++) if (state.getValue(JOINS[i])) mask |= 1 << i;
        return mask;
    }
    private static VoxelShape[] shapes(double halfWidth) {
        VoxelShape[] shapes = new VoxelShape[16];
        VoxelShape post = Shapes.or(Block.box(4.5, 0, 4.5, 11.5, 1, 11.5),
                Block.box(7.25, 1, 7.25, 8.75, 12.5, 8.75), Block.box(6.75, 12.5, 6.75, 9.25, 14.25, 9.25));
        VoxelShape[] belts = {Block.box(8-halfWidth, 12.5, 0, 8+halfWidth, 13.75, 6.75),
                Block.box(9.25, 12.5, 8-halfWidth, 16, 13.75, 8+halfWidth),
                Block.box(8-halfWidth, 12.5, 9.25, 8+halfWidth, 13.75, 16),
                Block.box(0, 12.5, 8-halfWidth, 6.75, 13.75, 8+halfWidth)};
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = post;
            for (int i = 0; i < 4; i++) if ((mask & (1 << i)) != 0) shape = Shapes.or(shape, belts[i]);
            shapes[mask] = shape.optimize();
        }
        return shapes;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (!canSurvive(defaultBlockState(), context.getLevel(), pos)) return null;
        BlockState state = defaultBlockState();
        for (int i = 0; i < 4; i++) {
            BlockPos neighbor = pos.relative(SIDES[i]);
            state = state.setValue(JOINS[i], context.getLevel().hasChunkAt(neighbor) && context.getLevel().getBlockState(neighbor).is(this));
        }
        return state;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return side.getAxis().isHorizontal() ? state.setValue(JOINS[index(side)], other.is(this)) : state;
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state;
        for (int i = 0; i < 4; i++) result = result.setValue(JOINS[index(rotation.rotate(SIDES[i]))], state.getValue(JOINS[i]));
        return result;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.LEFT_RIGHT) return state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
        if (mirror == Mirror.FRONT_BACK) return state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
        return state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return OUTLINE[mask(state)]; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return COLLISION[mask(state)]; }
    //? if >=1.21 {
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    //?}
}
