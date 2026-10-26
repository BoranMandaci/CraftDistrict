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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-cell floor-standing machine; linked placement/removal is owned by the shared base. */
public final class DoughSheeterBlock extends MultiBlockDecorationBlock {
    // Conveyor height is 16; the roller housing sits above the working surface.
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(
                    Block.box(12, 0, 1.5, 16, 21.375, 14.5),
                    Block.box(0.75, 15.125, 2.5, 12, 16.375, 13.5),
                    Block.box(2.5, 0, 2.5, 5, 15.125, 13.5)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    Block.box(0, 0, 1.5, 4, 21.375, 14.5),
                    Block.box(4, 15.125, 2.5, 15.25, 16.375, 13.5),
                    Block.box(11, 0, 2.5, 13.5, 15.125, 13.5)).optimize())
    };
    //? if >=1.21 {
    public static final MapCodec<DoughSheeterBlock> CODEC = simpleCodec(DoughSheeterBlock::new);
    //?}

    public DoughSheeterBlock(Properties properties) { super(properties); }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(FACING).getClockWise();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos origin = pos.subtract(partOffset(state, partIndex(state)));
        for (int part = 0; part < partCount(); part++) {
            BlockPos floor = origin.offset(partOffset(state, part)).below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return false;
        }
        return true;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                  LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        // The linked removal loop clears the partner without requesting a second loot drop.
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        Direction partner = getExtensionDirection(state);
        if (!state.getValue(PRIMARY)) partner = partner.getOpposite();
        if (side == partner && other != stateForPart(state, 1 - partIndex(state))) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
