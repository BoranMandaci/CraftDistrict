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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/** Local left and right joins for teller counters and their security screens. */
public final class BankLineBlock extends CashRegisterBlock {
    public enum Design implements StringRepresentable {
        COUNTER, SECURITY_GLASS;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    private static final VoxelShape[][] COUNTER = joinedShapes(Shapes.or(
                    box(0, 0, 2.55, 16, 1.2, 13.95),
                    box(0, 1.2, 1.6, 16, 13.15, 9.2),
                    box(0, 1.92, 1.17, 16, 12.81, 1.6),
                    box(0, 1.2, 9.2, 16, 7.15, 13.42),
                    box(0, 7.15, 9.2, 16, 7.79, 14.23),
                    box(7.76, 7.79, 9.2, 8.24, 13.15, 13.94),
                    box(0, 13.15, 1.36, 16, 14.5, 14.6),
                    box(0, 14.5, 0, 16, 16, 16)),
            box(0, 7.79, 9.2, .57, 13.15, 14.23), box(15.43, 7.79, 9.2, 16, 13.15, 14.23));
    private static final VoxelShape[][] GLASS = joinedShapes(Shapes.or(
                    box(0, 0, 6.1, 5, 4.4, 7.65), box(11, 0, 6.1, 16, 4.4, 7.65),
                    box(0, 3.9, 6.4, 16, 4.4, 7.35), box(0, 4.4, 6.7, 16, 14.9, 7.05),
                    box(0, 14.9, 6.05, 16, 16, 7.65)),
            box(0, .65, 6.08, .7, 14.9, 7.64), box(15.3, .65, 6.08, 16, 14.9, 7.64));
    private final Design design;

    public BankLineBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }

    private static VoxelShape[][] joinedShapes(VoxelShape body, VoxelShape left, VoxelShape right) {
        return new VoxelShape[][] {
                RestaurantKitchenBlock.rotations(Shapes.or(body, left, right)),
                RestaurantKitchenBlock.rotations(Shapes.or(body, right)),
                RestaurantKitchenBlock.rotations(Shapes.or(body, left)),
                RestaurantKitchenBlock.rotations(body)
        };
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEFT, RIGHT);
    }

    private boolean connects(BlockState neighbor, Direction facing) {
        return neighbor.is(this) && neighbor.getValue(FACING) == facing;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        var level = context.getLevel();
        BlockState clicked = level.getBlockState(pos.relative(face.getOpposite()));
        if (face.getAxis().isHorizontal() && clicked.is(this)
                && face.getAxis() != clicked.getValue(FACING).getAxis()) {
            state = state.setValue(FACING, clicked.getValue(FACING));
        }
        if (design == Design.SECURITY_GLASS) {
            BlockState below = level.getBlockState(pos.below());
            if (below.getBlock() instanceof BankLineBlock counter && counter.design == Design.COUNTER) {
                state = state.setValue(FACING, below.getValue(FACING));
            }
        }
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, connects(level.getBlockState(pos.relative(facing.getCounterClockWise())), facing))
                .setValue(RIGHT, connects(level.getBlockState(pos.relative(facing.getClockWise())), facing));
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (side == facing.getCounterClockWise()) return state.setValue(LEFT, connects(neighbor, facing));
        if (side == facing.getClockWise()) return state.setValue(RIGHT, connects(neighbor, facing));
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int ends = (state.getValue(LEFT) ? 1 : 0) | (state.getValue(RIGHT) ? 2 : 0);
        return (design == Design.COUNTER ? COUNTER : GLASS)[ends][RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    public static final MapCodec<BankLineBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((BankLineBlock block) -> block.design),
            propertiesCodec()).apply(instance, BankLineBlock::new));
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
