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

/** The existing seat interaction owns occupancy, dismounting and disconnect/removal cleanup. */
public final class CasinoStoolBlock extends RestaurantSeatBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(4, 0, 4, 12, 1.2, 12),
            box(7.325, 1.2, 7.325, 8.675, 11.3, 8.675), box(5, 6, 5, 11, 6.375, 11),
            box(3.5, 11.3, 3.5, 12.5, 13, 12.5)).optimize();
    //? if >=1.21 {
    public static final MapCodec<CasinoStoolBlock> CODEC = simpleCodec(CasinoStoolBlock::new);
    //?}
    public CasinoStoolBlock(Properties properties) { super(Design.BAR_STOOL, properties); }
    @Override public double seatHeight() { return 13.0 / 16; }
    @Override public double forwardOffset() { return 0; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
