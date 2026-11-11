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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** New fixtures place one module; saved two-cell fixtures retain their linked removal lifecycle. */
public final class RestroomSpaModuleBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty MODULAR = BooleanProperty.create("modular"),
            LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    public enum Design implements StringRepresentable {
        SINK_COUNTER(false, Shapes.or(box(0, 0, 3, 16, 12.25, 16),
                box(1.5, 12.25, 3.75, 14.5, 12.75, 13.25), box(0, 12.75, 2.5, 16, 15.5, 3.75),
                box(0, 12.75, 13.25, 16, 15.5, 16), box(0, 12.75, 3.75, 1.5, 15.5, 13.25),
                box(14.5, 12.75, 3.75, 16, 15.5, 13.25), box(6.75, 15.5, 11.25, 9.25, 19, 15.25)).optimize()),
        MIRROR(true, box(0, 2.25, 15.125, 16, 14.75, 16));
        private final boolean wall;
        private final VoxelShape[] shapes;
        Design(boolean wall, VoxelShape shape) { this.wall = wall; shapes = RestaurantKitchenBlock.rotations(shape); }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private static final VoxelShape[][] SINK_SHAPES = createSinkShapes();
    private static VoxelShape[][] createSinkShapes() {
        VoxelShape[][] result = new VoxelShape[4][];
        for (int ends = 0; ends < 4; ends++) {
            VoxelShape shape = Design.SINK_COUNTER.shapes[0];
            if ((ends & 1) != 0) shape = Shapes.or(shape, box(0, 15.5, 2.5, .5, 15.8125, 13.25));
            if ((ends & 2) != 0) shape = Shapes.or(shape, box(15.5, 15.5, 2.5, 16, 15.8125, 13.25));
            result[ends] = RestaurantKitchenBlock.rotations(shape.optimize());
        }
        return result;
    }
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<RestroomSpaModuleBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((RestroomSpaModuleBlock b) -> b.design),
            propertiesCodec()).apply(instance, RestroomSpaModuleBlock::new));
    //?}
    public RestroomSpaModuleBlock(Design design, Properties properties) {
        super(properties); this.design = design;
        registerDefaultState(defaultBlockState().setValue(MODULAR, false).setValue(LEFT, false).setValue(RIGHT, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(MODULAR, LEFT, RIGHT);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(MODULAR) ? 1 : 2; }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getClockWise(); }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        if (!design.wall) return super.getPlacementFacing(context);
        return context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null;
    }
    private boolean connects(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(MODULAR) && other.getValue(FACING) == state.getValue(FACING);
    }
    private BlockState connections(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos left = pos.relative(facing.getCounterClockWise()), right = pos.relative(facing.getClockWise());
        return state.setValue(LEFT, level.hasChunkAt(left) && connects(state, level.getBlockState(left)))
                .setValue(RIGHT, level.hasChunkAt(right) && connects(state, level.getBlockState(right)));
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : connections(state.setValue(MODULAR, true), context.getLevel(), context.getClickedPos());
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(MODULAR) && !state.getValue(PRIMARY)) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = design.wall ? state.getValue(FACING) : Direction.UP;
        BlockPos support = design.wall ? pos.relative(face.getOpposite()) : pos.below();
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (!state.getValue(MODULAR)) {
            Direction along = getExtensionDirection(state);
            if (side == (state.getValue(PRIMARY) ? along : along.getOpposite())
                    && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        } else {
            Direction facing = state.getValue(FACING);
            if (side == facing.getCounterClockWise()) return state.setValue(LEFT, connects(state, neighbor));
            if (side == facing.getClockWise()) return state.setValue(RIGHT, connects(state, neighbor));
        }
        Direction support = design.wall ? state.getValue(FACING).getOpposite() : Direction.DOWN;
        return side == support && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        if (mirror == Mirror.NONE) return result;
        return state.getValue(MODULAR)
                ? result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT))
                : result.setValue(PRIMARY, !state.getValue(PRIMARY));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = RestaurantKitchenBlock.turn(state);
        if (design == Design.MIRROR) return design.shapes[turn];
        int ends = state.getValue(MODULAR)
                ? (state.getValue(LEFT) ? 0 : 1) | (state.getValue(RIGHT) ? 0 : 2)
                : state.getValue(PRIMARY) ? 1 : 2;
        return SINK_SHAPES[ends][turn];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
