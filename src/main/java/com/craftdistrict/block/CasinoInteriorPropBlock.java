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

/** A seamless floor tile and a static tabletop device; neither has an inventory or ticker. */
public final class CasinoInteriorPropBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        CARPET, BILL_COUNTER;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final VoxelShape CARPET = box(0, 0, 0, 16, .125, 16);
    private static final VoxelShape[] MACHINE = RestaurantKitchenBlock.rotations(Shapes.or(
            box(3, 0, 3, 13, 5.75, 13.15), box(3.6, 5.75, 8.5, 12.4, 8.65, 13),
            box(3.5, .5, 1.75, 12.5, 1.5, 4)).optimize());
    //? if >=1.21 {
    public static final MapCodec<CasinoInteriorPropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((CasinoInteriorPropBlock b) -> b.design),
            propertiesCodec()).apply(instance, CasinoInteriorPropBlock::new));
    //?}

    public CasinoInteriorPropBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos below = pos.below();
        return level.hasChunkAt(below) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        return design == Design.CARPET && side.getAxis().isHorizontal() && neighbor.is(this)
                || super.skipRendering(state, neighbor, side);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return design == Design.CARPET ? CARPET : MACHINE[RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
