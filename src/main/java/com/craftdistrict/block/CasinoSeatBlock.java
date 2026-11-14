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

/** Reuses the established interaction, temporary seat entity and removal/dismount cleanup. */
public final class CasinoSeatBlock extends RestaurantSeatBlock {
    public enum Style implements StringRepresentable {
        GAMING_CHAIR, DEALER_STOOL;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Style style;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(3.35, 0, 3.6, 4.65, 8.4, 4.9),
                    box(11.35, 0, 3.6, 12.65, 8.4, 4.9), box(3.35, 0, 11.1, 4.65, 8.4, 12.4),
                    box(11.35, 0, 11.1, 12.65, 8.4, 12.4), box(2.75, 7.4, 2.5, 13.25, 9.75, 13),
                    box(2.9, 9.75, 11.35, 13.1, 23, 13.3),
                    box(2.3, 9.75, 4.25, 3.55, 13.2, 12.4), box(12.45, 9.75, 4.25, 13.7, 13.2, 12.4)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(3.15, 0, 7.15, 12.85, .9, 8.85),
                    box(7.15, 0, 3.15, 8.85, .9, 12.85), box(6.85, .9, 6.85, 9.15, 2.4, 9.15),
                    box(7.4, 2.4, 7.4, 8.6, 11.8, 8.6), box(4.6, 6.15, 4.6, 11.4, 6.6, 11.4),
                    box(3.75, 11.8, 3.75, 12.25, 13.5, 12.25), box(4.5, 13.5, 11.1, 11.5, 20, 12.65)).optimize())};
    //? if >=1.21 {
    public static final MapCodec<CasinoSeatBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Style::values).fieldOf("style").forGetter((CasinoSeatBlock b) -> b.style),
            propertiesCodec()).apply(instance, CasinoSeatBlock::new));
    //?}

    public CasinoSeatBlock(Style style, Properties properties) { super(Design.CUSHIONED, properties); this.style = style; }
    @Override public double seatHeight() { return (style == Style.GAMING_CHAIR ? 9.75 : 13.5) / 16; }
    @Override public double forwardOffset() { return 1.0 / 16; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[style.ordinal()][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
