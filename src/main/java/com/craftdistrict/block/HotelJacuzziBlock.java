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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The existing four-part transaction is laid flat; the water is model geometry only. */
public final class HotelJacuzziBlock extends WideTallDecorationBlock {
    private static final VoxelShape[][] SHAPES = createShapes();
    //? if >=1.21 {
    public static final MapCodec<HotelJacuzziBlock> CODEC = simpleCodec(HotelJacuzziBlock::new);
    //?}

    public HotelJacuzziBlock(Properties properties) { super(properties); }

    private static VoxelShape[][] createShapes() {
        VoxelShape whole = Shapes.or(box(2, 0, 2, 30, 3.5, 30),
                box(3, 3.5, .5, 29, 12.5, 4), box(3, 3.5, 28, 29, 12.5, 31.5),
                box(.5, 3.5, 3, 4, 12.5, 29), box(28, 3.5, 3, 31.5, 12.5, 29),
                box(4, 3.5, 4, 8, 6.5, 28), box(24, 3.5, 4, 28, 6.5, 28),
                box(8, 3.5, 24, 24, 6.5, 28), box(14.25, 12.5, 25.75, 16.25, 15.5, 30.5),
                box(22, 12.5, 29, 26, 13, 30.5)).optimize();
        VoxelShape[][] result = new VoxelShape[4][];
        for (int part = 0; part < 4; part++) {
            VoxelShape local = whole.move(-(part % 2), 0, -(part / 2));
            result[part] = RestaurantKitchenBlock.rotations(Shapes.join(local, Shapes.block(), BooleanOp.AND).optimize());
        }
        return result;
    }

    @Override protected BlockPos partOffset(BlockState state, int index) {
        Direction facing = state.getValue(FACING);
        return BlockPos.ZERO.relative(facing.getClockWise(), index % 2).relative(facing.getOpposite(), index / 2);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos floor = pos.below();
        return DecorationSupport.isPlaced(state, level, pos)
                || level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int part = 0; part < 4; part++) {
            if (origin.offset(partOffset(state, part)).equals(otherPos) && neighbor != stateForPart(state, part))
                return Blocks.AIR.defaultBlockState();
        }
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PART)][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
