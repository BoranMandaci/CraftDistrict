package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tileable bars and solid locker banks; no connections or inventory. */
public final class PoliceSecurityPropBlock extends CashRegisterBlock {
    private static final VoxelShape BAR_NORTH = Block.box(0, 0, 13, 16, 16, 16);
    private static final VoxelShape BAR_EAST = Block.box(0, 0, 0, 3, 16, 16);
    private static final VoxelShape BAR_SOUTH = Block.box(0, 0, 0, 16, 16, 3);
    private static final VoxelShape BAR_WEST = Block.box(13, 0, 0, 16, 16, 16);
    //? if >=1.21 {
    public static final MapCodec<PoliceSecurityPropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Variant::values).fieldOf("variant")
                    .forGetter((PoliceSecurityPropBlock block) -> block.variant),
            propertiesCodec()).apply(instance, PoliceSecurityPropBlock::new));
    //?}

    public enum Variant implements StringRepresentable {
        BARS("bars", 4), LOCKERS("lockers", 7);

        private final String name;
        private final int shapeIndex;

        Variant(String name, int shapeIndex) {
            this.name = name;
            this.shapeIndex = shapeIndex;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private final Variant variant;

    public PoliceSecurityPropBlock(Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Both decorations tile horizontally/vertically like ordinary blocks.
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        // Match vanilla door facing so adjacent panels land on the same edge
        // when both are placed while looking in the same direction.
        return state != null && variant == Variant.BARS
                ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection()) : state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (variant == Variant.LOCKERS) {
            return Shapes.block();
        }
        // Bridge the visible bar gaps only within the thin frame's edge plane.
        return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> BAR_EAST;
            case SOUTH -> BAR_SOUTH;
            case WEST -> BAR_WEST;
            default -> BAR_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return variant == Variant.LOCKERS ? Shapes.block()
                : PolicePackage2Shapes.get(variant.shapeIndex, 0, state);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
