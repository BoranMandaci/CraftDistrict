package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four immediate neighbors select the outer frame; light controls remain per panel. */
public final class ModularLedPanelBlock extends SurfaceLightBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
            SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
    private static final VoxelShape SHAPE = box(0, 14.75, 0, 16, 16, 16);
    private static final VoxelShape[] WALL_SHAPES = RestaurantKitchenBlock.rotations(box(0, 0, 14.75, 16, 16, 16));
    //? if >=1.21 {
    public static final MapCodec<ModularLedPanelBlock> CODEC = simpleCodec(ModularLedPanelBlock::new);
    //?}

    public ModularLedPanelBlock(Properties properties) {
        super(Fixture.PANEL, properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }

    private static BooleanProperty side(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Panel connection must be horizontal");
        };
    }

    // Reuse the four existing flags in the mounting plane: on walls N/S are up/down and W/E are local left/right.
    private static Direction physicalSide(BlockState state, Direction edge) {
        if (!state.getValue(WALL_MOUNTED)) return edge;
        Direction facing = state.getValue(FACING);
        return switch (edge) {
            case NORTH -> Direction.UP;
            case SOUTH -> Direction.DOWN;
            case EAST -> facing.getClockWise();
            case WEST -> facing.getCounterClockWise();
            default -> throw new IllegalArgumentException("Panel edge must be in the mounting plane");
        };
    }

    private boolean connects(BlockState state, BlockState neighbor) {
        return neighbor.is(this) && neighbor.getValue(WALL_MOUNTED) == state.getValue(WALL_MOUNTED)
                && (!state.getValue(WALL_MOUNTED) || neighbor.getValue(FACING) == state.getValue(FACING));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        for (Direction direction : Direction.Plane.HORIZONTAL)
            state = state.setValue(side(direction), connects(state, context.getLevel()
                    .getBlockState(context.getClickedPos().relative(physicalSide(state, direction)))));
        return state;
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState supported = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        if (!supported.is(this)) return supported;
        for (Direction edge : Direction.Plane.HORIZONTAL)
            if (physicalSide(supported, edge) == direction) return supported.setValue(side(edge), connects(supported, neighbor));
        return supported;
    }

    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos otherPos, boolean moving) {
        super.neighborChanged(state, level, pos, source, otherPos, moving);
        if (level.isClientSide) return;
        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) return;
        BlockState joined = current;
        for (Direction direction : Direction.Plane.HORIZONTAL)
            joined = joined.setValue(side(direction), connects(current, level.getBlockState(pos.relative(physicalSide(current, direction)))));
        if (joined != current) level.setBlock(pos, joined, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = super.rotate(state, rotation);
        if (state.getValue(WALL_MOUNTED)) return result;
        for (Direction direction : Direction.Plane.HORIZONTAL)
            result = result.setValue(side(rotation.rotate(direction)), state.getValue(side(direction)));
        return result;
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (state.getValue(WALL_MOUNTED)) {
            BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
            return mirror == Mirror.NONE ? result : result.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
        }
        BlockState result = super.mirror(state, mirror);
        for (Direction direction : Direction.Plane.HORIZONTAL)
            result = result.setValue(side(mirror.mirror(direction)), state.getValue(side(direction)));
        return result;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(WALL_MOUNTED) ? WALL_SHAPES[RestaurantKitchenBlock.turn(state)] : SHAPE;
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
