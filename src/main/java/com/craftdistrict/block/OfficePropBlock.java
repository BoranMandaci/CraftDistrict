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

/** Static floor/tabletop props share the existing solid-top placement rules. */
public final class OfficePropBlock extends CashRegisterBlock {
    public enum Design implements StringRepresentable {
        CUBICLE, SPEAKER, MAGAZINE_RACK, COFFEE_TABLE, PRINTER, PLANTER;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    //? if >=1.21 {
    public static final MapCodec<OfficePropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((OfficePropBlock b) -> b.design),
            propertiesCodec()).apply(instance, OfficePropBlock::new));
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 13.5, 1, 16, 14.5, 15.25),
                    box(0, 0, 4, 1, 13.5, 15.25), box(15, 0, 4, 16, 13.5, 15.25), box(0, 0, 15.25, 16, 22, 16))),
            RestaurantKitchenBlock.rotations(box(2.5, 0, 3.5, 13.5, 2, 12.5)),
            RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 3, 14, 1, 13), box(2, 1, 8, 14, 16, 12))),
            RestaurantKitchenBlock.rotations(Shapes.or(box(1, 8.5, 1, 15, 9.5, 15),
                    box(2.5, 0, 3, 4.25, 8.5, 13), box(12, 0, 3, 13.75, 8.5, 13))),
            RestaurantKitchenBlock.rotations(Shapes.or(box(.75, 0, 1.5, 15.125, 11, 14.5), box(1, 11, .5, 15, 15.5, 14.5))),
            RestaurantKitchenBlock.rotations(Shapes.or(box(3, 0, 3, 13, 9.25, 13), box(1, 9.25, 1, 15, 28, 15)))
    };
    private final Design design;
    public OfficePropBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
