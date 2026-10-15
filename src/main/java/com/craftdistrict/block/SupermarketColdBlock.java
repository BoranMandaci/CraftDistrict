package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static sealed units reuse the existing two-cell placement and removal system. */
public final class SupermarketColdBlock extends MultiBlockDecorationBlock {
    public enum Layout implements StringRepresentable {
        DRINKS, CHEST, DELI;

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<SupermarketColdBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Layout::values).fieldOf("layout")
                    .forGetter((SupermarketColdBlock block) -> block.layout),
            propertiesCodec()).apply(instance, SupermarketColdBlock::new));
    //?}

    private final Layout layout;

    public SupermarketColdBlock(Layout layout, Properties properties) {
        super(properties);
        this.layout = layout;
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return layout == Layout.DRINKS ? Direction.UP : state.getValue(FACING).getClockWise();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SupermarketRefrigerationShapes.get(layout.ordinal(), partIndex(state), state);
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
