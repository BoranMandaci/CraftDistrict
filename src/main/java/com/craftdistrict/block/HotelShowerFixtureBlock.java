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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-high wall plumbing can share its lower cell with the existing decorative shower tray. */
public final class HotelShowerFixtureBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty TALL = BooleanProperty.create("tall"), BASE = BooleanProperty.create("base"),
            PLAIN_BASE = BooleanProperty.create("plain_base");
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(5.25, .5, 13.5, 10.75, 15.75, 16), box(4.5, 14.125, 7, 11.5, 15.75, 15)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(7.75, 2, 15, 8.25, 16, 15.5), box(5.25, 2, 13.5, 10.75, 4.625, 16)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(7, 0, 15, 9, 15.75, 16), box(4.5, 14.125, 7, 11.5, 15.75, 15)).optimize())};
    private static final VoxelShape[][] WITH_BASE = new VoxelShape[16][4];
    static {
        for (int mask = 0; mask < 16; mask++) for (int turn = 0; turn < 4; turn++)
            WITH_BASE[mask][turn] = Shapes.or(SHAPES[1][turn], HotelShowerFloorBlock.shapeFor(mask)).optimize();
    }
    //? if >=1.21 {
    public static final MapCodec<HotelShowerFixtureBlock> CODEC = simpleCodec(HotelShowerFixtureBlock::new);
    //?}
    public HotelShowerFixtureBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(TALL, false).setValue(BASE, false).setValue(PLAIN_BASE, false)
                .setValue(HotelShowerFloorBlock.NORTH, false).setValue(HotelShowerFloorBlock.EAST, false)
                .setValue(HotelShowerFloorBlock.SOUTH, false).setValue(HotelShowerFloorBlock.WEST, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(TALL, BASE, PLAIN_BASE,
                HotelShowerFloorBlock.NORTH, HotelShowerFloorBlock.EAST, HotelShowerFloorBlock.SOUTH, HotelShowerFloorBlock.WEST);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(TALL) ? 2 : 1; }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        return context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null;
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(TALL, true);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(TALL) && !state.getValue(PRIMARY)) return false;
        BlockPos wall = pos.relative(state.getValue(FACING).getOpposite());
        return DecorationSupport.isPlaced(state, level, pos)
                || level.hasChunkAt(wall) && level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING));
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (state.getValue(TALL)) {
            Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
            if (side == partner && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        }
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = super.rotate(state, rotation);
        for (Direction side : Direction.Plane.HORIZONTAL) result = result.setValue(HotelShowerFloorBlock.property(rotation.rotate(side)), state.getValue(HotelShowerFloorBlock.property(side)));
        return result;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        for (Direction side : Direction.Plane.HORIZONTAL) result = result.setValue(HotelShowerFloorBlock.property(mirror.mirror(side)), state.getValue(HotelShowerFloorBlock.property(side)));
        return result;
    }
    /** Keep both halves identical while changing the attached tray's edge flags. */
    public void refreshFloor(Level level, BlockPos pos) {
        if (level.isClientSide || !level.hasChunkAt(pos.above())) return;
        BlockState lower = level.getBlockState(pos), upper = level.getBlockState(pos.above());
        if (!lower.is(this) || !HotelShowerFloorBlock.compatible(lower) || upper != stateForPart(lower, 1)) return;
        BlockState next = HotelShowerFloorBlock.connections(lower, level, pos), nextUpper = stateForPart(next, 1);
        if (next == lower) return;
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        if (!level.setBlock(pos, next, flags)) return;
        if (level.getBlockState(pos) != next || level.getBlockState(pos.above()) != upper
                || !level.setBlock(pos.above(), nextUpper, flags)
                || level.getBlockState(pos) != next || level.getBlockState(pos.above()) != nextUpper) {
            if (level.getBlockState(pos.above()) == nextUpper) level.setBlock(pos.above(), upper, flags);
            if (level.getBlockState(pos) == next) level.setBlock(pos, lower, flags);
        }
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving) {
        super.neighborChanged(state, level, pos, source, sourcePos, moving);
        if (HotelShowerFloorBlock.compatible(state)) refreshFloor(level, pos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        boolean refresh = !level.isClientSide && !next.is(this) && state.getValue(TALL) && state.getValue(BASE)
                && !isRemovingLinkedStructure(state, level, pos);
        BlockPos origin = refresh ? structureOrigin(state, pos) : pos;
        super.onRemove(state, level, pos, next, moving);
        if (refresh) HotelShowerFloorBlock.refreshAround(level, origin);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = RestaurantKitchenBlock.turn(state);
        return !state.getValue(TALL) ? SHAPES[0][turn] : !state.getValue(PRIMARY) ? SHAPES[2][turn]
                : state.getValue(BASE) ? WITH_BASE[HotelShowerFloorBlock.mask(state)][turn] : SHAPES[1][turn];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
