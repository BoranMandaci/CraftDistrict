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
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cart contents are visual only; empty and filled carts share the same envelopes. */
public final class SupermarketShoppingPropBlock extends RotatableDecorationBlock {
    public enum Prop implements StringRepresentable {
        CART, BASKETS;

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<SupermarketShoppingPropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Prop::values).fieldOf("prop")
                    .forGetter((SupermarketShoppingPropBlock block) -> block.prop),
            propertiesCodec()).apply(instance, SupermarketShoppingPropBlock::new));
    //?}

    private final Prop prop;

    public SupermarketShoppingPropBlock(Prop prop, Properties properties) {
        super(properties);
        this.prop = prop;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SupermarketShoppingShapes.get(prop == Prop.CART ? 1 : 2, 0, state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
