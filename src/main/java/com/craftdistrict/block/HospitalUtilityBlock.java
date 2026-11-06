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

/** Static clinical props; thin decorative details share a few cached shape envelopes. */
public class HospitalUtilityBlock extends CashRegisterBlock {
    public enum Design implements StringRepresentable {
        CRASH_CART, HANDWASH, OVERBED, SANITIZER;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<HospitalUtilityBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HospitalUtilityBlock b) -> b.design),
            propertiesCodec()).apply(instance, HospitalUtilityBlock::new));
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(1.25, 0, 1, 14.75, 17.5, 15.75),
                    box(3, 17.5, 5, 13.5, 22.5, 13)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(.5, 0, 1, 15.5, 14.25, 16),
                    box(.5, 14.25, 15.5, 15.5, 25.25, 16), box(2, 14.25, 10, 14.5, 24, 15.5)).optimize()),
            // The cantilever is visual only outside this cell, leaving the occupied bed untouched.
            RestaurantKitchenBlock.rotations(Shapes.or(box(1.5, 0, 2, 14.5, 2.5, 15),
                    box(12, 2.5, 2.5, 14, 17, 4.5), box(1.75, 17, 0, 14.25, 17.75, 5.5)).optimize()),
            RestaurantKitchenBlock.rotations(box(5.125, 6, 12.75, 10.875, 13.625, 16))};

    public HospitalUtilityBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (design == Design.OVERBED) {
            BlockState state = super.getStateForPlacement(context);
            if (state == null) return null;
            Direction preferred = state.getValue(FACING);
            // Choose once at placement; no ticking or linked bed parts are needed.
            for (Direction side : new Direction[]{preferred, preferred.getClockWise(),
                    preferred.getCounterClockWise(), preferred.getOpposite()}) {
                if (context.getLevel().getBlockState(context.getClickedPos().relative(side)).getBlock() instanceof HospitalBedBlock) {
                    return state.setValue(FACING, side);
                }
            }
            return state;
        }
        if (design != Design.SANITIZER) return super.getStateForPlacement(context);
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) return null;
        BlockState state = defaultBlockState().setValue(FACING, face);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (design != Design.SANITIZER) return super.canSurvive(state, level, pos);
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction facing = state.getValue(FACING); BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (design != Design.SANITIZER) return super.updateShape(state, side, other, level, pos, otherPos);
        return side == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
