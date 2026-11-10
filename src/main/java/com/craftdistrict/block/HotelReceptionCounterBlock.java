package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Square counter modules join only their own family, independently of placement facing. */
public final class HotelReceptionCounterBlock extends RotatableDecorationBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
            SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
    //? if >=1.21 {
    public static final MapCodec<HotelReceptionCounterBlock> CODEC = simpleCodec(HotelReceptionCounterBlock::new);
    //?}
    public HotelReceptionCounterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }
    private static BooleanProperty side(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH; case EAST -> EAST; case SOUTH -> SOUTH; case WEST -> WEST;
            default -> throw new IllegalArgumentException("Horizontal counter edge required");
        };
    }
    private BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(direction);
            state = state.setValue(side(direction), level.hasChunkAt(next) && level.getBlockState(next).is(this));
        }
        return state;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(NORTH, EAST, SOUTH, WEST);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return DecorationSupport.isPlaced(state, level, pos)
                || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos())
                ? layout(state, context.getLevel(), context.getClickedPos()) : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (direction.getAxis().isHorizontal()) return state.setValue(side(direction), neighbor.is(this));
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!level.isClientSide && (!previous.is(this) || previous.getValue(FACING) != state.getValue(FACING))) {
            BlockState joined = layout(state, level, pos);
            if (joined != state) level.setBlock(pos, joined, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = super.rotate(state, rotation);
        for (Direction direction : Direction.Plane.HORIZONTAL)
            result = result.setValue(side(rotation.rotate(direction)), state.getValue(side(direction)));
        return result;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        for (Direction direction : Direction.Plane.HORIZONTAL)
            result = result.setValue(side(mirror.mirror(direction)), state.getValue(side(direction)));
        return result;
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
