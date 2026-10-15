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

/** Two occupied cells; existing placement and linked removal own the whole structure. */
public final class SupermarketDecorationBlock extends MultiBlockDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<SupermarketDecorationBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Layout::values).fieldOf("layout")
                    .forGetter((SupermarketDecorationBlock block) -> block.layout),
            propertiesCodec()).apply(instance, SupermarketDecorationBlock::new));
    //?}

    public enum Layout implements StringRepresentable {
        AISLE, PROMOTION, PRODUCE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private final Layout layout;

    public SupermarketDecorationBlock(Layout layout, Properties properties) {
        super(properties);
        this.layout = layout;
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return layout == Layout.PRODUCE ? state.getValue(FACING).getClockWise() : Direction.UP;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SupermarketShapes.get(layout.ordinal(), partIndex(state), state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        // Only flush rear faces join; different shelf contents may share the spine.
        // No extra blockstates or runtime shape/model construction are needed.
        if (layout == Layout.AISLE && direction == state.getValue(FACING).getOpposite()
                && adjacentState.getBlock() instanceof SupermarketDecorationBlock other
                && other.layout == Layout.AISLE
                && adjacentState.getValue(FACING) == direction
                && adjacentState.getValue(PRIMARY).equals(state.getValue(PRIMARY))) {
            return true;
        }
        return super.skipRendering(state, adjacentState, direction);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
