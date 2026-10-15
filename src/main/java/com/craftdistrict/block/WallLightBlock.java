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

/** Reuses clicked-wall orientation, sturdy-wall checks, and support-loss drops. */
public final class WallLightBlock extends BakerySignageBlock {
    public enum Fixture implements StringRepresentable {
        SCONCE(2), FLOODLIGHT(3), EXIT(4), NEON(LightingShapes.NEON), SECURITY(LightingShapes.SECURITY);

        private final int shape;

        Fixture(int shape) { this.shape = shape; }

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<WallLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Fixture::values).fieldOf("fixture")
                    .forGetter((WallLightBlock block) -> block.fixture),
            propertiesCodec()).apply(instance, WallLightBlock::new));
    //?}

    private final Fixture fixture;

    public WallLightBlock(Fixture fixture, Properties properties) {
        super(Variant.WALL_SHOP, properties);
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
