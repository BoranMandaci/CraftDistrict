package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static dispatch/evidence furniture, using the established two-part lifecycle. */
public class PoliceWorkroomBlock extends MultiBlockDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<PoliceWorkroomBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Variant::values).fieldOf("variant")
                    .forGetter((PoliceWorkroomBlock block) -> block.variant),
            propertiesCodec()).apply(instance, PoliceWorkroomBlock::new));
    //?}

    public enum Variant implements StringRepresentable {
        DISPATCH("dispatch"), EVIDENCE("evidence");

        private final String name;

        Variant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private final Variant variant;

    public PoliceWorkroomBlock(Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return variant == Variant.EVIDENCE ? Direction.UP
                : state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Upper shelf placement is checked before its lower partner is written;
        // the shared lifecycle handles matching-part removal after placement.
        if (variant == Variant.EVIDENCE && !state.getValue(PRIMARY)) {
            return true;
        }
        BlockPos support = pos.below();
        return level.getBlockState(support).isFaceSturdy(level, support, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return PoliceWorkroomShapes.SHAPES[variant.ordinal()][partIndex(state)][turn];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
