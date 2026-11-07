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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The same two-part basket lies on the floor or is stored horizontally on wall brackets. */
public final class RescueStretcherBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    private static final VoxelShape[][] WALL_SHAPES = {
            RestaurantKitchenBlock.rotations(box(1.5, .75, 9.75, 16, 15.25, 16)),
            RestaurantKitchenBlock.rotations(box(0, .75, 9.75, 14.5, 15.25, 16))
    };
    //? if >=1.21 {
    public static final MapCodec<RescueStretcherBlock> CODEC = simpleCodec(RescueStretcherBlock::new);
    //?}

    public RescueStretcherBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WALL, false));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(WALL);
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(WALL, context.getClickedFace().getAxis().isHorizontal());
    }

    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return face.getAxis().isHorizontal() ? face : super.getPlacementFacing(context);
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(WALL) ? facing.getClockWise() : facing.getOpposite();
    }

    private Direction supportDirection(BlockState state) {
        return state.getValue(WALL) ? state.getValue(FACING).getOpposite() : Direction.DOWN;
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return state.getValue(WALL) && mirror != Mirror.NONE
                ? result.setValue(PRIMARY, !state.getValue(PRIMARY)) : result;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction direction = supportDirection(state);
        BlockPos support = pos.relative(direction);
        return level.getBlockState(support).isFaceSturdy(level, support, direction.getOpposite());
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
        return state.getValue(WALL) ? WALL_SHAPES[partIndex(state)][RestaurantKitchenBlock.turn(state)]
                : FireRescueShapes.get(4, partIndex(state), state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
