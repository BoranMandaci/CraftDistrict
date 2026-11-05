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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-high screens use four local half-panels to join straight runs and corners. */
public final class MedicalPrivacyCurtainBlock extends MultiBlockDecorationBlock {
    // Retain the original saved connection properties alongside the new corner profile.
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 15);
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    //? if >=1.21 {
    public static final MapCodec<MedicalPrivacyCurtainBlock> CODEC = simpleCodec(MedicalPrivacyCurtainBlock::new);
    //?}
    private static final VoxelShape[][] SHAPES = buildShapes();

    public MedicalPrivacyCurtainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false).setValue(CONNECTIONS, 0));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(LEFT, RIGHT, CONNECTIONS);
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    private boolean matches(BlockState state, BlockState neighbor, boolean primary) {
        return neighbor.is(this) && neighbor.getValue(PRIMARY) == primary && neighbor.getValue(FACING) == state.getValue(FACING);
    }
    public static int bit(Direction direction) {
        return switch (direction) { case NORTH -> 1; case EAST -> 2; case SOUTH -> 4; case WEST -> 8; default -> 0; };
    }
    public static int panelMask(BlockState state) {
        int mask = state.getValue(CONNECTIONS);
        return mask == 0 ? (state.getValue(FACING).getAxis() == Direction.Axis.Z ? 10 : 5) : mask;
    }
    private static VoxelShape[][] buildShapes() {
        VoxelShape[][] result = new VoxelShape[2][16];
        for (int half = 0; half < 2; half++) for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = box(7.125, 0, 7.125, 8.875, 16, 8.875);
            if (half == 0) shape = Shapes.or(shape, box(3.5, 0, 3.5, 12.5, .75, 12.5));
            for (Direction side : SIDES) if ((mask & bit(side)) != 0) {
                shape = Shapes.or(shape, switch (side) {
                    case NORTH -> box(7.125, half == 0 ? 3 : 0, 0, 8.875, 16, 7.125);
                    case EAST -> box(8.875, half == 0 ? 3 : 0, 7.125, 16, 16, 8.875);
                    case SOUTH -> box(7.125, half == 0 ? 3 : 0, 8.875, 8.875, 16, 16);
                    default -> box(0, half == 0 ? 3 : 0, 7.125, 7.125, 16, 8.875);
                });
            }
            result[half][mask] = shape.optimize();
        }
        return result;
    }
    private BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos origin = structureOrigin(state, pos);
        int mask = 0;
        // Both halves read the lower row; no dependency on a neighbor's connection state.
        for (Direction side : SIDES) {
            BlockPos next = origin.relative(side);
            if (!level.hasChunkAt(next)) continue;
            BlockState neighbor = level.getBlockState(next);
            if (neighbor.is(this) && neighbor.getValue(PRIMARY)) mask |= bit(side);
        }
        boolean left = (mask & bit(facing.getCounterClockWise())) != 0;
        boolean right = (mask & bit(facing.getClockWise())) != 0;
        if (mask == 0) mask = facing.getAxis() == Direction.Axis.Z ? 10 : 5;
        else if (Integer.bitCount(mask) == 1) mask |= ((mask << 2) | (mask >> 2)) & 15;
        return state.setValue(LEFT, left).setValue(RIGHT, right).setValue(CONNECTIONS, mask);
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return layout(super.getPlacementState(context), context.getLevel(), context.getClickedPos());
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockState(pos).is(this) && level.getBlockState(pos.above()).is(this)) {
            for (BlockPos part : structureParts(state, pos))
                level.getBlockState(part).updateNeighbourShapes(level, part, Block.UPDATE_ALL);
        }
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && !matches(state, neighbor, !state.getValue(PRIMARY))) return Blocks.AIR.defaultBlockState();
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return layout(state, level, pos);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PRIMARY) ? 0 : 1][panelMask(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        int mask = 0;
        for (Direction side : SIDES) if ((state.getValue(CONNECTIONS) & bit(side)) != 0) mask |= bit(rotation.rotate(side));
        return super.rotate(state, rotation).setValue(CONNECTIONS, mask);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        int mask = 0;
        for (Direction side : SIDES) if ((state.getValue(CONNECTIONS) & bit(side)) != 0) mask |= bit(mirror.mirror(side));
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(CONNECTIONS, mask)
                .setValue(LEFT, mirror == Mirror.NONE ? state.getValue(LEFT) : state.getValue(RIGHT))
                .setValue(RIGHT, mirror == Mirror.NONE ? state.getValue(RIGHT) : state.getValue(LEFT));
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
