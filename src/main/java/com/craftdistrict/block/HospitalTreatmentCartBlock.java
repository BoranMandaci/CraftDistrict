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

/** Small floor-standing treatment carts with two cached envelopes per design. */
public final class HospitalTreatmentCartBlock extends CashRegisterBlock {
    public enum Design implements StringRepresentable {
        INSTRUMENTS, DEFIBRILLATOR, ANESTHESIA, TRAY;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<HospitalTreatmentCartBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HospitalTreatmentCartBlock b) -> b.design),
            propertiesCodec()).apply(instance, HospitalTreatmentCartBlock::new));
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 2, 14, 12, 14), box(3, 12, 5, 13, 15.5, 12)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(1.5, 0, 1.5, 14.5, 10, 14.5), box(2, 10, 4, 14, 16, 14)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(.75, 0, .75, 15.25, 10, 15), box(3, 10, 7, 12, 16, 12)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 2, 14, 1.5, 14), box(7.25, 1.5, 7.25, 8.75, 11.5, 8.75),
                    box(2, 11.5, 3, 14, 13.25, 13)).optimize())};
    public HospitalTreatmentCartBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
