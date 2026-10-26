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

/** Simple clicked-wall placement and support-loss rules; no clock or cabinet logic. */
public class FireWallFixtureBlock extends BakerySignageBlock {
    public enum Fixture implements StringRepresentable {
        CLOCK("clock",0), FIRST_AID("first_aid",3);

        private final String name;
        private final int shapeIndex;

        Fixture(String name, int shapeIndex) { this.name=name; this.shapeIndex=shapeIndex; }

        @Override
        public String getSerializedName() { return name; }
    }

    //? if >=1.21 {
    public static final MapCodec<FireWallFixtureBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Fixture::values).fieldOf("fixture")
                    .forGetter((FireWallFixtureBlock block) -> block.fixture),
            propertiesCodec()).apply(instance, FireWallFixtureBlock::new));
    //?}

    private final Fixture fixture;

    public FireWallFixtureBlock(Fixture fixture, Properties properties) {
        super(Variant.WALL_MENU, properties);
        this.fixture=fixture;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FireFixtureShapes.get(fixture.shapeIndex,0,state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
