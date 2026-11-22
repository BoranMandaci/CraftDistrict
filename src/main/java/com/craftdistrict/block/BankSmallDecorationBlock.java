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

public final class BankSmallDecorationBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        COIN_ROLL_TRAY, KEY_RACK;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    private static final VoxelShape[] TRAY = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1.18, 0, 2.05, 14.82, 1.27, 13.95),
            box(2.44, .65, 3.12, 13.32, 2.41, 7.45),
            box(2.45, .65, 8.39, 13.34, 2.37, 12.63)));
    private static final VoxelShape[] RACK = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1.42, 1.91, 14.33, 14.58, 14.86, 16),
            box(2.87, 8.52, 13.19, 12.89, 11.79, 14.42),
            box(2.87, 2.40, 13.19, 12.89, 5.79, 14.42)));
    private final Design design;

    public BankSmallDecorationBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (design == Design.KEY_RACK) {
            Direction face = context.getClickedFace();
            if (!face.getAxis().isHorizontal()) return null;
            state = state.setValue(FACING, face);
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = design == Design.KEY_RACK ? state.getValue(FACING) : Direction.UP;
        BlockPos support = pos.relative(face.getOpposite());
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (design == Design.KEY_RACK ? RACK : TRAY)[RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<BankSmallDecorationBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((BankSmallDecorationBlock block) -> block.design),
            propertiesCodec()).apply(instance, BankSmallDecorationBlock::new));
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
