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

/** Static full-width kitchenette cabinet, using the existing floor support rules. */
public final class OfficeKitchenetteCounterBlock extends CashRegisterBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(0, 0, 1.5, 16, 1.25, 16), box(0, 1.25, 0, 16, 16, 16)));
    //? if >=1.21 {
    public static final MapCodec<OfficeKitchenetteCounterBlock> CODEC = simpleCodec(OfficeKitchenetteCounterBlock::new);
    //?}
    public OfficeKitchenetteCounterBlock(Properties properties) { super(properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
