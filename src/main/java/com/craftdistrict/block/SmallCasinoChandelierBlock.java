package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SmallCasinoChandelierBlock extends RotatableDecorationBlock implements ControlledLight {
    private static final VoxelShape SHAPE = Shapes.or(box(5.46, 14.94, 5.46, 10.54, 16, 10.54),
            box(6.67, 14.26, 6.67, 9.33, 14.94, 9.33), box(7.35, 8.32, 7.35, 8.65, 14.26, 8.65),
            box(6.48, 6.11, 6.48, 9.52, 8.32, 9.52), box(7.11, 3.12, 7.11, 8.89, 6.11, 8.89),
            box(3.35, 5.90, 7.65, 12.65, 7.99, 8.35), box(7.65, 5.90, 3.35, 8.35, 7.99, 12.65),
            box(2.45, 7.34, 6.93, 4.59, 9.91, 9.07), box(11.41, 7.34, 6.93, 13.55, 9.91, 9.07),
            box(6.93, 7.34, 2.45, 9.07, 9.91, 4.59), box(6.93, 7.34, 11.41, 9.07, 9.91, 13.55)).optimize();

    public SmallCasinoChandelierBlock(Properties properties) {
        super(properties);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.DOWN) return null;
        BlockState state = super.getStateForPlacement(context);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos ceiling = pos.above();
        return level.hasChunkAt(ceiling) && level.getBlockState(ceiling).isFaceSturdy(level, ceiling, Direction.DOWN);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    //? if >=1.21 {
    public static final MapCodec<SmallCasinoChandelierBlock> CODEC = simpleCodec(SmallCasinoChandelierBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
