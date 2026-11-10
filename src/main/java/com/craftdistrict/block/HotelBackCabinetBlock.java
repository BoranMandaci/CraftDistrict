package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared paired-cabinet lifecycle and reciprocal row connections, with shallower lobby proportions. */
public final class HotelBackCabinetBlock extends MedicalSupplyCabinetBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(0, 0, 4, 16, 16, 16));
    //? if >=1.21 {
    public static final MapCodec<HotelBackCabinetBlock> CODEC = simpleCodec(HotelBackCabinetBlock::new);
    //?}
    public HotelBackCabinetBlock(Properties properties) { super(properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
