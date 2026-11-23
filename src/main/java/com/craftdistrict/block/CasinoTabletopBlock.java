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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

public final class CasinoTabletopBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        SCATTERED_CHIPS(Shapes.or(
                box(3.40, 0, 4.25, 8.00, .278, 6.68), box(9.43, 0, 4.25, 11.84, .278, 6.70),
                box(10.94, 0, 6.50, 13.08, .278, 8.64), box(3.57, 0, 6.54, 8.80, .278, 9.10),
                box(3.61, 0, 9.34, 9.19, .278, 11.64), box(9.43, 0, 8.22, 11.73, .278, 10.52),
                box(6.96, .278, 5.38, 9.24, .556, 7.66), box(10.30, .278, 8.91, 12.56, .556, 11.17),
                box(4.75, .278, 8.01, 7.11, .556, 10.36))),
        CHIP_PILE(Shapes.or(
                box(4.14, 0, 4.10, 11.36, .295, 6.96), box(3.81, 0, 6.60, 12.72, .295, 10.08),
                box(4.44, 0, 9.28, 11.70, .295, 11.78),
                box(5.08, .295, 5.14, 10.67, .59, 7.57), box(5.46, .295, 7.87, 12.25, .59, 10.45),
                box(6.94, .59, 5.23, 9.26, .885, 7.55), box(6.92, .59, 7.67, 11.48, .885, 10.69),
                box(7.84, .885, 6.62, 10.16, 1.18, 8.96))),
        PLAQUES(Shapes.or(
                box(1.85, 0, 3.07, 8.40, .365, 8.07), box(7.45, 0, 7.88, 14.00, .365, 12.88),
                box(3.17, .365, 4.79, 8.97, .73, 7.79), box(8.08, .365, 8.38, 13.88, .73, 11.38),
                box(2.33, .73, 3.27, 8.85, 1.095, 8.27), box(6.88, .73, 8.21, 13.40, 1.095, 13.21))),
        CARD_DECK(box(6.44, 0, 5.81, 9.56, .80, 10.19)),
        FANNED_CARDS(box(3.90, 0, 4.55, 12.10, .30, 9.83)),
        FACE_UP_SPREAD(Shapes.or(box(2.25, 0, 4.82, 6.3, .075, 9.88),
                box(6.61, 0, 6.04, 9.39, .075, 10.06), box(9.62, 0, 5.33, 13.73, .075, 10.33))),
        ROYAL_FLUSH(box(.93, 0, 5.99, 15.07, .075, 10.01)),
        DISCARD_PILE(box(5.03, 0, 5.15, 11.18, .30, 11.67));

        private final VoxelShape[] shapes;

        Design(VoxelShape shape) {
            this.shapes = RestaurantKitchenBlock.rotations(shape);
        }

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    private final Design design;

    public CasinoTabletopBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = pos.below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return design.shapes[RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<CasinoTabletopBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((CasinoTabletopBlock block) -> block.design),
            propertiesCodec()).apply(instance, CasinoTabletopBlock::new));
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
