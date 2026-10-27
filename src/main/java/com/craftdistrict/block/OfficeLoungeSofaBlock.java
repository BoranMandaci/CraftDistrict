package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floor-based Office upholstery; shared reciprocal connections and sitting stay unchanged. */
public final class OfficeLoungeSofaBlock extends ConnectedRestaurantSeatBlock {
    //? if >=1.21 {
    public static final MapCodec<OfficeLoungeSofaBlock> CODEC = simpleCodec(OfficeLoungeSofaBlock::new);
    //?}
    private static final VoxelShape[][] SHAPES = buildShapes();
    public OfficeLoungeSofaBlock(Properties properties) { super(Design.MODERN_BOOTH, properties); }
    private static VoxelShape turn(VoxelShape shape, int turns) {
        for (int i = 0; i < turns; i++) {
            VoxelShape next = Shapes.empty();
            for (AABB b : shape.toAabbs()) next = Shapes.or(next, Shapes.box(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX));
            shape = next.optimize();
        }
        return shape;
    }
    private static VoxelShape[][] buildShapes() {
        VoxelShape[][] all = new VoxelShape[Connection.values().length][];
        for (Connection c : Connection.values()) {
            // The upholstered plinth and cushion share one simple collision volume.
            VoxelShape shape = box(0, 0, 0, 16, 9.5, 16);
            shape = Shapes.or(shape, box(c.corner == Corner.OUTER_RIGHT ? 12 : 0, 9.5, 12,
                    c.corner == Corner.OUTER_LEFT ? 4 : 16, 16.5, 16));
            if (c.corner == Corner.INNER_LEFT) shape = Shapes.or(shape, box(0, 9.5, 0, 4, 16.5, 12));
            if (c.corner == Corner.INNER_RIGHT) shape = Shapes.or(shape, box(12, 9.5, 0, 16, 16.5, 12));
            if (!c.left) shape = Shapes.or(shape, turn(box(0, 9.5, 2, 2, 13.5, 12),
                    c.corner == Corner.INNER_LEFT ? 1 : c.corner == Corner.OUTER_RIGHT ? 3 : 0));
            if (!c.right) shape = Shapes.or(shape, turn(box(14, 9.5, 2, 16, 13.5, 12),
                    c.corner == Corner.INNER_RIGHT ? 3 : c.corner == Corner.OUTER_LEFT ? 1 : 0));
            all[c.ordinal()] = RestaurantKitchenBlock.rotations(shape);
        }
        return all;
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
