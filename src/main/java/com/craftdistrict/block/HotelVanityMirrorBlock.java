package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** New mirrors are vertical; saved wide mirrors retain their original paired layout. */
public final class HotelVanityMirrorBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty STACKED = BooleanProperty.create("stacked"), TRAY = BooleanProperty.create("tray");
    public static final DirectionProperty TRAY_FACING = DirectionProperty.create("tray_facing", Direction.Plane.HORIZONTAL);
    private static final VoxelShape[][] PARTS = {
            RestaurantKitchenBlock.rotations(box(1.5, .5, 13.125, 16, 15.5, 16)),
            RestaurantKitchenBlock.rotations(box(0, .5, 13.125, 14.5, 15.5, 16)),
            RestaurantKitchenBlock.rotations(box(1, .5, 14.75, 15, 16, 16)),
            RestaurantKitchenBlock.rotations(box(1, 0, 14.75, 15, 8.5, 16))};
    private static final VoxelShape[][] COMBINED = new VoxelShape[4][4];
    static {
        VoxelShape[] trays = RestaurantKitchenBlock.rotations(box(.5, 0, 2.75, 5.5, 6.5, 11.75));
        for (int mirror = 0; mirror < 4; mirror++) for (int tray = 0; tray < 4; tray++)
            COMBINED[mirror][tray] = Shapes.or(PARTS[2][mirror], trays[tray]).optimize();
    }
    //? if >=1.21 {
    public static final MapCodec<HotelVanityMirrorBlock> CODEC = simpleCodec(HotelVanityMirrorBlock::new);
    //?}
    public HotelVanityMirrorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STACKED, false).setValue(TRAY, false).setValue(TRAY_FACING, Direction.NORTH));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(STACKED, TRAY, TRAY_FACING);
    }
    @Override protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(STACKED) ? Direction.UP : state.getValue(FACING).getClockWise();
    }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        return context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null;
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(STACKED, true);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos wall = pos.relative(state.getValue(FACING).getOpposite());
        return DecorationSupport.isPlaced(state, level, pos)
                || level.hasChunkAt(wall) && level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING));
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? getExtensionDirection(state) : getExtensionDirection(state).getOpposite();
        if (side == partner && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return super.rotate(state, rotation).setValue(TRAY_FACING, rotation.rotate(state.getValue(TRAY_FACING)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
                .setValue(TRAY_FACING, mirror.mirror(state.getValue(TRAY_FACING)));
        return mirror != Mirror.NONE && !state.getValue(STACKED) ? result.setValue(PRIMARY, !state.getValue(PRIMARY)) : result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = RestaurantKitchenBlock.turn(state);
        if (!state.getValue(STACKED)) return PARTS[state.getValue(PRIMARY) ? 0 : 1][turn];
        if (!state.getValue(PRIMARY)) return PARTS[3][turn];
        return state.getValue(TRAY) ? COMBINED[turn][RestaurantKitchenBlock.turn(state.setValue(FACING, state.getValue(TRAY_FACING)))] : PARTS[2][turn];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
