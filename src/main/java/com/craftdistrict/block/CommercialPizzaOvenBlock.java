package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static four-deck oven using the shared vertical placement and single-drop lifecycle. */
public final class CommercialPizzaOvenBlock extends MultiBlockDecorationBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Block.box(0, 0, 0, 16, 16, 16));
    //? if >=1.21 {
    public static final MapCodec<CommercialPizzaOvenBlock> CODEC = simpleCodec(CommercialPizzaOvenBlock::new);
    //?}

    public CommercialPizzaOvenBlock(Properties properties) { super(properties); }

    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos floor = pos.below(partIndex(state) + 1);
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && (!other.is(this) || other.getValue(PRIMARY) == state.getValue(PRIMARY)
                || other.getValue(FACING) != state.getValue(FACING))) return Blocks.AIR.defaultBlockState();
        return state;
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
