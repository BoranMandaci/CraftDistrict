package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Each fixture carries its own rail mount up to the block boundary. */
public abstract class OfficeTurnstileFixtureBlock extends CashRegisterBlock {
    private static final BooleanProperty SIDE_BARRIER = BooleanProperty.create("side_barrier");

    protected OfficeTurnstileFixtureBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SIDE_BARRIER, false));
    }

    abstract Direction barrierSide(BlockState state);

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIDE_BARRIER);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        BlockPos otherPos = context.getClickedPos().relative(barrierSide(state));
        return withBarrier(state, context.getLevel().getBlockState(otherPos));
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side == barrierSide(state)) return withBarrier(state, neighbor);
        return super.updateShape(state, side, neighbor, level, pos, otherPos);
    }

    private static BlockState withBarrier(BlockState state, BlockState neighbor) {
        return state.setValue(SIDE_BARRIER, neighbor.getBlock() instanceof OfficeTurnstileSideBarrierBlock);
    }

    protected static VoxelShape[][] mountedShapes(VoxelShape base, VoxelShape mount) {
        return new VoxelShape[][] { RestaurantKitchenBlock.rotations(base),
                RestaurantKitchenBlock.rotations(Shapes.or(base, mount)) };
    }

    protected static VoxelShape fixtureShape(VoxelShape[][] shapes, BlockState state) {
        return shapes[state.getValue(SIDE_BARRIER) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }

    public static final class EndPost extends OfficeTurnstileFixtureBlock {
        //? if >=1.21 {
        public static final MapCodec<EndPost> CODEC = simpleCodec(EndPost::new);
        //?}
        private static final VoxelShape[][] SHAPES = mountedShapes(Shapes.or(box(1, 0, 2.5, 4, 1, 13.5),
                box(1.25, 1, 3, 3.75, 15, 13), box(1.25, 15, 3.25, 3.75, 15.5, 12.75)),
                OfficeTurnstileSideBarrierBlock.rails(3.75, 7.25, 16, 8.75));

        public EndPost(Properties properties) { super(properties); }

        @Override Direction barrierSide(BlockState state) { return state.getValue(FACING).getClockWise(); }

        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return fixtureShape(SHAPES, state);
        }

        //? if >=1.21 {
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
    }
}
