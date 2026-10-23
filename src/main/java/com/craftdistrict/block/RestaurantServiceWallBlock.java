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

/** Wall-mounted service accessories use the established mounting-face and support rules. */
public final class RestaurantServiceWallBlock extends BakerySignageBlock {
    public enum Design implements StringRepresentable {
        PLATE_CUTLERY_RACK;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    //? if >=1.21 {
    public static final MapCodec<RestaurantServiceWallBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((RestaurantServiceWallBlock block) -> block.design),
            propertiesCodec()).apply(instance, RestaurantServiceWallBlock::new));
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(.5, 2, 14.5, 15.5, 15, 16),
                    Block.box(.5, 1, 7.5, 15.5, 4, 14.5), Block.box(.5, 4, 9, 10.5, 11, 14.5)))
    };
    private final Design design;

    public RestaurantServiceWallBlock(Design design, Properties properties) {
        super(Variant.WALL_SHOP, properties);
        this.design = design;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
