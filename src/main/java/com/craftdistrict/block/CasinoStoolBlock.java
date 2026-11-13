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
    private static final VoxelShape SHAPE = Shapes.or(box(3, 0, 3, 13, 1.5, 13),
            box(7, 1.5, 7, 9, 11, 9), box(4.25, 6, 4.25, 11.75, 6.5, 11.75),
            box(2.5, 10.75, 2.5, 13.5, 13, 13.5)).optimize();
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
