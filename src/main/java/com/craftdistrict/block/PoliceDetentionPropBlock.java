package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floor-supported toilet/sink and a freely tileable asymmetric glass panel. */
public final class PoliceDetentionPropBlock extends CashRegisterBlock {
    //? if >=1.21 {
    public static final MapCodec<PoliceDetentionPropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Prop::values).fieldOf("prop")
                    .forGetter((PoliceDetentionPropBlock block) -> block.prop),
            propertiesCodec()).apply(instance, PoliceDetentionPropBlock::new));
    //?}

    public enum Prop implements StringRepresentable {
        TOILET_SINK("toilet_sink", 1), GLASS("glass", 2);

        private final String name;
        private final int shapeIndex;

        Prop(String name, int shapeIndex) {
            this.name = name;
            this.shapeIndex = shapeIndex;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private final Prop prop;

    public PoliceDetentionPropBlock(Prop prop, Properties properties) {
        super(properties);
        this.prop = prop;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return prop == Prop.GLASS || super.canSurvive(state, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PoliceDetentionShapes.get(prop.shapeIndex, 0, state);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        // Remove only coplanar glass edge faces, retaining the distinct front/rear.
        if (prop == Prop.GLASS && adjacent.is(this)
                && adjacent.getValue(BlockStateProperties.HORIZONTAL_FACING) == state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                && direction.getAxis() != state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()) {
            return true;
        }
        return super.skipRendering(state, adjacent, direction);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
