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

/** Existing two-cell wall sign validation and linked removal, with fire-service artwork. */
public final class FireDepartmentSignBlock extends PoliceEntranceBlock {
    //? if >=1.21 {
    public static final MapCodec<FireDepartmentSignBlock> CODEC = simpleCodec(FireDepartmentSignBlock::new);
    //?}

    public FireDepartmentSignBlock(Properties properties) { super(Variant.DEPARTMENT_SIGN, properties); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FireInteriorShapes.get(0, partIndex(state), state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
