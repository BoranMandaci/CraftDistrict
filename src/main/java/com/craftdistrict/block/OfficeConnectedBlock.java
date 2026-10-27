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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Same-facing row joins inspect only the two adjoining cells; no ticking or traversal. */
public final class OfficeConnectedBlock extends CashRegisterBlock {
    public enum Design implements StringRepresentable {
        CONFERENCE, DIVIDER;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    //? if >=1.21 {
    public static final MapCodec<OfficeConnectedBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((OfficeConnectedBlock b) -> b.design),
            propertiesCodec()).apply(instance, OfficeConnectedBlock::new));
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 13.5, 0, 16, 14.5, 16),
                    box(6.5, 1, 6.5, 9.5, 13.5, 9.5), box(4, 0, 4, 12, 1, 12))),
            RestaurantKitchenBlock.rotations(Shapes.or(box(0, 2, 7, 16, 16, 9), box(7, 0, 3, 9, 2, 13)))
    };
    private final Design design;
    public OfficeConnectedBlock(Design design, Properties properties) {
        super(properties); this.design = design;
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }
    public boolean supportsTabletopProps() { return design == Design.CONFERENCE; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LEFT, RIGHT);
    }
    private boolean joins(BlockState state, BlockState other) {
        return other.is(this) && state.getValue(FACING) == other.getValue(FACING);
    }
    public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos a = pos.relative(facing.getCounterClockWise()), b = pos.relative(facing.getClockWise());
        return state.setValue(LEFT, level.hasChunkAt(a) && joins(state, level.getBlockState(a)))
                .setValue(RIGHT, level.hasChunkAt(b) && joins(state, level.getBlockState(b)));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : layout(state, context.getLevel(), context.getClickedPos());
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        BlockState supported = super.updateShape(state, side, neighbor, level, pos, otherPos);
        if (supported.isAir()) return supported;
        Direction facing = state.getValue(FACING);
        if (side == facing.getCounterClockWise()) return supported.setValue(LEFT, level.hasChunkAt(otherPos) && joins(state, neighbor));
        if (side == facing.getClockWise()) return supported.setValue(RIGHT, level.hasChunkAt(otherPos) && joins(state, neighbor));
        return supported;
    }
    @Override public boolean skipRendering(BlockState state, BlockState other, Direction side) {
        Direction facing = state.getValue(FACING);
        return joins(state, other) && (side == facing.getClockWise() || side == facing.getCounterClockWise())
                || super.skipRendering(state, other, side);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
