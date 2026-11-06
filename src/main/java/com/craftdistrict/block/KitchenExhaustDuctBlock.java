package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Passive six-port network: immediate neighbors determine geometry, without support cascades or scans. */
public final class KitchenExhaustDuctBlock extends RotatableDecorationBlock {
    public enum Placement implements StringRepresentable {
        VERTICAL, WALL, CEILING, HOOD;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final EnumProperty<Placement> PLACEMENT = EnumProperty.create("placement", Placement.class);
    public static final BooleanProperty MANUAL_CONNECTIONS = BooleanProperty.create("manual_connections");
    public static final BooleanProperty DOWN = BooleanProperty.create("down"), UP = BooleanProperty.create("up"),
            NORTH = BooleanProperty.create("north"), SOUTH = BooleanProperty.create("south"),
            WEST = BooleanProperty.create("west"), EAST = BooleanProperty.create("east");
    private static final BooleanProperty[] PORTS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};
    private static final Direction[] DIRECTIONS = Direction.values();
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
    private static final VoxelShape[][][] SHAPES = buildShapes(false), EXACT_SHAPES = buildShapes(true);
    //? if >=1.21 {
    public static final MapCodec<KitchenExhaustDuctBlock> CODEC = simpleCodec(KitchenExhaustDuctBlock::new);
    //?}

    public KitchenExhaustDuctBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(PLACEMENT, Placement.VERTICAL).setValue(MANUAL_CONNECTIONS, false);
        for (BooleanProperty port : PORTS) state = state.setValue(port, false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PLACEMENT, DOWN, UP, NORTH, SOUTH, WEST, EAST, MANUAL_CONNECTIONS);
    }

    /** A side hit on an arm targets that arm; a junction/cap hit targets the clicked face. */
    static Direction targetDirection(BlockState state, BlockPos pos, Direction face, Vec3 hit) {
        Vec3 local = hit.subtract(pos.getX(), pos.getY(), pos.getZ());
        int visible = visibleMask(state);
        if (state.getValue(PLACEMENT) == Placement.HOOD && (visible & 1) != 0 && local.y < 9.0 / 16)
            return Direction.DOWN;
        for (Direction direction : DIRECTIONS) {
            if ((visible & 1 << direction.get3DDataValue()) == 0) continue;
            boolean outside = switch (direction) {
                case DOWN -> local.y < 9.0 / 16 - 1e-5;
                case UP -> local.y > 15.0 / 16 + 1e-5;
                case NORTH -> local.z < 5.0 / 16 - 1e-5;
                case SOUTH -> local.z > 11.0 / 16 + 1e-5;
                case WEST -> local.x < 5.0 / 16 - 1e-5;
                case EAST -> local.x > 11.0 / 16 + 1e-5;
            };
            if (outside && ARMS[direction.get3DDataValue()].bounds().inflate(1e-5).contains(local)) return direction;
        }
        return face;
    }

    public InteractionResult toggleConnection(BlockState state, Level level, BlockPos pos, Player player, Direction face, Vec3 hit) {
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos) || level.getBlockState(pos) != state)
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.PASS;
        DuctConnectionData data = DuctConnectionData.get(server);
        Map<BlockPos, Integer> changed = new LinkedHashMap<>();
        if (player.isShiftKeyDown()) {
            changed.put(pos, 0);
            // A shared edge is one choice: reset its reciprocal bit, leaving other neighbor choices intact.
            for (Direction direction : DIRECTIONS) {
                BlockPos neighbor = pos.relative(direction);
                if (level.hasChunkAt(neighbor) && level.getBlockState(neighbor).is(this))
                    changed.put(neighbor, data.mask(neighbor) & ~(1 << direction.getOpposite().get3DDataValue()));
            }
        } else {
            Direction direction = targetDirection(state, pos, face, hit);
            BlockPos neighbor = pos.relative(direction);
            boolean duct = level.hasChunkAt(neighbor) && level.getBlockState(neighbor).is(this);
            int bit = 1 << direction.get3DDataValue(), opposite = 1 << direction.getOpposite().get3DDataValue();
            boolean blocked = (data.mask(pos) & bit) != 0 || duct && (data.mask(neighbor) & opposite) != 0;
            boolean physical = duct || direction == Direction.DOWN && level.hasChunkAt(neighbor)
                    && level.getBlockState(neighbor).getBlock() instanceof KitchenExhaustHoodBlock;
            if (!blocked && !physical && (visibleMask(state) & bit) == 0) return InteractionResult.PASS;
            changed.put(pos, blocked ? data.mask(pos) & ~bit : data.mask(pos) | bit);
            if (duct) changed.put(neighbor, blocked ? data.mask(neighbor) & ~opposite : data.mask(neighbor) | opposite);
        }
        for (var entry : changed.entrySet()) if (entry.getValue() != data.mask(entry.getKey())
                && !level.mayInteract(player, entry.getKey())) return InteractionResult.FAIL;
        return applyOverrides(server, pos, player, data, changed) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    private boolean applyOverrides(ServerLevel level, BlockPos pos, Player player, DuctConnectionData data, Map<BlockPos, Integer> changes) {
        Map<BlockPos, Integer> beforeMasks = new LinkedHashMap<>();
        changes.forEach((part, mask) -> { beforeMasks.put(part, data.mask(part)); data.put(part, mask); });
        Map<BlockPos, BlockState> before = new LinkedHashMap<>(), after = new LinkedHashMap<>();
        before.put(pos, level.getBlockState(pos));
        for (Direction direction : DIRECTIONS) {
            BlockPos neighbor = pos.relative(direction);
            if (level.hasChunkAt(neighbor) && level.getBlockState(neighbor).is(this)) before.put(neighbor, level.getBlockState(neighbor));
        }
        for (var entry : before.entrySet()) {
            BlockState next = connections(entry.getValue(), level, entry.getKey());
            if ((visibleMask(next) & ~visibleMask(entry.getValue())) != 0
                    && !level.isUnobstructed(next, entry.getKey(), CollisionContext.of(player))) {
                beforeMasks.forEach(data::put);
                return false;
            }
            after.put(entry.getKey(), next);
        }
        var written = new ArrayList<BlockPos>();
        for (var entry : after.entrySet()) {
            if (entry.getValue() == before.get(entry.getKey())) continue;
            if (!level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                beforeMasks.forEach(data::put);
                for (int i = written.size() - 1; i >= 0; i--)
                    level.setBlock(written.get(i), before.get(written.get(i)), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                return false;
            }
            written.add(entry.getKey());
        }
        return true;
    }
    public static BooleanProperty port(Direction direction) { return PORTS[direction.get3DDataValue()]; }
    static int connectionsMask(BlockState state) {
        int mask = 0;
        for (Direction direction : DIRECTIONS) if (state.getValue(port(direction))) mask |= 1 << direction.get3DDataValue();
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
    static int visibleMask(BlockState state) {
        int active = connectionsMask(state);
        return state.getValue(MANUAL_CONNECTIONS) ? active : visiblePorts(active, state.getValue(PLACEMENT), state.getValue(FACING));
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
        DuctConnectionData data = level instanceof ServerLevel server ? DuctConnectionData.get(server) : null;
        int blocked = data == null ? 0 : data.mask(pos);
        boolean manual = blocked != 0;
        for (Direction direction : DIRECTIONS) {
            BlockPos neighborPos = pos.relative(direction);
            boolean loaded = level.hasChunkAt(neighborPos);
            boolean duct = loaded && level.getBlockState(neighborPos).is(this);
            boolean otherBlocked = duct && data != null && (data.mask(neighborPos) & 1 << direction.getOpposite().get3DDataValue()) != 0;
            manual |= otherBlocked;
            boolean connected = loaded && (duct || direction == Direction.DOWN && hood)
                    && (blocked & 1 << direction.get3DDataValue()) == 0 && !otherBlocked;
            state = state.setValue(port(direction), connected);
        }
        return state.setValue(MANUAL_CONNECTIONS, manual);
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
        // Clients render the effective ports sent by the server; they do not own override data.
        if (level instanceof Level world && world.isClientSide) return state;
        return connections(state, level, pos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!next.is(this) && level instanceof ServerLevel server) DuctConnectionData.get(server).put(pos, 0);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = super.rotate(state, rotation);
        for (Direction direction : DIRECTIONS) result = result.setValue(port(rotation.rotate(direction)), state.getValue(port(direction)));
        return result;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        for (Direction direction : DIRECTIONS) result = result.setValue(port(mirror.mirror(direction)), state.getValue(port(direction)));
        return result;
    }
    private static VoxelShape[][][] buildShapes(boolean exact) {
        VoxelShape[][][] result = new VoxelShape[4][4][64];
        VoxelShape[] base = new VoxelShape[64];
        VoxelShape[][] walls = new VoxelShape[4][64], adapters = new VoxelShape[4][64];
        VoxelShape[] ceilings = new VoxelShape[64];
        for (int visible = 0; visible < 64; visible++) {
            VoxelShape shape = CORE;
            for (Direction direction : DIRECTIONS) if ((visible & 1 << direction.get3DDataValue()) != 0) shape = Shapes.or(shape, ARMS[direction.get3DDataValue()]);
            base[visible] = shape.optimize();
        }
        for (Placement placement : Placement.values()) for (int turn = 0; turn < 4; turn++) for (int actual = 0; actual < 64; actual++) {
            Direction facing = switch (turn) { case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; case 3 -> Direction.WEST; default -> Direction.NORTH; };
            int visible = exact ? actual : visiblePorts(actual, placement, facing);
            VoxelShape shape = base[visible];
            if (placement == Placement.WALL) {
                if (walls[turn][visible] == null) walls[turn][visible] = Shapes.or(shape, WALL_MOUNTS[turn]).optimize();
                shape = walls[turn][visible];
            } else if (placement == Placement.CEILING) {
                if (ceilings[visible] == null) ceilings[visible] = Shapes.or(shape, CEILING_MOUNT).optimize();
                shape = ceilings[visible];
            } else if (placement == Placement.HOOD && (actual & 1) != 0) {
                int arms = visible & ~1;
                if (adapters[turn][arms] == null) adapters[turn][arms] = Shapes.or(base[arms], HOOD_ADAPTERS[turn]).optimize();
                shape = adapters[turn][arms];
            }
            result[placement.ordinal()][turn][actual] = shape;
        }
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(MANUAL_CONNECTIONS) ? EXACT_SHAPES : SHAPES)
                [state.getValue(PLACEMENT).ordinal()][RestaurantKitchenBlock.turn(state)][connectionsMask(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
