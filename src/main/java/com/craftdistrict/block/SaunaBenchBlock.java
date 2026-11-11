package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Reuses reciprocal corner ports and temporary seats; every timber section has a stable trestle. */
public final class SaunaBenchBlock extends ConnectedRestaurantSeatBlock {
    private static final VoxelShape[][] SHAPES = createShapes();
    //? if >=1.21 {
    public static final MapCodec<SaunaBenchBlock> CODEC = simpleCodec(SaunaBenchBlock::new);
    //?}
    public SaunaBenchBlock(Properties properties) { super(Design.BENCH, properties); }
    @Override public double seatHeight() { return 8.5 / 16; }
    @Override public double forwardOffset() { return 0; }

    private static VoxelShape[][] createShapes() {
        VoxelShape[] leftEnds = RestaurantKitchenBlock.rotations(box(0, 6, 2, .5, 6.75, 14));
        VoxelShape[] rightEnds = RestaurantKitchenBlock.rotations(box(15.5, 6, 2, 16, 6.75, 14));
        VoxelShape straight = Shapes.or(box(0, 7.5, 2, 16, 8.5, 14),
                box(6.5, 0, 3.25, 9.5, 6.75, 4.75), box(6.5, 0, 11.25, 9.5, 6.75, 12.75),
                box(6.5, 6.75, 3.25, 9.5, 7.5, 12.5), box(6.5, 1.5, 4.75, 9.5, 2.25, 11.25));
        // One diagonal infill and trestle rotate to all four reciprocal corner layouts.
        VoxelShape[] corners = RestaurantKitchenBlock.rotations(Shapes.or(
                box(2, 7.5, 2, 14, 8.5, 14), box(2, 6.75, 0, 14, 8.5, 2),
                box(14, 6.75, 2, 16, 8.5, 14), box(2, 6.75, 2, 14, 7.5, 14),
                box(3.5, 0, 3.5, 6.875, 6.75, 6.875), box(9.125, 0, 9.125, 12.5, 6.75, 12.5),
                box(4.5, 1.5, 4.5, 11.5, 2.25, 11.5)).optimize());
        VoxelShape[][] result = new VoxelShape[Connection.values().length][];
        for (Connection c : Connection.values()) {
            VoxelShape shape = switch (c.corner) {
                case STRAIGHT -> straight;
                case INNER_LEFT -> corners[0]; case INNER_RIGHT -> corners[3];
                case OUTER_LEFT -> corners[2]; case OUTER_RIGHT -> corners[1];
            };
            if (!c.left) shape = Shapes.or(shape, leftEnds[c.corner == Corner.INNER_LEFT ? 1 : c.corner == Corner.OUTER_RIGHT ? 3 : 0]);
            if (!c.right) shape = Shapes.or(shape, rightEnds[c.corner == Corner.INNER_RIGHT ? 3 : c.corner == Corner.OUTER_LEFT ? 1 : 0]);
            result[c.ordinal()] = RestaurantKitchenBlock.rotations(shape.optimize());
        }
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(CONNECTION).ordinal()][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
