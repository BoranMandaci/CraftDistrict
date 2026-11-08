package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Passive floor/countertop accessories on the existing solid-support placement rules. */
public final class PharmacyAccessoryBlock extends CashRegisterBlock {
    public enum Style implements StringRepresentable {
        PROMOTIONAL, MEDICINE_STACK, DELIVERY_CART;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Style style;
    //? if >=1.21 {
    public static final MapCodec<PharmacyAccessoryBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Style::values).fieldOf("style").forGetter((PharmacyAccessoryBlock block) -> block.style),
            propertiesCodec()).apply(instance, PharmacyAccessoryBlock::new));
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(1.5, 0, 2, 14.5, 1.25, 14),
                    box(2, 1.25, 2.5, 14, 11, 13), box(2.5, 11, 10.5, 13.5, 16, 13)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(2.25, 0, 2.5, 5, 6, 5),
                    box(1.5, 0, 8, 7.75, 4.5, 13), box(6, 0, 2.5, 13.75, 4.5, 11)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(1.25, 0, 2, 14.75, 3.875, 14.25),
                    box(1.75, 3.875, 2, 14.25, 14.25, 14.25),
                    box(2.375, 14.25, 13.375, 13.625, 15.625, 14.25)).optimize())};

    public PharmacyAccessoryBlock(Style style, Properties properties) { super(properties); this.style = style; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[style.ordinal()][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
