package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import com.craftdistrict.entity.Seating;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** New independent seats connect locally; saved three-part benches keep their original lifecycle. */
public final class PoliceWaitingBenchBlock extends PoliceEntranceBlock implements SittableBlock {
    public static final BooleanProperty END = BooleanProperty.create("end");
    public static final BooleanProperty MODULAR = BooleanProperty.create("modular");
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty SUPPORT = BooleanProperty.create("support");
    public static final BooleanProperty MANUAL_SUPPORT = ModularDiningTableBlock.MANUAL_SUPPORT;
    private static final VoxelShape[][] MODULAR_SHAPES = buildShapes();
    //? if >=1.21 {
    public static final MapCodec<PoliceWaitingBenchBlock> CODEC = simpleCodec(PoliceWaitingBenchBlock::new);
    //?}

    public PoliceWaitingBenchBlock(Properties properties) {
        super(Variant.WAITING_BENCH, properties);
        // Missing properties in old saves must resolve to the original three-part bench.
        registerDefaultState(defaultBlockState().setValue(MODULAR, false).setValue(END, false)
                .setValue(LEFT, false).setValue(RIGHT, false).setValue(SUPPORT, false)
                .setValue(MANUAL_SUPPORT, false));
    }

    @Override
    protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(END, MODULAR, LEFT, RIGHT, SUPPORT, MANUAL_SUPPORT);
    }

    @Override
    protected int partCount() {
        return 3;
    }

    @Override protected int partCount(BlockState state) {
        return state.getValue(MODULAR) ? 1 : 3;
    }

    @Override
    protected int partIndex(BlockState state) {
        return state.getValue(MODULAR) || state.getValue(PRIMARY) ? 0 : state.getValue(END) ? 2 : 1;
    }

    @Override
    protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PRIMARY, state.getValue(MODULAR) || index == 0)
                .setValue(END, !state.getValue(MODULAR) && index == 2);
    }

    private boolean matches(BlockState state, LevelReader level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState neighbor = level.getBlockState(pos);
        return neighbor.is(this) && neighbor.getValue(MODULAR)
                && neighbor.getValue(FACING) == state.getValue(FACING);
    }

    private BlockState connections(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        boolean left = matches(state, level, pos.relative(facing.getCounterClockWise()));
        boolean right = matches(state, level, pos.relative(facing.getClockWise()));
        // End supports plus one intermediate pedestal every three cells: constant-time updates.
        int along = facing.getAxis() == Direction.Axis.Z ? pos.getX() : pos.getZ();
        return state.setValue(LEFT, left).setValue(RIGHT, right)
                .setValue(SUPPORT, state.getValue(MANUAL_SUPPORT) ? state.getValue(SUPPORT)
                        : !left || !right || Math.floorMod(along, 3) == 0);
    }

    public InteractionResult toggleSupport(BlockState state, Level level, BlockPos pos, Player player) {
        // Original three-part benches retain their linked state identity and removal behavior.
        if (!state.getValue(MODULAR) || player == null || !player.mayBuild()
                || !level.mayInteract(player, pos) || level.getBlockState(pos) != state)
            return InteractionResult.PASS;
        BlockState automatic = connections(state.setValue(MANUAL_SUPPORT, false), level, pos);
        BlockState next = player.isShiftKeyDown() ? automatic
                : automatic.setValue(MANUAL_SUPPORT, true).setValue(SUPPORT, !state.getValue(SUPPORT));
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (next == state) return InteractionResult.CONSUME;
        if (next.getValue(SUPPORT) && !state.getValue(SUPPORT)) {
            // Test only the added leg, so an occupied cushion does not block a safe adjustment.
            VoxelShape added = Shapes.join(next.getCollisionShape(level, pos),
                    state.getCollisionShape(level, pos), BooleanOp.ONLY_FIRST);
            if (!level.isUnobstructed(null, added.move(pos.getX(), pos.getY(), pos.getZ())))
                return InteractionResult.FAIL;
        }
        return level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : connections(state.setValue(MODULAR, true),
                context.getLevel(), context.getClickedPos());
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState supported = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return !supported.isAir() && supported.getValue(MODULAR) && direction.getAxis().isHorizontal()
                && direction.getAxis() != state.getValue(FACING).getAxis()
                ? connections(supported, level, pos) : supported;
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE || !state.getValue(MODULAR) ? result
                : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    private static VoxelShape[][] buildShapes() {
        VoxelShape[][] result = new VoxelShape[8][];
        for (int mask = 0; mask < 8; mask++) {
            boolean left = (mask & 1) != 0, right = (mask & 2) != 0;
            VoxelShape shape = Shapes.or(box(1.5, 6.5, 4, 14.5, 8, 12),
                    box(1.5, 9, 11.5, 14.5, 15.75, 15.5),
                    box(3.75, 5.75, 10.5, 12.25, 6.25, 12.5),
                    box(3.75, 6.25, 12, 12.25, 9.25, 13.75),
                    box(left ? 0 : 1, 5.5, 7, right ? 16 : 15, 6.5, 10.5));
            // Connected halves meet at the cell edge to form one padded armrest.
            shape = Shapes.or(shape, box(left ? 0 : .375, 8, 4.625, left ? .625 : 1.625, 10.625, 10.625),
                    box(right ? 15.375 : 14.375, 8, 4.625, right ? 16 : 15.625, 10.625, 10.625));
            if ((mask & 4) != 0) shape = Shapes.or(shape, box(7, 0, 3, 9, 6.5, 13));
            result[mask] = RestaurantKitchenBlock.rotations(shape.optimize());
        }
        return result;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(MODULAR)) return super.getShape(state, level, pos, context);
        int mask = (state.getValue(LEFT) ? 1 : 0) | (state.getValue(RIGHT) ? 2 : 0)
                | (state.getValue(SUPPORT) ? 4 : 0);
        return MODULAR_SHAPES[mask][RestaurantKitchenBlock.turn(state)];
    }

    @Override public double seatHeight() { return 8.0 / 16; }
    @Override public double forwardOffset() { return 1.0 / 16; }

    // Both legacy and modular seats own their own passenger anchor.
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Seating.sit(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? Seating.sit(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) || replacement.getValue(FACING) != state.getValue(FACING)) Seating.removeAt(level, pos);
        // Only legacy states remove linked parts; modular seats are independent.
        super.onRemove(state, level, pos, replacement, moving);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
