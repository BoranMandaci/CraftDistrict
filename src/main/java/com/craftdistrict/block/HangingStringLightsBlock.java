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

/** Ceiling strings retain their existing models; side placement makes anchored straight spans. */
public final class HangingStringLightsBlock extends ConnectedLightBlock {
    // The former unused wall=true states identify suspended strings. Old ceiling states stay false.
    public static final int UNSUPPORTED = 16;
    public static final IntegerProperty DISTANCE = IntegerProperty.create("distance", 0, UNSUPPORTED);
    private static final VoxelShape[][][] SUSPENDED_SHAPES = createShapes();
    private static final VoxelShape[] TWO_ANCHORS = RestaurantKitchenBlock.rotations(Shapes.or(
            Block.box(7, 9.75, 7, 9, 13.5, 9), Block.box(7, 9.75, 0, 9, 13.5, 16),
            Block.box(6.75, 12.75, 0, 9.25, 14.5, 1), Block.box(6.75, 12.75, 15, 9.25, 14.5, 16)));
    //? if >=1.21 {
    public static final MapCodec<HangingStringLightsBlock> CODEC = simpleCodec(HangingStringLightsBlock::new);
    //?}

    public HangingStringLightsBlock(Properties properties) {
        super(Kind.STRING, properties);
        registerDefaultState(defaultBlockState().setValue(DISTANCE, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DISTANCE);
    }

    private static VoxelShape[][][] createShapes() {
        VoxelShape[][][] result = new VoxelShape[2][4][];
        for (int anchored = 0; anchored < 2; anchored++) for (int mask = 0; mask < 4; mask++) {
            VoxelShape shape = Block.box(7, 9.75, 7, 9, 13.5, 9);
            if ((mask & 1) != 0) shape = Shapes.or(shape, Block.box(7, 9.75, 0, 9, 13.5, 8));
            if ((mask & 2) != 0 || anchored == 1) shape = Shapes.or(shape, Block.box(7, 9.75, 8, 9, 13.5, 16));
            if (anchored == 1) shape = Shapes.or(shape, Block.box(6.75, 12.75, 15, 9.25, 14.5, 16));
            result[anchored][mask] = RestaurantKitchenBlock.rotations(shape);
        }
        return result;
    }

    private boolean compatible(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(WALL_MOUNTED)
                && other.getValue(FACING).getAxis() == state.getValue(FACING).getAxis();
    }

    private static Direction runFacing(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
    }

    private Direction directAnchor(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        for (int i = 0; i < 2; i++) {
            Direction side = i == 0 ? facing.getOpposite() : facing;
            if (isAnchor(level, pos, side)) return side;
        }
        return null;
    }

    private boolean isAnchor(LevelReader level, BlockPos pos, Direction side) {
        BlockPos support = pos.relative(side);
        if (!level.hasChunkAt(support)) return false;
        BlockState anchor = level.getBlockState(support);
        return !anchor.is(this) && anchor.isFaceSturdy(level, support, side.getOpposite());
    }

    private int supportDistance(BlockState state, LevelReader level, BlockPos pos) {
        if (directAnchor(state, level, pos) != null) return 0;
        int distance = UNSUPPORTED;
        Direction facing = state.getValue(FACING);
        for (int i = 0; i < 2; i++) {
            Direction side = i == 0 ? facing : facing.getOpposite();
            BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState other = level.getBlockState(neighbor);
            if (compatible(state, other)) distance = Math.min(distance, other.getValue(DISTANCE) + 1);
        }
        return distance;
    }

    private BlockState connections(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos front = pos.relative(facing), back = pos.relative(facing.getOpposite());
        // In suspended mode UP/DOWN are the horizontal run ports; RIGHT marks a second wall anchor.
        return state.setValue(LEFT, false).setValue(RIGHT, isAnchor(level, pos, facing))
                .setValue(UP, level.hasChunkAt(front) && compatible(state, level.getBlockState(front)))
                .setValue(DOWN, level.hasChunkAt(back) && compatible(state, level.getBlockState(back)));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face == Direction.DOWN) return super.getStateForPlacement(context);
        if (!face.getAxis().isHorizontal()) return null;
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos.relative(face.getOpposite()));
        Direction facing = face;
        if (clicked.is(this)) {
            if (!clicked.getValue(WALL_MOUNTED)) return null;
            // A side click can begin a parallel row only when that row has its own
            // support path. It never connects perpendicular strings together.
            facing = runFacing(clicked.getValue(FACING).getAxis());
        }
        BlockState state = defaultBlockState().setValue(WALL_MOUNTED, true).setValue(FACING, facing);
        Direction anchor = directAnchor(state, level, pos);
        if (anchor != null) state = state.setValue(FACING, anchor.getOpposite());
        else state = state.setValue(FACING, runFacing(state.getValue(FACING).getAxis()));
        int distance = supportDistance(state, level, pos);
        return distance >= UNSUPPORTED ? null : connections(state.setValue(DISTANCE, distance), level, pos);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(WALL_MOUNTED) ? supportDistance(state, level, pos) < UNSUPPORTED
                : super.canSurvive(state, level, pos);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.getValue(WALL_MOUNTED)) return super.updateShape(state, side, neighbor, level, pos, neighborPos);
        if (side.getAxis() != state.getValue(FACING).getAxis()) return state;
        int distance = supportDistance(state, level, pos);
        if (distance >= UNSUPPORTED) return Blocks.AIR.defaultBlockState();
        Direction anchor = directAnchor(state, level, pos);
        if (anchor != null) state = state.setValue(FACING, anchor.getOpposite());
        else state = state.setValue(FACING, runFacing(state.getValue(FACING).getAxis()));
        return connections(state.setValue(DISTANCE, distance), level, pos);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(WALL_MOUNTED)) return super.getShape(state, level, pos, context);
        if (state.getValue(RIGHT)) return TWO_ANCHORS[RestaurantKitchenBlock.turn(state)];
        int mask = (state.getValue(UP) ? 1 : 0) | (state.getValue(DOWN) ? 2 : 0);
        return SUSPENDED_SHAPES[state.getValue(DISTANCE) == 0 ? 1 : 0][mask][RestaurantKitchenBlock.turn(state)];
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.getValue(WALL_MOUNTED) ? rotate(state, mirror.getRotation(state.getValue(FACING)))
                : super.mirror(state, mirror);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
