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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Sixteen static layouts share one pile surface and only draw borders on exposed edges. */
public final class HotelRoomRugBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
            SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, .1875, 16);
    //? if >=1.21 {
    public static final MapCodec<HotelRoomRugBlock> CODEC = simpleCodec(HotelRoomRugBlock::new);
    //?}
    public HotelRoomRugBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }
    private static BooleanProperty property(Direction side) {
        return switch (side) { case NORTH -> NORTH; case EAST -> EAST; case SOUTH -> SOUTH; case WEST -> WEST;
            default -> throw new IllegalArgumentException("Horizontal rug edge required"); };
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(NORTH, EAST, SOUTH, WEST); }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = pos.below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos next = context.getClickedPos().relative(side);
            state = state.setValue(property(side), context.getLevel().hasChunkAt(next) && context.getLevel().getBlockState(next).is(this));
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side.getAxis().isHorizontal()) return state.setValue(property(side), neighbor.is(this));
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state;
        for (Direction side : Direction.Plane.HORIZONTAL) result = result.setValue(property(rotation.rotate(side)), state.getValue(property(side)));
        return result;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state;
        for (Direction side : Direction.Plane.HORIZONTAL) result = result.setValue(property(mirror.mirror(side)), state.getValue(property(side)));
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    //? if >=1.21 {
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    //?}
}
