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
import net.minecraft.world.phys.shapes.VoxelShape;

/** A passive vertical extension aligned with the hood outlet; support propagates upward. */
public final class KitchenExhaustDuctBlock extends RotatableDecorationBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Block.box(5, 0, 12, 11, 16, 16));
    //? if >=1.21 {
    public static final MapCodec<KitchenExhaustDuctBlock> CODEC = simpleCodec(KitchenExhaustDuctBlock::new);
    //?}

    public KitchenExhaustDuctBlock(Properties properties) { super(properties); }

    private boolean alignedSupport(BlockState support, Direction facing) {
        return (support.is(this) || support.getBlock() instanceof KitchenExhaustHoodBlock)
                && support.getValue(FACING) == facing;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos below = pos.below(), behind = pos.relative(facing.getOpposite());
        BlockState support = level.getBlockState(below);
        // Downward dependencies cannot form mutually supporting floating stacks.
        return alignedSupport(support, facing) || support.isFaceSturdy(level, below, Direction.UP)
                || level.getBlockState(behind).isFaceSturdy(level, behind, facing);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState below = context.getLevel().getBlockState(pos.below());
        Direction facing = below.is(this) || below.getBlock() instanceof KitchenExhaustHoodBlock
                ? below.getValue(FACING) : context.getClickedFace().getAxis().isHorizontal()
                ? context.getClickedFace() : context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return state.canSurvive(context.getLevel(), pos) ? state : null;
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return (direction == Direction.DOWN || direction == state.getValue(FACING).getOpposite())
                && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return direction.getAxis() == Direction.Axis.Y
                && (neighbor.is(this) || direction == Direction.DOWN && neighbor.getBlock() instanceof KitchenExhaustHoodBlock)
                && neighbor.getValue(FACING) == state.getValue(FACING)
                || super.skipRendering(state, neighbor, direction);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
