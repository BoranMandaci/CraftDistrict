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

/** Shared two-part placement/removal for bed, wall chart and tall booking camera. */
public final class PoliceDetentionStructureBlock extends PoliceEntranceBlock {
    //? if >=1.21 {
    public static final MapCodec<PoliceDetentionStructureBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Structure::values).fieldOf("structure")
                    .forGetter((PoliceDetentionStructureBlock block) -> block.structure),
            propertiesCodec()).apply(instance, PoliceDetentionStructureBlock::new));
    //?}

    public enum Structure implements StringRepresentable {
        BED("bed", 0), CHART("chart", 3), CAMERA("camera", 4);

        private final String name;
        private final int shapeIndex;

        Structure(String name, int shapeIndex) {
            this.name = name;
            this.shapeIndex = shapeIndex;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private final Structure structure;

    public PoliceDetentionStructureBlock(Structure structure, Properties properties) {
        super(structure == Structure.CHART ? Variant.DEPARTMENT_SIGN : Variant.RECEPTION_DESK, properties);
        this.structure = structure;
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return structure == Structure.BED ? super.getExtensionDirection(state) : Direction.UP;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // The upper camera cell is validated before the lower cell is written.
        // Only the lower part needs floor support; shared cleanup links both.
        return structure == Structure.CAMERA && partIndex(state) == 1
                || super.canSurvive(state, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PoliceDetentionShapes.get(structure.shapeIndex, partIndex(state), state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
