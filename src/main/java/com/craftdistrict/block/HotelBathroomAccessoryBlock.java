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

/** Static bathroom fittings share mounting rules and a small set of precomputed envelopes. */
public final class HotelBathroomAccessoryBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        SHOWER_FIXTURE(true, Shapes.or(box(5.25, .5, 13.5, 10.75, 15.75, 16),
                box(4.5, 14.125, 7, 11.5, 15.75, 15)).optimize()),
        SHOWER_BASE(false, box(0, 0, 0, 16, 1, 16)),
        TOILET(false, Shapes.or(box(4, 0, 3, 12, 5.5, 15.25), box(2.5, 5.5, 1.5, 13.5, 9.875, 12.5),
                box(3.75, 4.25, 12.5, 12.25, 15, 16)).optimize()),
        PAPER_HOLDER(true, box(5, 4.75, 11.5, 11, 11.25, 16)),
        TOWEL_RAIL(true, box(2, 2.75, 12.75, 14, 14, 16)),
        TOWEL_STACK(false, box(2.75, 0, 3, 13.25, 4.5, 13)),
        TOILETRIES(false, box(.5, 0, 2.75, 5.5, 6.5, 11.75)),
        HAIR_DRYER(true, box(4, 4, 11.25, 12, 13, 16)),
        TRASH_BIN(false, box(4, 0, 3.5, 12, 11, 12.5));

        private final boolean wall;
        private final VoxelShape[] shapes;
        Design(boolean wall, VoxelShape shape) { this.wall = wall; this.shapes = RestaurantKitchenBlock.rotations(shape); }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<HotelBathroomAccessoryBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HotelBathroomAccessoryBlock b) -> b.design),
            propertiesCodec()).apply(instance, HotelBathroomAccessoryBlock::new));
    //?}
    public HotelBathroomAccessoryBlock(Design design, Properties properties) { super(properties); this.design = design; }
    public boolean isToiletries() { return design == Design.TOILETRIES; }
    public boolean isShowerBase() { return design == Design.SHOWER_BASE; }
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
        if (design == Design.TOILETRIES) {
            BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
            if (below.getBlock() instanceof HotelVanitySinkBlock) state = state.setValue(FACING, below.getValue(FACING));
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
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
