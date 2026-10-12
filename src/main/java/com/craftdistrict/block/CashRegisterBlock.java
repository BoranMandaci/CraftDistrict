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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Small static countertop decoration; all shape work is performed once at startup. */
public final class CashRegisterBlock extends RotatableDecorationBlock {
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2.25, 14),
            Block.box(2.75, 2.25, 6, 13.25, 3.25, 13.25),
            Block.box(2.75, 3.25, 10, 13.25, 4.5, 13.25),
            Block.box(3, 2.25, 3.25, 11, 4.25, 6.5),
            Block.box(3, 3.25, 6.5, 11, 5.5, 10.75),
            Block.box(11, 2.25, 6, 13.25, 6.5, 13.25),
            Block.box(5.75, 4.5, 10.75, 7.25, 7.25, 12.25),
            Block.box(3.25, 7, 10.5, 11.25, 10, 12.75)).optimize();
    private static final VoxelShape EAST_SHAPE = turnClockwise(NORTH_SHAPE);
    private static final VoxelShape SOUTH_SHAPE = turnClockwise(EAST_SHAPE);
    private static final VoxelShape WEST_SHAPE = turnClockwise(SOUTH_SHAPE);
    //? if >=1.21 {
    public static final MapCodec<CashRegisterBlock> CODEC = simpleCodec(CashRegisterBlock::new);
    //?}

    public CashRegisterBlock(Properties properties) {
        super(properties);
    }

    private static VoxelShape turnClockwise(VoxelShape shape) {
        VoxelShape result = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            result = Shapes.or(result, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                    1 - box.minZ, box.maxY, box.maxX));
        }
        return result.optimize();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos supportPos = pos.below();
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            // Vanilla neighbor-shape propagation removes the block and handles its loot.
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
