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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two ordinary vertical cells using the shared validation and linked removal. */
public final class PoliceArmoryRackBlock extends PoliceEntranceBlock {
    //? if >=1.21 {
    public static final MapCodec<PoliceArmoryRackBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Rack::values).fieldOf("rack")
                    .forGetter((PoliceArmoryRackBlock block) -> block.rack),
            propertiesCodec()).apply(instance, PoliceArmoryRackBlock::new));
    //?}

    public enum Rack implements StringRepresentable {
        EQUIPMENT("equipment", 1), RIFLE("rifle", 3);

        private final String name;
        private final int shapeIndex;

        Rack(String name, int shapeIndex) {
            this.name = name;
            this.shapeIndex = shapeIndex;
        }

        @Override
        public String getSerializedName() { return name; }
    }

    private final Rack rack;

    public PoliceArmoryRackBlock(Rack rack, Properties properties) {
        super(Variant.RECEPTION_DESK, properties);
        this.rack = rack;
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Upper-cell obstruction is checked before the lower cell is written.
        return partIndex(state) == 1 || super.canSurvive(state, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PoliceArmoryShapes.get(rack.shapeIndex, partIndex(state), state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
