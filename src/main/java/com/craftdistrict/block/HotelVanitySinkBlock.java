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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Reciprocal row joins remove interior end panels while keeping each sink and worktop intact. */
public final class HotelVanitySinkBlock extends RotatableDecorationBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(0, 0, 0, 16, 14.5, 16), box(0, 14.5, 0, 5.75, 16, 16),
            box(14, 14.5, 0, 16, 16, 16), box(5.75, 14.5, 0, 14, 16, 3.25),
            box(5.75, 14.5, 11.75, 14, 16, 16), box(9.25, 16, 10.75, 10.75, 19.5, 15),
            box(11.75, 16, 13, 13.25, 17.125, 14.75)).optimize());
    //? if >=1.21 {
    public static final MapCodec<HotelVanitySinkBlock> CODEC = simpleCodec(HotelVanitySinkBlock::new);
    //?}
    public HotelVanitySinkBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LEFT, RIGHT);
    }
    private boolean joins(BlockState state, BlockState other) { return other.is(this) && other.getValue(FACING) == state.getValue(FACING); }
    private BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos left = pos.relative(facing.getCounterClockWise()), right = pos.relative(facing.getClockWise());
        return state.setValue(LEFT, level.hasChunkAt(left) && joins(state, level.getBlockState(left)))
                .setValue(RIGHT, level.hasChunkAt(right) && joins(state, level.getBlockState(right)));
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos floor = pos.below();
        return DecorationSupport.isPlaced(state, level, pos)
                || level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? layout(state, context.getLevel(), context.getClickedPos()) : null;
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!level.isClientSide && (!previous.is(this) || previous.getValue(FACING) != state.getValue(FACING))) {
            BlockState joined = layout(state, level, pos);
            if (joined != state) level.setBlock(pos, joined, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        Direction facing = state.getValue(FACING);
        if (side == facing.getCounterClockWise()) return state.setValue(LEFT, joins(state, neighbor));
        if (side == facing.getClockWise()) return state.setValue(RIGHT, joins(state, neighbor));
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[RestaurantKitchenBlock.turn(state)]; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    @Override public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return DecorationSupport.withTop(getShape(state, level, pos, CollisionContext.empty()));
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
