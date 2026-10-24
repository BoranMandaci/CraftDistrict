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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static appliances and accessories, including exact mounting on 14.5-unit worktops. */
public class RestaurantStoragePropBlock extends CashRegisterBlock {
    public enum Design implements StringRepresentable {
        DISHWASHER, MICROWAVE, CUTTING_BOARD, TRASH_BIN, DRYING_RACK;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
        boolean countertop() { return this == MICROWAVE || this == CUTTING_BOARD || this == DRYING_RACK; }
    }
    public static final BooleanProperty LOW_COUNTER = BooleanProperty.create("low_counter");
    private static final VoxelShape SUPPORT_FOOTPRINT = Block.box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape[][][] SHAPES = shapes();
    //? if >=1.21 {
    public static final MapCodec<RestaurantStoragePropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design")
                    .forGetter((RestaurantStoragePropBlock block) -> block.design),
            propertiesCodec()).apply(instance, RestaurantStoragePropBlock::new));
    //?}
    private final Design design;

    public RestaurantStoragePropBlock(Design design, Properties properties) {
        super(properties); this.design = design;
        registerDefaultState(defaultBlockState().setValue(LOW_COUNTER, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LOW_COUNTER);
    }

    private static VoxelShape[][][] shapes() {
        VoxelShape[] north = {
                Block.box(0, 0, 0, 16, 14.5, 16),
                Block.box(1, 0, 2, 15, 9, 14),
                Block.box(1.5, 0, 2, 14.5, 2.75, 14),
                Shapes.or(Block.box(1.75, 0, 2.75, 14.25, 13.5, 14.5), Block.box(6, 0, .75, 10, 1.5, 2.75)),
                Shapes.or(Block.box(1, 0, 1, 15, 2, 14), Block.box(2, 2, 3, 14, 8, 13))
        };
        VoxelShape[][][] result = new VoxelShape[north.length][2][];
        for (int i = 0; i < north.length; i++) {
            result[i][0] = RestaurantKitchenBlock.rotations(north[i]);
            result[i][1] = RestaurantKitchenBlock.rotations(north[i].move(0, -1.5 / 16, 0));
        }
        return result;
    }

    private boolean lowCounter(LevelReader level, BlockPos pos) {
        if (!design.countertop()) return false;
        BlockPos below = pos.below();
        VoxelShape top = level.getBlockState(below).getCollisionShape(level, below);
        return !top.isEmpty() && Math.abs(top.max(Direction.Axis.Y) - 14.5 / 16) < 1.0E-7
                && !Shapes.joinIsNotEmpty(SUPPORT_FOOTPRINT,
                        top.move(0, 1.5 / 16, 0).getFaceShape(Direction.UP), BooleanOp.ONLY_FIRST);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(LOW_COUNTER) ? lowCounter(level, pos) : super.canSurvive(state, level, pos);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LOW_COUNTER, lowCounter(context.getLevel(), context.getClickedPos()));
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (side != Direction.DOWN) return state;
        BlockState mounted = state.setValue(LOW_COUNTER, lowCounter(level, pos));
        return mounted.canSurvive(level, pos) ? mounted : Blocks.AIR.defaultBlockState();
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][design.countertop() && state.getValue(LOW_COUNTER) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
