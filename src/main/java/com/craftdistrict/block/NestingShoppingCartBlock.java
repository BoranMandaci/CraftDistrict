package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Three individually paid-for carts fit each queued cell; geometry is never narrowed or scaled. */
public final class NestingShoppingCartBlock extends SupermarketShoppingCartBlock {
    public static final BooleanProperty NESTED = BooleanProperty.create("nested");
    public static final BooleanProperty FRONT = BooleanProperty.create("front");
    public static final BooleanProperty BACK = BooleanProperty.create("back");
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    public static final IntegerProperty CARTS = IntegerProperty.create("carts", 0, 3);
    private static final int PART_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final double PITCH = 16.0 / 3.0;
    private static final VoxelShape[][][][] SHAPES = shapes();
    private static final VoxelShape[][] BRIDGE_SHAPES = {
            RestaurantKitchenBlock.rotations(box(1.5, 0, 16 - 11.2 - PITCH, 14.5, 14.5, 16)),
            RestaurantKitchenBlock.rotations(box(1.5, 0, 16 - 11.2 - 2 * PITCH, 14.5, 14.5, 16))};
    private static final VoxelShape[][] LOOSE_SHAPES = {
            RestaurantKitchenBlock.rotations(box(1.5, 0, -11.2, 14.5, 14.5, 12.2)),
            RestaurantKitchenBlock.rotations(box(1.5, 0, -16, 14.5, 14.5, 12.2)),
            RestaurantKitchenBlock.rotations(box(1.5, 0, -16, 14.5, 14.5, 12.2))};
    private record Edit(Level level, BlockPos origin) { }
    private static final ThreadLocal<Edit> EDITING = new ThreadLocal<>();
    //? if >=1.21 {
    public static final MapCodec<NestingShoppingCartBlock> CODEC = simpleCodec(NestingShoppingCartBlock::new);
    //?}

    public NestingShoppingCartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NESTED, false).setValue(FRONT, false)
                .setValue(BACK, false).setValue(TIP, false).setValue(CARTS, 0));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(NESTED, FRONT, BACK, TIP, CARTS);
    }

    // Zero is the saved-state default for the original one-cart-per-cell arrangement.
    private static int cartCount(BlockState state) { return Math.max(1, state.getValue(CARTS)); }

    @Override protected int partCount(BlockState state) {
        return state.getValue(NESTED) ? 1 : cartCount(state) == 1 || state.getValue(FRONT) ? 2 : 3;
    }
    @Override protected int partIndex(BlockState state) {
        return state.getValue(PRIMARY) ? 0 : state.getValue(TIP) ? 2 : 1;
    }
    @Override protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PRIMARY, index == 0).setValue(TIP, index == 2);
    }

    public boolean rootAt(LevelReader level, BlockPos pos, Direction facing) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState other = level.getBlockState(pos);
        return other.is(this) && other.getValue(PRIMARY) && other.getValue(FACING) == facing;
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        Direction facing = state.getValue(FACING);
        BlockPos pos = context.getClickedPos();
        boolean front = rootAt(context.getLevel(), pos.relative(facing), facing);
        return state.setValue(CARTS, 1).setValue(NESTED, front).setValue(FRONT, front)
                .setValue(BACK, rootAt(context.getLevel(), pos.relative(facing.getOpposite()), facing));
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        queueNearby(level, pos, state);
    }

    /** Use the normal cart item to insert exactly one cart into an existing row. */
    public boolean insertCart(Level level, BlockPos pos, Player player, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) || !state.getValue(PRIMARY) || cartCount(state) >= 3
                || !isStructureComplete(state, level, pos)) return false;
        BlockState updated = state.setValue(CARTS, cartCount(state) + 1);
        for (int index = 0; index < partCount(updated); index++) {
            BlockPos part = pos.relative(state.getValue(FACING), index);
            if (!validCell(level, part) || !level.mayInteract(player, part)
                    || !player.mayUseItemAt(part, Direction.UP, stack)) return false;
        }
        if (!canWrite(level, pos, state, updated, CollisionContext.of(player))) return false;
        return level.isClientSide || writeParts(level, pos, state, updated);
    }

    private static boolean validCell(LevelReader level, BlockPos pos) {
        return level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos)
                && level.getWorldBorder().isWithinBounds(pos);
    }

    private boolean ownedPart(BlockState other, BlockState root, int index) {
        return other.is(this) && other.getValue(FACING) == root.getValue(FACING)
                && partIndex(other) == index;
    }

    private boolean canWrite(Level level, BlockPos pos, BlockState old, BlockState updated, CollisionContext collision) {
        for (int index = 0; index < partCount(updated); index++) {
            BlockPos part = pos.relative(updated.getValue(FACING), index);
            if (!validCell(level, part)) return false;
            BlockState existing = level.getBlockState(part);
            boolean owned = index < partCount(old) && ownedPart(existing, old, index);
            if (!owned && !existing.isAir()) return false;
            if ((!owned || cartCount(old) != cartCount(updated))
                    && !level.isUnobstructed(stateForPart(updated, index), part, collision)) return false;
        }
        return true;
    }

    /** Update the small fixture atomically; never remove a neighbor belonging to another cart. */
    private boolean writeParts(Level level, BlockPos pos, BlockState old, BlockState updated) {
        int oldCount = partCount(old), count = partCount(updated), length = Math.max(oldCount, count);
        BlockState[] before = new BlockState[length];
        BlockState[] after = new BlockState[length];
        BlockPos[] positions = new BlockPos[length];
        for (int index = 0; index < length; index++) {
            positions[index] = pos.relative(updated.getValue(FACING), index);
            if (!validCell(level, positions[index])) return false;
            before[index] = level.getBlockState(positions[index]);
            after[index] = index < count ? stateForPart(updated, index)
                    : ownedPart(before[index], old, index) ? Blocks.AIR.defaultBlockState() : before[index];
        }
        Edit previous = EDITING.get();
        EDITING.set(new Edit(level, pos.immutable()));
        boolean success = false;
        try {
            for (int index = 0; index < length; index++) {
                if (level.getBlockState(positions[index]) != before[index]) return false;
                if (before[index] != after[index] && !level.setBlock(positions[index], after[index], PART_FLAGS)) return false;
            }
            for (int index = 0; index < length; index++)
                if (level.getBlockState(positions[index]) != after[index]) return false;
            success = true;
            return true;
        } finally {
            if (!success) {
                for (int index = length - 1; index >= 0; index--) {
                    if (before[index] != after[index] && level.getBlockState(positions[index]) == after[index])
                        level.setBlock(positions[index], before[index], PART_FLAGS);
                }
            }
            if (previous == null) EDITING.remove(); else EDITING.set(previous);
            if (success) {
                // Shape changes notify only this fixture and its immediate neighbors.
                for (BlockPos part : positions) level.updateNeighborsAt(part, this);
                queueNearby(level, pos, updated);
            }
        }
    }

    private void queueNearby(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        BlockPos origin = structureOrigin(state, pos);
        Direction facing = state.getValue(FACING);
        for (int step = -3; step <= 3; step++) {
            BlockPos candidate = origin.relative(facing, step);
            if (rootAt(level, candidate, facing)) level.scheduleTick(candidate, this, 1);
        }
    }

    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                           Block source, BlockPos sourcePos, boolean moving) {
        super.neighborChanged(state, level, pos, source, sourcePos, moving);
        queueNearby(level, pos, state);
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!previous.is(this)) queueNearby(level, pos, state);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        Edit editing = EDITING.get();
        if (editing != null && editing.level == level && editing.origin.equals(structureOrigin(state, pos))) return;
        super.onRemove(state, level, pos, replacement, moving);
        if (!replacement.is(this)) queueNearby(level, pos, state);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PRIMARY)) return;
        Direction facing = state.getValue(FACING);
        BlockPos front = pos.relative(facing), rear = pos.relative(facing.getOpposite());
        if (!level.hasChunkAt(front) || !level.hasChunkAt(rear)) return;
        boolean connected = rootAt(level, front, facing);
        boolean bridge = !connected && cartCount(state) > 1
                && rootAt(level, front.relative(facing), facing);
        boolean back = rootAt(level, rear, facing);
        BlockState updated = state.setValue(NESTED, connected).setValue(FRONT, connected || bridge).setValue(BACK, back);
        if (!connected && !canWrite(level, pos, state, updated, CollisionContext.empty())) {
            // Do not force a full basket through an obstruction when a row splits.
            updated = updated.setValue(NESTED, true);
        }
        if (updated != state || !isStructureComplete(state, level, pos)) writeParts(level, pos, state, updated);
    }

    private static VoxelShape[][][][] shapes() {
        VoxelShape[][][][] result = new VoxelShape[2][3][3][];
        for (int nested = 0; nested < 2; nested++) for (int carts = 1; carts <= 3; carts++) {
            double front = -11.2 - (carts - 1) * PITCH;
            for (int part = 0; part < 3; part++) {
                VoxelShape north;
                if (nested == 1) north = part == 0
                        ? box(1.5, 0, 0, 14.5, 14.5, 12.2 - (3 - carts) * PITCH)
                        : net.minecraft.world.phys.shapes.Shapes.empty();
                else {
                    double min = Math.max(0, front + part * 16);
                    double max = Math.min(16, 12.2 + part * 16);
                    north = min < max ? box(1.5, 0, min, 14.5, 14.5, max)
                            : net.minecraft.world.phys.shapes.Shapes.empty();
                }
                result[nested][carts - 1][part] = RestaurantKitchenBlock.rotations(north);
            }
        }
        return result;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(NESTED) && (!state.getValue(FRONT) || state.getValue(CARTS) == 0))
            return LOOSE_SHAPES[cartCount(state) - 1][RestaurantKitchenBlock.turn(state)];
        if (!state.getValue(NESTED) && cartCount(state) == 1 && !state.getValue(TIP))
            return super.getShape(state, level, pos, context);
        if (!state.getValue(NESTED) && state.getValue(FRONT) && cartCount(state) > 1 && partIndex(state) == 1)
            return BRIDGE_SHAPES[cartCount(state) - 2][RestaurantKitchenBlock.turn(state)];
        return SHAPES[state.getValue(NESTED) ? 1 : 0][cartCount(state) - 1]
                [partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
