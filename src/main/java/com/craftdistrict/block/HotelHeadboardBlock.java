package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall panels share their padded core; the lower overhang and trims appear only on exposed edges. */
public final class HotelHeadboardBlock extends RotatableDecorationBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP = BooleanProperty.create("up"), DOWN = BooleanProperty.create("down");
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(0, -3.5, 13.5, 16, 16, 16)),
            RestaurantKitchenBlock.rotations(box(0, 0, 13.5, 16, 16, 16))};
    //? if >=1.21 {
    public static final MapCodec<HotelHeadboardBlock> CODEC = simpleCodec(HotelHeadboardBlock::new);
    //?}
    public HotelHeadboardBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false).setValue(UP, false).setValue(DOWN, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LEFT, RIGHT, UP, DOWN);
    }
    private boolean matches(BlockState state, BlockState other) { return other.is(this) && other.getValue(FACING) == state.getValue(FACING); }
    private boolean connects(BlockState state, LevelReader level, BlockPos pos, Direction side) {
        BlockPos neighbor = pos.relative(side);
        return level.hasChunkAt(neighbor) && matches(state, level.getBlockState(neighbor));
    }
    private BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        return state.setValue(LEFT, connects(state, level, pos, state.getValue(FACING).getCounterClockWise()))
                .setValue(RIGHT, connects(state, level, pos, state.getValue(FACING).getClockWise()))
                .setValue(UP, connects(state, level, pos, Direction.UP))
                .setValue(DOWN, connects(state, level, pos, Direction.DOWN));
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = state.getValue(FACING); BlockPos wall = pos.relative(face.getOpposite());
        return level.hasChunkAt(wall) && level.getBlockState(wall).isFaceSturdy(level, wall, face);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!context.getClickedFace().getAxis().isHorizontal()) return null;
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        if (!state.canSurvive(context.getLevel(), context.getClickedPos())) return null;
        return layout(state, context.getLevel(), context.getClickedPos());
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!level.isClientSide && (!previous.is(this) || previous.getValue(FACING) != state.getValue(FACING))) {
            BlockState joined = layout(state, level, pos);
            if (joined != state) level.setBlock(pos, joined, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side == state.getValue(FACING).getCounterClockWise()) return state.setValue(LEFT, matches(state, neighbor));
        if (side == state.getValue(FACING).getClockWise()) return state.setValue(RIGHT, matches(state, neighbor));
        if (side == Direction.UP) return state.setValue(UP, matches(state, neighbor));
        if (side == Direction.DOWN) return state.setValue(DOWN, matches(state, neighbor));
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(DOWN) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
