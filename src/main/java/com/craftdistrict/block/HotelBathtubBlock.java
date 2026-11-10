package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A two-cell freestanding tub uses the existing atomic placement and single-drop lifecycle. */
public final class HotelBathtubBlock extends MultiBlockDecorationBlock {
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 1, 14, 4, 16), box(1, 4, 2, 15, 11, 4),
                    box(1, 4, 4, 3.25, 11, 16), box(12.75, 4, 4, 15, 11, 16)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 0, 14, 4, 15), box(1, 4, 0, 3.25, 11, 12),
                    box(12.75, 4, 0, 15, 11, 12), box(1, 4, 12, 15, 11, 14),
                    box(6, 11, 9.5, 10, 15, 14)).optimize())};
    //? if >=1.21 {
    public static final MapCodec<HotelBathtubBlock> CODEC = simpleCodec(HotelBathtubBlock::new);
    //?}
    public HotelBathtubBlock(Properties properties) { super(properties); }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getOpposite(); }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos floor = pos.below();
        return DecorationSupport.isPlaced(state, level, pos)
                || level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? getExtensionDirection(state) : getExtensionDirection(state).getOpposite();
        if (side == partner && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)]; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
