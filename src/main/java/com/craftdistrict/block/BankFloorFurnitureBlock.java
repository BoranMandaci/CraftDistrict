package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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

import java.util.Locale;

public final class BankFloorFurnitureBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        CASH_TROLLEY, CONSULTATION_DESK;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    private static final VoxelShape DESKTOP = box(0, 14.38, 0, 16, 16, 16);
    private static final VoxelShape MODESTY_PANEL = box(0, 5.25, 2.34, 16, 14.38, 3.35);
    private static final VoxelShape[][] DESK = {
            RestaurantKitchenBlock.rotations(Shapes.or(DESKTOP, MODESTY_PANEL,
                    box(1.08, 0, 2.1, 3.05, .72, 13.95), box(1.38, .72, 2.45, 2.75, 14.38, 3.58),
                    box(1.38, .72, 12.48, 2.75, 14.38, 13.61))),
            RestaurantKitchenBlock.rotations(Shapes.or(DESKTOP, MODESTY_PANEL,
                    box(7.85, 0, 4.58, 14.85, 14.38, 14.55)))
    };
    private static final VoxelShape[][] TROLLEY = {
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(1.78, 0, 3.55, 3.96, 4.22, 7.47), box(12.04, 0, 3.55, 14.22, 4.22, 7.47),
                    box(1.08, 3.78, .78, 14.92, 5.28, 16), box(1.40, 4.02, .56, 14.60, 4.57, .78),
                    box(2.13, 5.28, 5.68, 13.87, 11.90, 15.52),
                    box(2.27, 5.28, 1.42, 3.67, 15.18, 2.84), box(12.33, 5.28, 1.42, 13.73, 15.18, 2.84),
                    box(2.27, 14.64, .54, 13.73, 15.96, 1.49))),
            RestaurantKitchenBlock.rotations(Shapes.or(
                    box(1.78, 0, 8.32, 3.96, 4.22, 12.24), box(12.04, 0, 8.32, 14.22, 4.22, 12.24),
                    box(1.08, 3.78, 0, 14.92, 5.28, 15.25), box(1.40, 4.02, 15.25, 14.60, 4.57, 15.57),
                    box(2.08, 5.28, .49, 13.92, 10.84, 13.81),
                    box(3.24, 10.84, 4.99, 12.76, 13.82, 12.13)))
    };
    private final Design design;

    public BankFloorFurnitureBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
    }

    @Override protected Direction getExtensionDirection(BlockState state) {
        Direction facing = state.getValue(FACING);
        return design == Design.CONSULTATION_DESK ? facing.getClockWise() : facing.getOpposite();
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = pos.below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction extension = getExtensionDirection(state);
        boolean primary = state.getValue(PRIMARY);
        if (side == (primary ? extension : extension.getOpposite()) && !isRemovingLinkedStructure(state, level, pos)
                && neighbor != stateForPart(state, primary ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return design == Design.CONSULTATION_DESK && mirror != Mirror.NONE
                ? mirrored.cycle(PRIMARY) : mirrored;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (design == Design.CONSULTATION_DESK ? DESK : TROLLEY)[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<BankFloorFurnitureBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((BankFloorFurnitureBlock block) -> block.design),
            propertiesCodec()).apply(instance, BankFloorFurnitureBlock::new));
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
