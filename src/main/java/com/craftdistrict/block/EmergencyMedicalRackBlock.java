package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static rescue equipment; the shared four-cell lifecycle owns placement and removal. */
public final class EmergencyMedicalRackBlock extends WideTallDecorationBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(0, 0, 1, 16, 16, 15));
    private static final VoxelShape[] TOP_SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(0, 0, 1, 16, 16, 15), box(0, 15.75, 0, 16, 16, 16)).optimize());
    //? if >=1.21 {
    public static final MapCodec<EmergencyMedicalRackBlock> CODEC = simpleCodec(EmergencyMedicalRackBlock::new);
    //?}
    public EmergencyMedicalRackBlock(Properties properties) { super(properties); }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (partIndex(state) >= 2 || DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = pos.below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos other) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (partIndex(state) >= 2 ? TOP_SHAPES : SHAPES)[RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape shape = getShape(state, level, pos, CollisionContext.empty());
        return partIndex(state) >= 2 ? DecorationSupport.withTop(shape) : shape;
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
