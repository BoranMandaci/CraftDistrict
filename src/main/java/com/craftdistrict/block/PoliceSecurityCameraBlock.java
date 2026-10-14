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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Fixed wall/ceiling mounting with vanilla support-loss removal; no camera logic. */
public final class PoliceSecurityCameraBlock extends RotatableDecorationBlock {
    public static final BooleanProperty CEILING = BooleanProperty.create("ceiling");
    //? if >=1.21 {
    public static final MapCodec<PoliceSecurityCameraBlock> CODEC = simpleCodec(PoliceSecurityCameraBlock::new);
    //?}

    public PoliceSecurityCameraBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CEILING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CEILING);
    }

    private static Direction supportDirection(BlockState state) {
        return state.getValue(CEILING) ? Direction.UP
                : state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = supportDirection(state);
        BlockPos support = pos.relative(direction);
        return level.getBlockState(support).isFaceSturdy(level, support, direction.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face == Direction.UP) {
            return null;
        }
        boolean ceiling = face == Direction.DOWN;
        Direction facing = ceiling ? context.getHorizontalDirection().getOpposite() : face;
        BlockState state = defaultBlockState().setValue(CEILING, ceiling)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == supportDirection(state) && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PolicePackage2Shapes.get(state.getValue(CEILING) ? 8 : 6, 0, state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
