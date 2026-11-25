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
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/** Static, single-block gym props with collision cached for each facing. */
public final class GymFloorPropBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        FLOOR_DUMBBELLS(GymEquipmentShapes.FLOOR_DUMBBELLS),
        ROLLED_YOGA_MAT_STACK(GymEquipmentShapes.ROLLED_YOGA_MAT_STACK),
        EXERCISE_BALL(GymEquipmentShapes.EXERCISE_BALL);

        private final VoxelShape[] shapes;

        Design(VoxelShape[] shapes) {
            this.shapes = shapes;
        }

        @Override public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final Design design;

    //? if >=1.21 {
    public static final MapCodec<GymFloorPropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((GymFloorPropBlock block) -> block.design),
            propertiesCodec()).apply(instance, GymFloorPropBlock::new));

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}

    public GymFloorPropBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return DecorationSupport.isPlaced(state, level, pos)
                || GymEquipmentBlock.supported(level, pos.below(), Direction.UP);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return design.shapes[RestaurantKitchenBlock.turn(state)];
    }
}
