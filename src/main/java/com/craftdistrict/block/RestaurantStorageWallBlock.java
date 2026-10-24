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

/** Reuses wall-facing placement and support-loss behavior without polling. */
public final class RestaurantStorageWallBlock extends BakerySignageBlock {
    public enum Design implements StringRepresentable {
        COOKWARE, KNIVES, SPICES;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    //? if >=1.21 {
    public static final MapCodec<RestaurantStorageWallBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design")
                    .forGetter((RestaurantStorageWallBlock block) -> block.design),
            propertiesCodec()).apply(instance, RestaurantStorageWallBlock::new));
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(.5, 12, 14, 15.5, 15, 16),
                    Block.box(.25, 1, 9.5, 15.75, 13, 14))),
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(1, 7.5, 14.25, 15, 10.5, 16),
                    Block.box(2, 2, 13.25, 14, 14, 14.25))),
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(.5, 1, 14.75, 15.5, 15, 16),
                    Block.box(.5, 1, 9, 15.5, 14, 14.75)))
    };
    private final Design design;

    public RestaurantStorageWallBlock(Design design, Properties properties) {
        super(Variant.WALL_SHOP, properties); this.design = design;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
