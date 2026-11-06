package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-cell hospital fixtures use the shared atomic placement and single-drop removal. */
public final class HospitalWideBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        HEAD_PANEL, RECEPTION;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<HospitalWideBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HospitalWideBlock b) -> b.design),
            propertiesCodec()).apply(instance, HospitalWideBlock::new));
    //?}
    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(box(1, 4, 12.75, 16, 12.5, 16)),
                    RestaurantKitchenBlock.rotations(box(0, 4, 12.75, 15, 12.5, 16))},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 0, 16, 13.75, 16),
                    box(0, 13.75, 0, 16, 15.25, 2), box(2.5, 13.75, 7.5, 13.5, 21.75, 14.5)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 0, 16, 13.75, 16),
                            box(0, 13.75, 0, 16, 15.25, 2), box(1.75, 13.75, 6.5, 14.5, 18, 14.5)).optimize())}};

    public HospitalWideBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getClockWise(); }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        return design == Design.HEAD_PANEL
                ? (context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null)
                : super.getPlacementFacing(context);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction side = design == Design.HEAD_PANEL ? state.getValue(FACING) : Direction.UP;
        BlockPos support = design == Design.HEAD_PANEL ? pos.relative(side.getOpposite()) : pos.below();
        return level.getBlockState(support).isFaceSturdy(level, support, side);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction along = getExtensionDirection(state);
        Direction partner = state.getValue(PRIMARY) ? along : along.getOpposite();
        if (side == partner && other != stateForPart(state, 1 - partIndex(state))) return Blocks.AIR.defaultBlockState();
        Direction support = design == Design.HEAD_PANEL ? state.getValue(FACING).getOpposite() : Direction.DOWN;
        return side == support && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(PRIMARY, !state.getValue(PRIMARY));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
