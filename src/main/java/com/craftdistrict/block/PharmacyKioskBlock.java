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

/** Static customer kiosks reuse the established paired floor-standing equipment lifecycle. */
public final class PharmacyKioskBlock extends HospitalEquipmentBlock {
    public enum Style implements StringRepresentable {
        QUEUE_TICKET, BLOOD_PRESSURE, INFORMATION_BOARD;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Style style;
    //? if >=1.21 {
    public static final MapCodec<PharmacyKioskBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Style::values).fieldOf("style").forGetter((PharmacyKioskBlock block) -> block.style),
            propertiesCodec()).apply(instance, PharmacyKioskBlock::new));
    //?}
    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 3, 14, 2, 14),
                    box(6.5, 2, 7.5, 9.5, 16, 10)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(6.5, 0, 7.5, 9.5, .72, 10),
                            box(3.5, .72, 5, 12.5, 9, 12.5), box(3, 9, 6, 13, 13.14, 9.25)).optimize())},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(1, 0, 2.5, 15, 1.5, 14.5),
                    box(2.25, 1.5, 4, 13.75, 15.25, 14), box(1, 15.25, 1.5, 15, 16, 14.5)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(3.875, 0, 5.75, 6.625, 1.73, 8.75),
                            box(2.55, 1.73, 5.25, 7.95, 6.77, 9.25), box(9.25, 0, 11, 10.75, 8, 12.25),
                            box(5, 7.25, 10.25, 14.5, 15.5, 12.5)).optimize())},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 4, 14, 1.25, 12),
                    box(7.25, 1.25, 7.25, 8.75, 11.75, 8.75),
                    box(1.75, 11.75, 7, 14.25, 16, 9)).optimize()),
                    RestaurantKitchenBlock.rotations(box(1.75, 0, 7, 14.25, 13.75, 9))}};

    public PharmacyKioskBlock(Style style, Properties properties) { super(Design.MONITOR, properties); this.style = style; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[style.ordinal()][state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
