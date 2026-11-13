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

/** A reserved 2-by-2 vertical fixture; no spinning, ticking or game state. */
public final class BigSixWheelBlock extends WideTallDecorationBlock {
    private static final VoxelShape[][] SHAPES = createShapes();
    private static VoxelShape[][] createShapes() {
        VoxelShape whole = Shapes.or(box(2, 0, 3, 30, 2, 13), box(9, 2, 4, 23, 6, 12),
                box(13.5, 6, 8.5, 18.5, 21, 12.5),
                box(3, 12, 5.25, 29, 26, 8.5), box(8, 6.25, 5.25, 24, 31.75, 8.5)).optimize();
        VoxelShape[][] result = new VoxelShape[4][];
        for (int part = 0; part < 4; part++) result[part] = RestaurantKitchenBlock.rotations(
                Shapes.join(whole.move(-(part % 2), -(part / 2), 0), Shapes.block(), BooleanOp.AND).optimize());
        return result;
    }
    //? if >=1.21 {
    public static final MapCodec<BigSixWheelBlock> CODEC = simpleCodec(BigSixWheelBlock::new);
    //?}
    public BigSixWheelBlock(Properties properties) { super(properties); }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).relative(state.getValue(FACING).getClockWise(), state.getValue(PART) % 2).below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int part = 0; part < 4; part++) {
            if (otherPos.equals(origin.offset(partOffset(state, part))) && neighbor != stateForPart(state, part))
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
