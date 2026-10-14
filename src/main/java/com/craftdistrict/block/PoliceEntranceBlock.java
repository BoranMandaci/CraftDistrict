package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Public entrance furniture and wall plaque; no interactive behavior. */
public class PoliceEntranceBlock extends MultiBlockDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<PoliceEntranceBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Variant::values).fieldOf("variant")
                    .forGetter((PoliceEntranceBlock block) -> block.variant),
            propertiesCodec()).apply(instance, PoliceEntranceBlock::new));
    //?}

    public enum Variant implements StringRepresentable {
        RECEPTION_DESK("reception_desk"), DEPARTMENT_SIGN("department_sign"), WAITING_BENCH("waiting_bench");

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

    public PoliceEntranceBlock(Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise();
    }

    @Override
    protected Direction getPlacementFacing(BlockPlaceContext context) {
        if (variant != Variant.DEPARTMENT_SIGN) {
            return super.getPlacementFacing(context);
        }
        Direction face = context.getClickedFace();
        return face.getAxis().isHorizontal() ? face : null;
    }

    private Direction supportDirection(BlockState state) {
        return variant == Variant.DEPARTMENT_SIGN
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite() : Direction.DOWN;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = supportDirection(state);
        BlockPos support = pos.relative(direction);
        return level.getBlockState(support).isFaceSturdy(level, support, direction.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == supportDirection(state) && !state.canSurvive(level, pos)) {
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
        return PoliceEntranceShapes.SHAPES[variant.ordinal()][partIndex(state)][turn];
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
