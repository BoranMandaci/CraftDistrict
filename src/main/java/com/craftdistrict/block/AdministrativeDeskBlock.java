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

/** Two-cell administrative desks reuse the established police workstation lifecycle. */
public final class AdministrativeDeskBlock extends PoliceWorkroomBlock {
    public enum Style implements StringRepresentable {
        CHIEF, INVESTIGATOR;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Style style;
    //? if >=1.21 {
    public static final MapCodec<AdministrativeDeskBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Style::values).fieldOf("style").forGetter((AdministrativeDeskBlock b) -> b.style),
            propertiesCodec()).apply(instance, AdministrativeDeskBlock::new));
    //?}
    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(Shapes.or(box(0, 13, .5, 16, 14, 15.5),
                    box(1, 0, 2, 9, 13, 14), box(2.75, 14, 1.75, 16, 18.125, 13.5)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(0, 13, .5, 16, 14, 15.5),
                            box(13.5, 0, 2, 15, 13, 14), box(0, 14, 3, 15.5, 23.25, 14.25)).optimize())},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(0, 13, .5, 16, 14, 15.5),
                    box(1, 0, 2, 3, 13, 14), box(1.25, 14, 2.75, 16, 23.25, 13.5)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(0, 13, .5, 16, 14, 15.5),
                            box(8, 0, 2, 15, 13, 14), box(0, 14, 2.5, 14.5, 18.25, 14.25)).optimize())}};

    public AdministrativeDeskBlock(Style style, Properties properties) { super(Variant.DISPATCH, properties); this.style = style; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[style.ordinal()][state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
