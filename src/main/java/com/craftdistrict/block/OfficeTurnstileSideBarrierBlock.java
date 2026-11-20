package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OfficeTurnstileSideBarrierBlock extends CashRegisterBlock {
    //? if >=1.21 {
    public static final MapCodec<OfficeTurnstileSideBarrierBlock> CODEC = simpleCodec(OfficeTurnstileSideBarrierBlock::new);
    //?}
    private static final Direction[] SIDES = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
    private static final BooleanProperty[] CONNECTIONS = { BlockStateProperties.NORTH, BlockStateProperties.EAST,
            BlockStateProperties.SOUTH, BlockStateProperties.WEST };
    private static final VoxelShape POST = Shapes.or(box(5.75, 0, 5.75, 10.25, .75, 10.25),
            box(6.5, .75, 6.5, 9.5, 15, 9.5), box(6.25, 15, 6.25, 9.75, 15.5, 9.75));
    private static final VoxelShape[] STANDALONE = RestaurantKitchenBlock.rotations(Shapes.or(POST,
            rails(1, 7.25, 6.5, 8.75), rails(9.5, 7.25, 15, 8.75)));
    private static final VoxelShape[] CONNECTED = new VoxelShape[1 << SIDES.length];

    static {
        VoxelShape[] arms = RestaurantKitchenBlock.rotations(rails(7.25, 0, 8.75, 6.5));
        for (int mask = 1; mask < CONNECTED.length; mask++) {
            VoxelShape shape = POST;
            for (int edge = 0; edge < SIDES.length; edge++) {
                if ((mask & (1 << edge)) != 0) shape = Shapes.or(shape, arms[edge]);
            }
            CONNECTED[mask] = shape.optimize();
        }
    }

    public OfficeTurnstileSideBarrierBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (BooleanProperty connection : CONNECTIONS) state = state.setValue(connection, false);
        registerDefaultState(state);
    }

    static VoxelShape rails(double minX, double minZ, double maxX, double maxZ) {
        return Shapes.or(box(minX, 5.25, minZ, maxX, 6.75, maxZ), box(minX, 11.5, minZ, maxX, 13, maxZ));
    }

    private static BooleanProperty connection(Direction side) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(side);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECTIONS);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        var level = context.getLevel();
        for (Direction side : SIDES) {
            BlockPos otherPos = context.getClickedPos().relative(side);
            state = state.setValue(connection(side), connects(level.getBlockState(otherPos), level, otherPos, side));
        }
        return state;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side.getAxis().isHorizontal()) return state.setValue(connection(side), connects(neighbor, level, otherPos, side));
        return super.updateShape(state, side, neighbor, level, pos, otherPos);
    }

    private boolean connects(BlockState neighbor, BlockGetter level, BlockPos pos, Direction side) {
        Direction face = side.getOpposite();
        if (neighbor.is(this)) return true;
        if (neighbor.getBlock() instanceof OfficeTurnstileFixtureBlock fixture) return fixture.barrierSide(neighbor) == face;
        return neighbor.isFaceSturdy(level, pos, face);
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = super.rotate(state, rotation);
        for (Direction side : SIDES) result = result.setValue(connection(rotation.rotate(side)), state.getValue(connection(side)));
        return result;
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        for (Direction side : SIDES) result = result.setValue(connection(mirror.mirror(side)), state.getValue(connection(side)));
        return result;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (int edge = 0; edge < CONNECTIONS.length; edge++) {
            if (state.getValue(CONNECTIONS[edge])) mask |= 1 << edge;
        }
        return mask != 0 ? CONNECTED[mask] : STANDALONE[RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
