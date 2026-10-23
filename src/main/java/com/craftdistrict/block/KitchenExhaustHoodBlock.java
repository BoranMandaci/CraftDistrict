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

/** Wall-facing placement and support removal are shared with existing wall decorations. */
public final class KitchenExhaustHoodBlock extends BakerySignageBlock {
    public static final BooleanProperty DUCT_ABOVE = BooleanProperty.create("duct_above");
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            Block.box(0, 3.75, 2.75, 16, 8, 16), Block.box(3, 8, 9, 13, 10, 16),
            Block.box(5, 10, 12, 11, 16, 16)));
    //? if >=1.21 {
    public static final MapCodec<KitchenExhaustHoodBlock> CODEC = simpleCodec(KitchenExhaustHoodBlock::new);
    //?}

    public KitchenExhaustHoodBlock(Properties properties) {
        super(Variant.WALL_SHOP, properties);
        registerDefaultState(defaultBlockState().setValue(DUCT_ABOVE, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DUCT_ABOVE);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(DUCT_ABOVE, context.getLevel().getBlockState(context.getClickedPos().above()).getBlock() instanceof KitchenExhaustDuctBlock);
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState supported = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return supported.isAir() || direction != Direction.UP ? supported
                : supported.setValue(DUCT_ABOVE, neighbor.getBlock() instanceof KitchenExhaustDuctBlock);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
