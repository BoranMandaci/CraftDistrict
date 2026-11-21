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

public final class BankAtmBlock extends MultiBlockDecorationBlock {
    private static final VoxelShape[] LOWER = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1.55, 0, 1.75, 14.45, .80, 14.40),
            box(1.86, .80, 2.15, 14.14, 1.38, 14.16),
            box(2.05, 1.38, 3.55, 13.95, 16, 13.85),
            box(2.05, 1.38, 2.35, 13.95, 10.01, 3.55),
            box(2.05, 10.01, 2.35, 5.92, 11.35, 3.55),
            box(10.08, 10.01, 2.35, 13.95, 11.35, 3.55),
            box(5.92, 10.01, 2.30, 10.08, 10.16, 3.55),
            box(2.05, 11.35, 2.35, 13.95, 13.30, 3.55),
            box(2.05, 13.30, 2.00, 13.95, 16, 3.55),
            box(3.11, 14.30, 1.78, 6.47, 15.64, 2.00)));
    private static final VoxelShape[] UPPER = RestaurantKitchenBlock.rotations(Shapes.or(
            box(2.05, 0, 4.95, 13.95, 11.94, 13.85),
            box(2.05, 0, 2.00, 13.95, 1.48, 4.95),
            box(2.05, 1.48, 1.70, 13.95, 3.20, 4.95),
            box(2.19, 3.20, 1.82, 13.81, 3.50, 4.95),
            box(1.90, 2.65, 1.80, 2.61, 3.67, 3.04),
            box(13.39, 2.65, 1.80, 14.10, 3.67, 3.04),
            // Approximate the angled display shroud with three shallow steps.
            box(1.83, 3.50, 1.88, 14.17, 5.75, 4.95),
            box(1.83, 5.75, 2.70, 14.17, 8.10, 4.95),
            box(1.83, 8.10, 3.70, 14.17, 10.58, 4.95),
            box(1.63, 10.58, 2.96, 14.37, 13.14, 7.15),
            box(1.94, 11.94, 7.15, 14.06, 12.56, 14.10)));

    public BankAtmBlock(Properties properties) {
        super(properties);
    }

    @Override protected Direction getExtensionDirection(BlockState state) {
        return Direction.UP;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && !isRemovingLinkedStructure(state, level, pos)
                && neighbor != state.setValue(PRIMARY, !state.getValue(PRIMARY))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(PRIMARY) ? LOWER : UPPER)[RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<BankAtmBlock> CODEC = simpleCodec(BankAtmBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
