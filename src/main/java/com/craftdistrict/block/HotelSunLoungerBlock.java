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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A static two-cell chaise uses the same atomic placement and single-drop removal as the hotel tables. */
public final class HotelSunLoungerBlock extends MultiBlockDecorationBlock {
    private static final VoxelShape[][] SHAPES = createShapes();
    //? if >=1.21 {
    public static final MapCodec<HotelSunLoungerBlock> CODEC = simpleCodec(HotelSunLoungerBlock::new);
    //?}
    public HotelSunLoungerBlock(Properties properties) { super(properties); }
    private static VoxelShape[][] createShapes() {
        VoxelShape whole = Shapes.or(box(1, 5.5, 14, 15, 8.75, 31.25),
                box(1.125, 9.5, 1, 14.875, 15, 7), box(1.125, 6.25, 7, 14.875, 12, 14),
                box(1.25, 0, 6, 2.75, 7.125, 10.5), box(13.25, 0, 6, 14.75, 7.125, 10.5),
                box(1.25, 0, 24.5, 2.75, 7.125, 29), box(13.25, 0, 24.5, 14.75, 7.125, 29),
                box(2, .75, 6.625, 14, 1.5, 7.375), box(2, .75, 27.625, 14, 1.5, 28.375)).optimize();
        VoxelShape[][] shapes = new VoxelShape[2][];
        for (int part = 0; part < 2; part++) shapes[part] = RestaurantKitchenBlock.rotations(
                Shapes.join(whole.move(0, 0, -part), Shapes.block(), BooleanOp.AND).optimize());
        return shapes;
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getOpposite(); }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return DecorationSupport.isPlaced(state, level, pos)
                || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? getExtensionDirection(state) : getExtensionDirection(state).getOpposite();
        if (side == partner && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
