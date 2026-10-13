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

/** Static single-cell signage: a wall behind the facing, or a floor below the A-frame. */
public class BakerySignageBlock extends RotatableDecorationBlock {
    //? if >=1.21 {
    public static final MapCodec<BakerySignageBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Variant::values).fieldOf("variant")
                    .forGetter((BakerySignageBlock block) -> block.variant),
            propertiesCodec()).apply(instance, BakerySignageBlock::new));
    //?}

    public enum Variant implements StringRepresentable {
        HANGING_SHOP("hanging_shop"), WALL_SHOP("wall_shop"),
        WALL_MENU("wall_menu"), DAILY_SPECIAL("daily_special"), SIDEWALK_MENU("sidewalk_menu");

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

    public BakerySignageBlock(Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    private Direction supportDirection(BlockState state) {
        return variant == Variant.SIDEWALK_MENU ? Direction.DOWN
                : state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = supportDirection(state);
        BlockPos supportPos = pos.relative(direction);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, direction.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state;
        if (variant == Variant.SIDEWALK_MENU) {
            state = super.getStateForPlacement(context);
        } else {
            // Click a vertical supporting face. Do not guess a different wall
            // from the player's look direction or mount to floors/ceilings.
            Direction face = context.getClickedFace();
            if (!face.getAxis().isHorizontal()) {
                return null;
            }
            state = defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, face);
        }
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == supportDirection(state) && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return BakerySignageShapes.SHAPES[variant.ordinal()][turn];
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
