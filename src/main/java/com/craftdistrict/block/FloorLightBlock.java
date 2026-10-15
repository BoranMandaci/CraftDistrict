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

/** Uses the existing solid-top support, support-loss, collision and rotation rules. */
public final class FloorLightBlock extends CashRegisterBlock {
    public enum Fixture implements StringRepresentable {
        DESK(LightingShapes.DESK), BOLLARD(LightingShapes.BOLLARD);

        private final int shape;

        Fixture(int shape) { this.shape = shape; }

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<FloorLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Fixture::values).fieldOf("fixture")
                    .forGetter((FloorLightBlock block) -> block.fixture),
            propertiesCodec()).apply(instance, FloorLightBlock::new));
    //?}

    private final Fixture fixture;

    public FloorLightBlock(Fixture fixture, Properties properties) {
        super(properties);
        this.fixture = fixture;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LightingShapes.get(fixture.shape, state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
