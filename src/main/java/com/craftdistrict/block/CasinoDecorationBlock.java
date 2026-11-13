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
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cached selection/collision envelopes for the sign and small tabletop accessories. */
public final class CasinoDecorationBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        JACKPOT, CHIPS, SHOE, RACK, CARDS;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(.5, 3, 13.75, 15.5, 13, 16)),
            RestaurantKitchenBlock.rotations(box(1.75, 0, 3.5, 13.5, 3, 12.5)),
            RestaurantKitchenBlock.rotations(box(3, 0, 3.5, 13, 5, 13)),
            RestaurantKitchenBlock.rotations(box(1.5, 0, 3.5, 14.5, 3.5, 12.5)),
            RestaurantKitchenBlock.rotations(box(2, 0, 3, 14, 2.25, 13))};
    //? if >=1.21 {
    public static final MapCodec<CasinoDecorationBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((CasinoDecorationBlock b) -> b.design),
            propertiesCodec()).apply(instance, CasinoDecorationBlock::new));
    //?}
    public CasinoDecorationBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = design == Design.JACKPOT ? state.getValue(FACING) : Direction.UP;
        BlockPos support = design == Design.JACKPOT ? pos.relative(face.getOpposite()) : pos.below();
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (design == Design.JACKPOT && !context.getClickedFace().getAxis().isHorizontal()) return null;
        BlockState state = super.getStateForPlacement(context);
        if (design == Design.JACKPOT) state = state.setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        Direction support = design == Design.JACKPOT ? state.getValue(FACING).getOpposite() : Direction.DOWN;
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
