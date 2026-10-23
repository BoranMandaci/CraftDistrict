package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Passive six-port network: immediate neighbors determine geometry, without support cascades or scans. */
public final class KitchenExhaustDuctBlock extends RotatableDecorationBlock {
    public enum Placement implements StringRepresentable {
        VERTICAL, WALL, CEILING, HOOD;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final EnumProperty<Placement> PLACEMENT = EnumProperty.create("placement", Placement.class);
    public static final BooleanProperty DOWN = BooleanProperty.create("down"), UP = BooleanProperty.create("up"),
            NORTH = BooleanProperty.create("north"), SOUTH = BooleanProperty.create("south"),
            WEST = BooleanProperty.create("west"), EAST = BooleanProperty.create("east");
    private static final BooleanProperty[] PORTS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};
    private static final VoxelShape CORE = Block.box(5, 9, 5, 11, 15, 11);
    private static final VoxelShape[] ARMS = {
            Block.box(5, 0, 6, 11, 9, 10), Block.box(5, 15, 6, 11, 16, 10),
            Block.box(5, 10, 0, 11, 14, 5), Block.box(5, 10, 11, 11, 14, 16),
            Block.box(0, 10, 5, 5, 14, 11), Block.box(11, 10, 5, 16, 14, 11)};
    private static final VoxelShape[] WALL_MOUNTS = RestaurantKitchenBlock.rotations(Shapes.or(
            Block.box(4.5, 14, 10, 5, 15.5, 16), Block.box(11, 14, 10, 11.5, 15.5, 16)));
    private static final VoxelShape CEILING_MOUNT = Shapes.or(Block.box(4.5, 14, 7, 5, 16, 9), Block.box(11, 14, 7, 11.5, 16, 9));
    private static final VoxelShape[] HOOD_ADAPTERS = {
            Shapes.or(Block.box(5, 0, 12, 11, 3, 16), Block.box(5, 3, 6, 11, 7, 16), Block.box(5, 7, 6, 11, 9, 10)),
            Shapes.or(Block.box(0, 0, 5, 4, 3, 11), Block.box(0, 3, 5, 11, 7, 11), Block.box(5, 7, 6, 11, 9, 10)),
            Shapes.or(Block.box(5, 0, 0, 11, 3, 4), Block.box(5, 3, 0, 11, 7, 10), Block.box(5, 7, 6, 11, 9, 10)),
            Shapes.or(Block.box(12, 0, 5, 16, 3, 11), Block.box(5, 3, 5, 16, 7, 11), Block.box(5, 7, 6, 11, 9, 10))};
    private static final VoxelShape[][][] SHAPES = buildShapes();
    //? if >=1.21 {
    public static final MapCodec<KitchenExhaustDuctBlock> CODEC = simpleCodec(KitchenExhaustDuctBlock::new);
    //?}

    public KitchenExhaustDuctBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(PLACEMENT, Placement.VERTICAL);
        for (BooleanProperty port : PORTS) state = state.setValue(port, false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PLACEMENT, DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }
    public static BooleanProperty port(Direction direction) { return PORTS[direction.get3DDataValue()]; }
    static int connectionsMask(BlockState state) {
        int mask = 0;
        for (Direction direction : Direction.values()) if (state.getValue(port(direction))) mask |= 1 << direction.get3DDataValue();
        return mask;
    }
    static int visiblePorts(int actual, Placement placement, Direction facing) {
        if (Integer.bitCount(actual) >= 2) return actual;
        if (actual != 0) {
            Direction connected = Direction.from3DDataValue(Integer.numberOfTrailingZeros(actual));
            return actual | 1 << connected.getOpposite().get3DDataValue();
        }
        Direction axis = placement == Placement.VERTICAL || placement == Placement.HOOD ? Direction.UP
                : placement == Placement.WALL ? facing.getClockWise() : facing;
        return (1 << axis.get3DDataValue()) | (1 << axis.getOpposite().get3DDataValue());
    }
    BlockState placementHint(Direction clicked, Direction player, BlockState below) {
        if (below.getBlock() instanceof KitchenExhaustHoodBlock)
            return defaultBlockState().setValue(PLACEMENT, Placement.HOOD).setValue(FACING, below.getValue(FACING));
        Placement placement = clicked == Direction.UP ? Placement.VERTICAL : clicked == Direction.DOWN ? Placement.CEILING : Placement.WALL;
        Direction facing = clicked.getAxis().isHorizontal() ? clicked : below.is(this) && clicked == Direction.UP ? below.getValue(FACING) : player.getOpposite();
        return defaultBlockState().setValue(PLACEMENT, placement).setValue(FACING, facing);
    }
    BlockState connections(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        boolean hood = below.getBlock() instanceof KitchenExhaustHoodBlock;
        if (hood) state = state.setValue(PLACEMENT, Placement.HOOD).setValue(FACING, below.getValue(FACING));
        else if (state.getValue(PLACEMENT) == Placement.HOOD) state = state.setValue(PLACEMENT, Placement.VERTICAL);
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            boolean connected = level.hasChunkAt(neighborPos) && (level.getBlockState(neighborPos).is(this) || direction == Direction.DOWN && hood);
            state = state.setValue(port(direction), connected);
        }
        return state;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return placementState(context.getClickedFace(), context.getHorizontalDirection(), context.getLevel(), context.getClickedPos());
    }
    BlockState placementState(Direction clicked, Direction player, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        BlockState state = placementHint(clicked, player, below);
        if (!(below.getBlock() instanceof KitchenExhaustHoodBlock)) {
            BlockPos above = pos.above();
            BlockState clickedBlock = level.getBlockState(pos.relative(clicked.getOpposite()));
            if (level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN)) {
                state = state.setValue(PLACEMENT, Placement.CEILING).setValue(FACING, player.getOpposite());
            } else if (clickedBlock.is(this)) {
                // Extending a pipe does not create a bracket aimed into empty air.
                Direction facing = clickedBlock.getValue(FACING);
                BlockPos wall = pos.relative(facing.getOpposite());
                state = state.setValue(FACING, facing).setValue(PLACEMENT,
                        level.getBlockState(wall).isFaceSturdy(level, wall, facing) ? Placement.WALL : Placement.VERTICAL);
            }
        }
        return connections(state, level, pos);
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return connections(state, level, pos);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = super.rotate(state, rotation);
        for (Direction direction : Direction.values()) result = result.setValue(port(rotation.rotate(direction)), state.getValue(port(direction)));
        return result;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        for (Direction direction : Direction.values()) result = result.setValue(port(mirror.mirror(direction)), state.getValue(port(direction)));
        return result;
    }
    private static VoxelShape[][][] buildShapes() {
        VoxelShape[][][] result = new VoxelShape[4][4][64];
        VoxelShape[] base = new VoxelShape[64];
        for (int visible = 0; visible < 64; visible++) {
            VoxelShape shape = CORE;
            for (Direction direction : Direction.values()) if ((visible & 1 << direction.get3DDataValue()) != 0) shape = Shapes.or(shape, ARMS[direction.get3DDataValue()]);
            base[visible] = shape.optimize();
        }
        for (Placement placement : Placement.values()) for (int turn = 0; turn < 4; turn++) for (int actual = 0; actual < 64; actual++) {
            Direction facing = switch (turn) { case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; case 3 -> Direction.WEST; default -> Direction.NORTH; };
            int visible = visiblePorts(actual, placement, facing);
            VoxelShape shape = base[visible];
            if (placement == Placement.WALL) shape = Shapes.or(shape, WALL_MOUNTS[turn]).optimize();
            else if (placement == Placement.CEILING) shape = Shapes.or(shape, CEILING_MOUNT).optimize();
            else if (placement == Placement.HOOD && (actual & 1) != 0) shape = Shapes.or(base[visible & ~1], HOOD_ADAPTERS[turn]).optimize();
            result[placement.ordinal()][turn][actual] = shape;
        }
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PLACEMENT).ordinal()][RestaurantKitchenBlock.turn(state)][connectionsMask(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
