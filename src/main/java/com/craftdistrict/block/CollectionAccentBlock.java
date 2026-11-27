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

public final class CollectionAccentBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        NOTICE_BOARD, POST_IT_NOTES, FIRE_HYDRANT, BILLIARD_CUE_RACK, DART_BOARD,
        CHESS_BOARD, CHECKERS_BOARD, BACKGAMMON_BOARD, JENGA_TOWER, FOUR_IN_A_ROW, DOMINO_SET;

        boolean wallMounted() {
            return switch (this) {
                case NOTICE_BOARD, POST_IT_NOTES, BILLIARD_CUE_RACK, DART_BOARD -> true;
                case FIRE_HYDRANT, CHESS_BOARD, CHECKERS_BOARD, BACKGAMMON_BOARD,
                        JENGA_TOWER, FOUR_IN_A_ROW, DOMINO_SET -> false;
            };
        }

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(.75, 3.1, 14.73, 15.25, 13.6, 16)),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(4.15, 8.15, 15.51, 7.6, 11.55, 16), box(8.05, 8.3, 15.49, 11.55, 11.70, 16),
                    box(5.05, 4.55, 15.44, 8.35, 7.81, 16), box(8.95, 4.82, 15.48, 12.15, 8.02, 16))),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(4.55, 0, 4.55, 11.45, 1.1, 11.45), box(5.62, 1.1, 5.62, 10.38, 11.25, 10.38),
                    box(4.62, 11.25, 4.62, 11.38, 12.4, 11.38), box(5.14, 12.4, 5.14, 10.86, 14.62, 10.86),
                    box(6.93, 14.62, 6.93, 9.07, 15.87, 9.07),
                    box(2.85, 7.73, 6.58, 13.15, 10.57, 9.42), box(6.28, 5.82, 2.52, 9.72, 9.38, 5.62))),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(1.55, 1.88, 13.12, 14.45, 3.14, 16), box(1.55, 12.32, 13.22, 14.45, 15.65, 16),
                    box(2.15, 3.14, 14.52, 13.85, 12.32, 16), box(3.28, 3.14, 13.54, 12.72, 12.32, 14.52),
                    box(5.38, 5.55, 12.97, 10.62, 10.65, 13.54))),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(2.5, 2.50, 15.36, 13.5, 13.50, 16),
                    box(3.26, 4.16, 14.02, 12.74, 11.84, 15.36), box(4.45, 3.26, 14.02, 11.55, 12.74, 15.36),
                    box(6.57, 6.5, 11.9, 9.96, 10.45, 14.02))),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(1.12, 0, 1.12, 14.88, .90, 14.88),
                    box(3.80, .90, 2.32, 13.68, 2.82, 3.32), box(2.32, .90, 12.68, 13.68, 2.82, 13.68),
                    box(2.40, .90, 3.88, 13.60, 1.84, 4.72), box(2.40, .90, 11.28, 12.12, 1.84, 12.12),
                    box(5.28, .90, 5.28, 6.28, 2.38, 6.28), box(6.84, .90, 6.84, 7.68, 1.84, 9.16),
                    box(12.68, .90, 6.76, 13.68, 2.54, 7.76), box(5.28, .90, 9.72, 6.28, 2.38, 10.72),
                    box(9.72, .90, 9.72, 10.72, 2.38, 10.72), box(12.76, .90, 9.80, 13.60, 1.84, 10.64),
                    box(6.76, .90, 11.20, 7.76, 2.54, 12.20))),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(1.14, 0, 1.14, 14.86, .76, 14.86),
                    box(2.24, .76, 2.24, 13.76, 1.06, 13.76), box(5.24, 1.06, 6.72, 6.32, 1.36, 7.80),
                    box(9.68, 1.06, 8.20, 10.76, 1.36, 9.28))),
            RestaurantKitchenBlock.rotations(box(1.06, 0, 1.90, 14.94, 1.38, 14.10)),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(5.30, 0, 5.30, 10.70, 14.04, 10.70),
                    box(10.70, 3.24, 7.14, 11.50, 4.32, 8.86), box(4.24, 7.56, 5.30, 5.30, 8.64, 7.02),
                    box(8.98, 10.80, 10.70, 10.70, 11.88, 11.36))),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(2.60, 0, 4.84, 4.62, .93, 11.16), box(11.38, 0, 4.84, 13.40, .93, 11.16),
                    box(2.68, .93, 6.68, 4.54, 1.80, 9.32), box(11.46, .93, 6.68, 13.32, 1.80, 9.32),
                    box(2.45, 1.80, 7.06, 13.55, 11.58, 8.94), box(7.27, 1.885, 6.94, 8.73, 2.083, 7.08))),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(2.35, 0, 4.07, 3.65, .35, 9.61), box(3.14, 0, 2.60, 11.58, .35, 3.90),
                    box(11.82, 0, 3.40, 13.12, .35, 8.93), box(5.25, 0, 9.38, 7.99, .35, 11.70),
                    box(8.96, 0, 10.12, 11.28, .70, 12.86)))
    };
    private final Design design;

    public CollectionAccentBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (design.wallMounted()) {
            Direction face = context.getClickedFace();
            if (!face.getAxis().isHorizontal()) return null;
            state = state.setValue(FACING, face);
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = design.wallMounted() ? state.getValue(FACING) : Direction.UP;
        BlockPos support = pos.relative(face.getOpposite());
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(design, state);
    }

    static VoxelShape shapeFor(Design design, BlockState state) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<CollectionAccentBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((CollectionAccentBlock block) -> block.design),
            propertiesCodec()).apply(instance, CollectionAccentBlock::new));
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
