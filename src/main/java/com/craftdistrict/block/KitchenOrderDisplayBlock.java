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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-cell wall display; the shared multiblock system validates both mounting positions. */
public final class KitchenOrderDisplayBlock extends MultiBlockDecorationBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            Block.box(0, 2.5, 12.625, 16, 14.5, 15), Block.box(5, 5, 15, 11, 12, 16)));
    //? if >=1.21 {
    public static final MapCodec<KitchenOrderDisplayBlock> CODEC = simpleCodec(KitchenOrderDisplayBlock::new);
    //?}

    public KitchenOrderDisplayBlock(Properties properties) { super(properties); }

    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        Direction side = context.getClickedFace();
        return side.getAxis().isHorizontal() ? side : null;
    }

    @Override protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(FACING).getClockWise();
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos behind = pos.relative(facing.getOpposite());
        return level.getBlockState(behind).isFaceSturdy(level, behind, facing);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return side == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
