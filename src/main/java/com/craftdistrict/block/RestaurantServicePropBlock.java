package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static floor/countertop service props share the existing solid-top lifecycle. */
public final class RestaurantServicePropBlock extends CashRegisterBlock {
    public enum Design implements StringRepresentable {
        PICKUP_COUNTER, POS_TERMINAL, BEVERAGE_DISPENSER, CONDIMENT_STATION, COFFEE_MACHINE, MENU_BOARD;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    //? if >=1.21 {
    public static final MapCodec<RestaurantServicePropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((RestaurantServicePropBlock block) -> block.design),
            propertiesCodec()).apply(instance, RestaurantServicePropBlock::new));
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(0, 14.5, 0, 16, 16, 16), Block.box(0, 0, 1, 16, 6.5, 16),
                    Block.box(0, 6.5, 1, 1, 14.5, 16), Block.box(15, 6.5, 1, 16, 14.5, 16),
                    Block.box(7.5, 6.5, 1, 8.5, 14.5, 16), Block.box(1, 6.5, 14, 15, 14.5, 16))),
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(2, 0, 2, 14, 1.25, 13), Block.box(6.5, 1.25, 8.75, 9.5, 4.5, 10.25),
                    Block.box(2.5, 4.5, 8, 12.5, 11.5, 12), Block.box(9.5, 1.25, 2.75, 13.5, 4, 7.5))),
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(1, 0, 2, 15, 1.75, 14), Block.box(1.75, 1.75, 8.5, 14.25, 15.75, 13),
                    Block.box(2.75, 6, 5.5, 13.25, 10, 8.5))),
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(1, 0, 1.5, 15, 1, 14), Block.box(2, 1, 3, 10.75, 9.75, 7.5),
                    Block.box(11.5, 1, 3, 14, 8, 7), Block.box(2, 1, 9, 14, 7.5, 13))),
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(1, 0, 2, 15, 1.75, 14), Block.box(1.5, 1.75, 10.5, 14.5, 11.75, 14),
                    Block.box(1, 7.5, 5, 15, 13, 14), Block.box(3, 4.5, 2.75, 11.75, 7.5, 8.5))),
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(2, 0, 2, 14, 1.5, 14),
                    Block.box(6.75, 1.5, 9, 9.25, 6, 11), Block.box(1, 5, 8, 15, 19, 10.5)))
    };
    private final Design design;

    public RestaurantServicePropBlock(Design design, Properties properties) { super(properties); this.design = design; }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
