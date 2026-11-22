package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PrivacyGlassPartitionBlock extends MultiBlockDecorationBlock {
    private static final Direction[] SIDES = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
    private static final int JOIN_UPDATE = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static {
        VoxelShape[] arms = RestaurantKitchenBlock.rotations(box(7.34, 0, 0, 8.66, 16, 7.42));
        VoxelShape center = box(7.34, 0, 7.42, 8.66, 16, 8.58);
        for (int mask = 0; mask < SHAPES.length; mask++) {
            VoxelShape shape = center;
            for (int side = 0; side < SIDES.length; side++) {
                if ((mask & (1 << side)) != 0) shape = Shapes.or(shape, arms[side]);
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    public PrivacyGlassPartitionBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (Direction side : SIDES) state = state.setValue(connection(side), false);
        registerDefaultState(state);
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        for (Direction side : SIDES) builder.add(connection(side));
    }

    private static BooleanProperty connection(Direction side) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(side);
    }

    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    private boolean isPartner(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(PRIMARY) != state.getValue(PRIMARY)
                && other.getValue(FACING) == state.getValue(FACING);
    }

    private BlockState joins(BlockState state, LevelReader level, BlockPos origin) {
        for (Direction side : SIDES) {
            BlockPos neighbor = origin.relative(side);
            // Keep the persisted edge while its neighbor's chunk is unavailable.
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState lower = level.getBlockState(neighbor);
            boolean connected = lower.is(this) && lower.getValue(PRIMARY)
                    && isPartner(lower, level.getBlockState(neighbor.above()));
            state = state.setValue(connection(side), connected);
        }
        return state;
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return joins(super.getPlacementState(context), context.getLevel(), context.getClickedPos());
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(PRIMARY)) refreshAround(level, pos);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (side == (state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN)
                && !isRemovingLinkedStructure(state, level, pos) && !isPartner(state, neighbor)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean removed = !level.isClientSide && !replacement.is(this) && !isRemovingLinkedStructure(state, level, pos);
        super.onRemove(state, level, pos, replacement, moving);
        if (!removed) return;
        BlockPos origin = structureOrigin(state, pos);
        BlockPos other = state.getValue(PRIMARY) ? origin.above() : origin;
        // Shared cleanup normally removes the identical partner. Also handle an inconsistent saved join.
        if (isPartner(state, level.getBlockState(other))) level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        refreshAround(level, origin);
    }

    private void refreshAround(Level level, BlockPos origin) {
        refresh(level, origin);
        for (Direction side : SIDES) refresh(level, origin.relative(side));
    }

    private void refresh(Level level, BlockPos origin) {
        if (!level.hasChunkAt(origin)) return;
        BlockState lower = level.getBlockState(origin);
        if (!lower.is(this) || !lower.getValue(PRIMARY)) return;
        BlockPos top = origin.above();
        BlockState upper = level.getBlockState(top);
        if (!isPartner(lower, upper)) return;

        BlockState joined = joins(lower, level, origin);
        BlockState joinedUpper = stateForPart(joined, 1);
        if (joined == lower && joinedUpper == upper) return;
        boolean updated = lower == joined || level.setBlock(origin, joined, JOIN_UPDATE);
        if (updated && level.getBlockState(origin) == joined && level.getBlockState(top) == upper
                && (upper == joinedUpper || level.setBlock(top, joinedUpper, JOIN_UPDATE))
                && level.getBlockState(origin) == joined && level.getBlockState(top) == joinedUpper) return;
        if (upper != joinedUpper && level.getBlockState(top) == joinedUpper) level.setBlock(top, upper, JOIN_UPDATE);
        if (lower != joined && level.getBlockState(origin) == joined) level.setBlock(origin, lower, JOIN_UPDATE);
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = super.rotate(state, rotation);
        for (Direction side : SIDES) {
            result = result.setValue(connection(rotation.rotate(side)), state.getValue(connection(side)));
        }
        return result;
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        for (Direction side : SIDES) {
            result = result.setValue(connection(mirror.mirror(side)), state.getValue(connection(side)));
        }
        return result;
    }

    private static int armMask(BlockState state) {
        int connected = 0;
        for (int side = 0; side < SIDES.length; side++) {
            if (state.getValue(connection(SIDES[side]))) connected |= 1 << side;
        }
        // An end module extends through its own cell. An isolated one follows placement.
        if (connected == 0) return state.getValue(FACING).getAxis() == Direction.Axis.Z ? 10 : 5;
        if (Integer.bitCount(connected) == 1) return connected | ((connected << 2 | connected >> 2) & 15);
        return connected;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[armMask(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<PrivacyGlassPartitionBlock> CODEC = simpleCodec(PrivacyGlassPartitionBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
