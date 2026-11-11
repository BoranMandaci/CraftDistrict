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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** No inventories, block entities or ticking: only mounting and cached collision envelopes. */
public final class RestroomSpaDecorationBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        SOAP_DISPENSER(true, Shapes.or(box(5.75, 5.875, 13.375, 10.25, 11.875, 16),
                box(7.0625, 5.078125, 13.1875, 8.9375, 5.875, 14.875)).optimize()),
        PAPER_TOWEL_DISPENSER(true, Shapes.or(box(3.5, 5, 12, 12.5, 13, 16), box(6, 2.5, 10.75, 10, 5, 12.5)).optimize()),
        TRASH_BIN(false, Shapes.or(box(3.25, 0, 3.25, 12.75, 2.25, 12.75),
                box(3, 2.25, 3, 13, 15.25, 4.5), box(3, 2.25, 11.5, 13, 15.25, 13),
                box(3, 2.25, 4.5, 4.5, 15.25, 11.5), box(11.5, 2.25, 4.5, 13, 15.25, 11.5)).optimize()),
        HEATED_TOWEL_RACK(true, Shapes.or(box(3, 1.5, 13.5, 4, 15, 16), box(12, 1.5, 13.5, 13, 15, 16),
                box(4, 3, 13.625, 12, 3.5, 14.125), box(4.5, 5, 13.0625, 7.25, 10.375, 14.375),
                box(8.25, 7.25, 13.0625, 11, 12.625, 14.375),
                box(4, 12, 13.625, 12, 12.5, 14.125)).optimize()),
        SIGN_WOMEN(true, signShape()), SIGN_MEN(true, signShape()), SIGN_ACCESSIBLE(true, signShape()),
        SAUNA_HEATER(false, Shapes.or(box(3, 0, 3, 13, 13, 13), box(3.5, 13, 3.5, 12.5, 15, 12.5)).optimize());

        private final boolean wall;
        private final VoxelShape[] shapes;
        Design(boolean wall, VoxelShape shape) { this.wall = wall; shapes = RestaurantKitchenBlock.rotations(shape); }
        private static VoxelShape signShape() { return box(3.75, 3.75, 15.375, 12.25, 12.25, 16); }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<RestroomSpaDecorationBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((RestroomSpaDecorationBlock block) -> block.design),
            propertiesCodec()).apply(instance, RestroomSpaDecorationBlock::new));
    //?}

    public RestroomSpaDecorationBlock(Design design, Properties properties) { super(properties); this.design = design; }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = design.wall ? state.getValue(FACING) : Direction.UP;
        BlockPos support = design.wall ? pos.relative(face.getOpposite()) : pos.below();
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (design.wall && !context.getClickedFace().getAxis().isHorizontal()) return null;
        BlockState state = super.getStateForPlacement(context);
        if (design.wall) state = state.setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        Direction support = design.wall ? state.getValue(FACING).getOpposite() : Direction.DOWN;
        return side == support && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return design.shapes[RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
