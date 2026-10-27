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

/** New placements use four cells; saved two-part boards keep their original footprint. */
public final class OfficeBoardBlock extends WideTallDecorationBlock {
    public enum Design implements StringRepresentable {
        SCREEN, WHITEBOARD;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    //? if >=1.21 {
    public static final MapCodec<OfficeBoardBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((OfficeBoardBlock b) -> b.design),
            propertiesCodec()).apply(instance, OfficeBoardBlock::new));
    //?}
    private static final VoxelShape[][][][] SHAPES = buildShapes();
    private final Design design;

    public OfficeBoardBlock(Design design, Properties properties) {
        super(properties); this.design = design;
        registerDefaultState(defaultBlockState().setValue(EXPANDED, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(PRIMARY, EXPANDED);
    }
    @Override protected BlockState stateForPart(BlockState state, int index) {
        return super.stateForPart(state, index).setValue(PRIMARY, index == 0);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 4 : 2; }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        if (state.getValue(EXPANDED)) return super.partOffset(state, index);
        return design == Design.SCREEN ? BlockPos.ZERO.relative(state.getValue(FACING).getClockWise(), index)
                : BlockPos.ZERO.above(index);
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(EXPANDED, true);
    }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        if (design != Design.SCREEN) return super.getPlacementFacing(context);
        return context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int part = partIndex(state);
        if (part >= partCount(state)) return false;
        if (design == Design.SCREEN) {
            Direction facing = state.getValue(FACING);
            BlockPos wall = pos.relative(facing.getOpposite());
            return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
        }
        int row = state.getValue(EXPANDED) ? part / 2 : part;
        BlockPos floor = pos.below(1 + row);
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction support = design == Design.SCREEN ? state.getValue(FACING).getOpposite() : Direction.DOWN;
        if (side == support && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        BlockPos origin = structureOrigin(state, pos);
        for (int i = 0; i < partCount(state); i++)
            if (i != partIndex(state) && otherPos.equals(origin.offset(partOffset(state, i)))
                    && other != stateForPart(state, i)) return Blocks.AIR.defaultBlockState();
        return state;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.rotate(mirror.getRotation(state.getValue(FACING)));
        if (mirror != Mirror.NONE && (state.getValue(EXPANDED) || design == Design.SCREEN)) {
            int part = partIndex(state);
            result = result.setValue(PART, part ^ 1);
            if (part < 2) result = result.setValue(PRIMARY, !state.getValue(PRIMARY));
        }
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][state.getValue(EXPANDED) ? 1 : 0][partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    private static VoxelShape[][][][] buildShapes() {
        VoxelShape[][][][] all = new VoxelShape[2][2][4][];
        for (Design design : Design.values()) for (int expanded = 0; expanded < 2; expanded++) for (int p = 0; p < 4; p++) {
            boolean right = (p & 1) != 0, upper = p >= 2;
            VoxelShape shape;
            if (design == Design.SCREEN) {
                shape = box(right ? 0 : .5, expanded == 1 && upper ? 0 : 2, 14,
                        right ? 15.5 : 16, expanded == 1 ? (upper ? 14 : 16) : 15, 16);
            } else if (expanded == 0) {
                shape = p == 0 ? Shapes.or(box(2, 0, 3, 14, 2, 13), box(1, 7, 7, 15, 16, 9)) : box(1, 0, 7, 15, 14, 9);
            } else if (upper) {
                shape = box(right ? 0 : 1, 0, 7, right ? 15 : 16, 14, 9.25);
            } else {
                double x = right ? 11 : 4;
                shape = Shapes.or(box(right ? 0 : 1, 8, 7, right ? 15 : 16, 16, 9.25),
                        box(x, 1.5, 7, x + 1, 8, 9), box(x - 1, 0, 3, x + 2, 1.5, 13));
            }
            all[design.ordinal()][expanded][p] = RestaurantKitchenBlock.rotations(shape);
        }
        return all;
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
