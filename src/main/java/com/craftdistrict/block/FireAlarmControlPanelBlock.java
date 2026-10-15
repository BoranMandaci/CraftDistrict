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

/** Decorative panel using the shared clicked-wall placement/support rules. */
public final class FireAlarmControlPanelBlock extends BakerySignageBlock {
    //? if >=1.21 {
    public static final MapCodec<FireAlarmControlPanelBlock> CODEC = simpleCodec(FireAlarmControlPanelBlock::new);
    //?}

    public FireAlarmControlPanelBlock(Properties properties) { super(Variant.WALL_MENU, properties); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FireInteriorShapes.get(1, 0, state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
