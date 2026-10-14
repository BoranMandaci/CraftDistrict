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

/** Existing sturdy-top/wall mounting rules with static equipment silhouettes. */
public final class PoliceArmoryPropBlock extends BakerySignageBlock {
    //? if >=1.21 {
    public static final MapCodec<PoliceArmoryPropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Prop::values).fieldOf("prop")
                    .forGetter((PoliceArmoryPropBlock block) -> block.prop),
            propertiesCodec()).apply(instance, PoliceArmoryPropBlock::new));
    //?}

    public enum Prop implements StringRepresentable {
        RADIO("radio", 0), PISTOL("pistol", 2);

        private final String name;
        private final int shapeIndex;

        Prop(String name, int shapeIndex) {
            this.name = name;
            this.shapeIndex = shapeIndex;
        }

        @Override
        public String getSerializedName() { return name; }
    }

    private final Prop prop;

    public PoliceArmoryPropBlock(Prop prop, Properties properties) {
        super(prop == Prop.PISTOL ? Variant.WALL_SHOP : Variant.SIDEWALK_MENU, properties);
        this.prop = prop;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PoliceArmoryShapes.get(prop.shapeIndex, 0, state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
