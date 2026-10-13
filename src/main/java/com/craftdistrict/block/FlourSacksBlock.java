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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Three static sack arrangements; all geometry is cached before any shape query. */
public final class FlourSacksBlock extends RotatableDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<FlourSacksBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Arrangement::values).fieldOf("arrangement")
                    .forGetter((FlourSacksBlock block) -> block.arrangement),
            propertiesCodec()).apply(instance, FlourSacksBlock::new));
    //?}

    public enum Arrangement implements StringRepresentable {
        SMALL("small"), STACKED("stacked"), PILE("pile");

        private final String name;

        Arrangement(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private final Arrangement arrangement;

    public FlourSacksBlock(Arrangement arrangement, Properties properties) {
        super(properties);
        this.arrangement = arrangement;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return FlourSackShapes.SHAPES[arrangement.ordinal()][turn];
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
