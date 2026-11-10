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

public final class HotelDecorationBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        BELL, ENCODER, LUGGAGE_STACK, COFFEE_TABLE, LOGO_SIGN;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(5, 0, 5, 11, 4.5, 11)),
            RestaurantKitchenBlock.rotations(box(3, 0, 3.5, 13, 5.5, 12.5)),
            RestaurantKitchenBlock.rotations(box(1.5, 0, 1.5, 14.5, 15.5, 14.5)),
            RestaurantKitchenBlock.rotations(Shapes.or(box(3, 0, 5, 13, 1.25, 11),
                    box(5, 1.25, 5, 11, 8, 11), box(1, 8, 2.5, 15, 9.5, 13.5)).optimize()),
            RestaurantKitchenBlock.rotations(box(0, 5.5, 14.5, 16, 14.5, 16))};
    //? if >=1.21 {
    public static final MapCodec<HotelDecorationBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HotelDecorationBlock b) -> b.design),
            propertiesCodec()).apply(instance, HotelDecorationBlock::new));
    //?}
    public HotelDecorationBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = design == Design.LOGO_SIGN ? state.getValue(FACING) : Direction.UP;
        BlockPos support = design == Design.LOGO_SIGN ? pos.relative(face.getOpposite()) : pos.below();
        return level.getBlockState(support).isFaceSturdy(level, support, face);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (design == Design.LOGO_SIGN) {
            if (!context.getClickedFace().getAxis().isHorizontal()) return null;
            state = defaultBlockState().setValue(FACING, context.getClickedFace());
        }
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        Direction support = design == Design.LOGO_SIGN ? state.getValue(FACING).getOpposite() : Direction.DOWN;
        return side == support && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
