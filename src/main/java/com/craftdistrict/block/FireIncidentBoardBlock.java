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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four thin wall cells, reusing the shared validation and linked-removal lifecycle. */
public final class FireIncidentBoardBlock extends WideTallDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<FireIncidentBoardBlock> CODEC = simpleCodec(FireIncidentBoardBlock::new);
    //?}

    public FireIncidentBoardBlock(Properties properties) { super(properties); }

    @Override
    protected Direction getPlacementFacing(BlockPlaceContext context) {
        Direction face=context.getClickedFace();
        return face.getAxis().isHorizontal() ? face : null;
    }

    private Direction supportDirection(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction=supportDirection(state);
        BlockPos support=pos.relative(direction);
        return level.getBlockState(support).isFaceSturdy(level,support,direction.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction==supportDirection(state) && !state.canSurvive(level,pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state,direction,neighbor,level,pos,neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FireFixtureShapes.get(1,partIndex(state),state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state,level,pos,context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
