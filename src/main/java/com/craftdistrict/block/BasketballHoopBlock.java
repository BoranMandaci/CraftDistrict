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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The same hoop attaches to a solid wall or to the side of any basketball pole segment. */
public final class BasketballHoopBlock extends RotatableDecorationBlock {
    private static final BooleanProperty POLE_MOUNTED = BooleanProperty.create("pole_mounted");
    private static final VoxelShape BOARD = box(-3, 3.25, 12.65, 19, 15.85, 14.1);
    private static final VoxelShape BOARD_AND_RIM = Shapes.or(
            BOARD, box(6.3, 5.75, 10.93, 9.7, 6.72, 12.65),
            box(4.96, 5.78, .80, 11.04, 6.61, 1.75), box(4.96, 5.78, 10.52, 11.04, 6.61, 11.46),
            box(2.65, 5.78, 3.05, 3.61, 6.61, 9.23), box(12.39, 5.78, 3.05, 13.35, 6.61, 9.23));
    private static final VoxelShape WALL_MOUNT = Shapes.or(
            box(4.30, 7.12, 15.55, 11.70, 12.10, 16), box(5.39, 7.98, 14.1, 10.61, 11.14, 15.55));
    private static final VoxelShape POLE_BRACKET = Shapes.or(
            box(4.8, 8.15, 14.1, 11.2, 10.8, 14.55),
            box(6.55, 8.8, 14.55, 9.45, 10.2, 24 - SportsPostBlock.Design.BASKETBALL.radius()),
            box(6.36, 6.53, 21.92, 9.64, 10.93, 22.25),
            box(7.47, 7.83, 19.57, 8.53, 9.26, 21.0), box(7.47, 6.60, 21.0, 8.53, 8.26, 22.23));
    private static final VoxelShape[] LEFT_OVERHANG = RestaurantKitchenBlock.rotations(
            Shapes.join(BOARD, box(-16, 0, 0, 0, 16, 16), BooleanOp.AND));
    private static final VoxelShape[] RIGHT_OVERHANG = RestaurantKitchenBlock.rotations(
            Shapes.join(BOARD, box(16, 0, 0, 32, 16, 16), BooleanOp.AND));
    private static final VoxelShape[][] COLLISION = mountedShapes(Shapes.empty());
    private static final VoxelShape[][] OUTLINE = mountedShapes(box(4.11, 1.12, 2.26, 11.89, 5.96, 10.04));

    //? if >=1.21 {
    public static final MapCodec<BasketballHoopBlock> CODEC = simpleCodec(BasketballHoopBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}

    public BasketballHoopBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POLE_MOUNTED, false));
    }

    private static VoxelShape[][] mountedShapes(VoxelShape net) {
        return new VoxelShape[][]{
                RestaurantKitchenBlock.rotations(Shapes.or(BOARD_AND_RIM, WALL_MOUNT, net)),
                RestaurantKitchenBlock.rotations(Shapes.or(BOARD_AND_RIM, POLE_BRACKET, net))
        };
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POLE_MOUNTED);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        if (!facing.getAxis().isHorizontal()) return null;
        BlockPos pos = context.getClickedPos();
        BlockPos behind = pos.relative(facing.getOpposite());
        var level = context.getLevel();
        BlockState mounted = mount(defaultBlockState().setValue(FACING, facing), level.getBlockState(behind), level, behind);
        if (mounted == null) return null;

        int turn = RestaurantKitchenBlock.turn(mounted);
        if (overhangBlocked(level, pos, facing.getCounterClockWise(), LEFT_OVERHANG[turn])
                || overhangBlocked(level, pos, facing.getClockWise(), RIGHT_OVERHANG[turn])) return null;
        return mounted;
    }

    private static boolean overhangBlocked(BlockGetter level, BlockPos pos, Direction side, VoxelShape overhang) {
        BlockPos sidePos = pos.relative(side);
        VoxelShape neighbor = level.getBlockState(sidePos).getShape(level, sidePos);
        return Shapes.joinIsNotEmpty(overhang, neighbor.move(side.getStepX(), 0, side.getStepZ()), BooleanOp.AND);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos behind = pos.relative(state.getValue(FACING).getOpposite());
        return mount(state, level.getBlockState(behind), level, behind) != null;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side != state.getValue(FACING).getOpposite()) return state;
        BlockState mounted = mount(state, neighbor, level, otherPos);
        return mounted == null ? Blocks.AIR.defaultBlockState() : mounted;
    }

    private static BlockState mount(BlockState state, BlockState support, BlockGetter level, BlockPos behind) {
        boolean pole = SportsPostBlock.isPost(support, SportsPostBlock.Design.BASKETBALL);
        if (!pole && !support.isFaceSturdy(level, behind, state.getValue(FACING))) return null;
        return state.setValue(POLE_MOUNTED, pole);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE[state.getValue(POLE_MOUNTED) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION[state.getValue(POLE_MOUNTED) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }
}
