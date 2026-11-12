package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** New showers reserve their taller head's cell; saved two-cell showers remain intact. */
public final class PoolsideShowerBlock extends HotelPoolFixtureBlock {
    public static final BooleanProperty TALL = BooleanProperty.create("tall");
    private static final VoxelShape[][] TALL_SHAPES = createTallShapes();
    //? if >=1.21 {
    public static final MapCodec<PoolsideShowerBlock> CODEC = simpleCodec(PoolsideShowerBlock::new);
    //?}
    public PoolsideShowerBlock(Properties properties) {
        super(Design.SHOWER, properties);
        registerDefaultState(defaultBlockState().setValue(TALL, false));
    }
    private static VoxelShape[][] createTallShapes() {
        VoxelShape whole = Shapes.or(box(4.5, 0, 8, 11.5, .75, 15),
                box(6.625, .75, 10.625, 9.375, 39.9375, 13.375),
                box(7.25, 39.25, 4.5, 8.75, 39.875, 12), box(3.75, 38.75, .75, 12.25, 39.75, 8.25),
                box(6.25, 14, 8.75, 9.75, 21, 10.75)).optimize();
        VoxelShape[][] shapes = new VoxelShape[3][];
        for (int part = 0; part < shapes.length; part++) shapes[part] = RestaurantKitchenBlock.rotations(
                Shapes.join(whole.move(0, -part, 0), Shapes.block(), BooleanOp.AND).optimize());
        return shapes;
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(TALL);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(TALL) ? 3 : 2; }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(TALL, true);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TALL) ? TALL_SHAPES[state.getValue(PART)][RestaurantKitchenBlock.turn(state)]
                : super.getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
