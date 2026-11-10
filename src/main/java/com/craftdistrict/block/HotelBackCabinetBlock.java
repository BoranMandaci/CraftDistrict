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

/** Full-depth cabinets retain the paired lifecycle and reciprocal row connections. */
public final class HotelBackCabinetBlock extends MedicalSupplyCabinetBlock {
    private static final VoxelShape SHAPE = Shapes.block();
    //? if >=1.21 {
    public static final MapCodec<HotelBackCabinetBlock> CODEC = simpleCodec(HotelBackCabinetBlock::new);
    //?}
    public HotelBackCabinetBlock(Properties properties) { super(properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
