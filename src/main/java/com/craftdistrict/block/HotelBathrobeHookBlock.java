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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A two-high robe hangs from the upper wall attachment, without an entity or cloth simulation. */
public final class HotelBathrobeHookBlock extends MultiBlockDecorationBlock {
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(4.125, 2, 11.5, 11.875, 16, 14.75)),
            RestaurantKitchenBlock.rotations(box(2, 0, 11.25, 14, 15.5, 16))};
    //? if >=1.21 {
    public static final MapCodec<HotelBathrobeHookBlock> CODEC = simpleCodec(HotelBathrobeHookBlock::new);
    //?}
    public HotelBathrobeHookBlock(Properties properties) { super(properties); }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) { return context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos wall = structureOrigin(state, pos).above().relative(state.getValue(FACING).getOpposite());
        return DecorationSupport.isPlaced(state, level, pos)
                || level.hasChunkAt(wall) && level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING));
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)]; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
