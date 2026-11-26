package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;
import java.util.function.UnaryOperator;

/** A suspended net that continues into coplanar panels and attaches to posts along its sides. */
public final class SportsNetBlock extends Block {
    private enum Edge implements StringRepresentable {
        END, PANEL, POST;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    private static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final EnumProperty<Edge> LEFT = EnumProperty.create("left", Edge.class);
    private static final EnumProperty<Edge> RIGHT = EnumProperty.create("right", Edge.class);
    private static final BooleanProperty UP = BlockStateProperties.UP;
    private static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    private static final VoxelShape[][] SHAPES = buildShapes();

    //? if >=1.21 {
    public static final MapCodec<SportsNetBlock> CODEC = simpleCodec(SportsNetBlock::new);
    @Override protected MapCodec<SportsNetBlock> codec() { return CODEC; }
    //?}

    public SportsNetBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z)
                .setValue(LEFT, Edge.END).setValue(RIGHT, Edge.END).setValue(UP, false).setValue(DOWN, false));
    }

    private static VoxelShape[][] buildShapes() {
        VoxelShape[][] shapes = new VoxelShape[16][];
        double reach = 8 - SportsPostBlock.Design.NET.radius();
        for (int mask = 0; mask < shapes.length; mask++) {
            double left = (mask & 1) == 0 ? 0 : -reach;
            double right = (mask & 2) == 0 ? 16 : 16 + reach;
            double top = (mask & 4) == 0 ? 15.14 : 16;
            double bottom = (mask & 8) == 0 ? 3.10 : 0;
            shapes[mask] = RestaurantKitchenBlock.rotations(box(left, bottom, 7.58, right, top, 8.42));
        }
        return shapes;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, LEFT, RIGHT, UP, DOWN);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState anchor = level.getBlockState(pos.relative(face.getOpposite()));
        BlockState above = face == Direction.DOWN ? anchor : level.getBlockState(pos.above());
        BlockState below = face == Direction.UP ? anchor : level.getBlockState(pos.below());
        Direction.Axis aimed = context.getHorizontalDirection().getAxis();
        Direction.Axis axis = aimed;
        if (face.getAxis().isHorizontal() && SportsPostBlock.isPost(anchor, SportsPostBlock.Design.NET)) {
            axis = across(face.getAxis());
        } else if (anchor.is(this) && anchor.getValue(AXIS) != face.getAxis()) {
            axis = anchor.getValue(AXIS);
        } else {
            Direction.Axis alternate = across(aimed);
            int aimedScore = horizontalScore(level, pos, aimed);
            int alternateScore = horizontalScore(level, pos, alternate);
            if (alternateScore > aimedScore || (alternateScore == aimedScore
                    && verticalScore(above, below, alternate) > verticalScore(above, below, aimed))) axis = alternate;
        }
        Direction left = leftOf(axis);
        return defaultBlockState().setValue(AXIS, axis)
                .setValue(LEFT, edge(level.getBlockState(pos.relative(left)), axis))
                .setValue(RIGHT, edge(level.getBlockState(pos.relative(left.getOpposite())), axis))
                .setValue(UP, matchesPanel(above, axis)).setValue(DOWN, matchesPanel(below, axis));
    }

    private int horizontalScore(BlockGetter level, BlockPos pos, Direction.Axis axis) {
        Direction left = leftOf(axis);
        return weight(edge(level.getBlockState(pos.relative(left)), axis))
                + weight(edge(level.getBlockState(pos.relative(left.getOpposite())), axis));
    }

    private int verticalScore(BlockState above, BlockState below, Direction.Axis axis) {
        return (matchesPanel(above, axis) ? 1 : 0) + (matchesPanel(below, axis) ? 1 : 0);
    }

    private static int weight(Edge edge) {
        return switch (edge) { case END -> 0; case POST -> 1; case PANEL -> 2; };
    }

    private Edge edge(BlockState neighbor, Direction.Axis axis) {
        if (matchesPanel(neighbor, axis)) return Edge.PANEL;
        return SportsPostBlock.isPost(neighbor, SportsPostBlock.Design.NET) ? Edge.POST : Edge.END;
    }

    private boolean matchesPanel(BlockState neighbor, Direction.Axis axis) {
        return neighbor.is(this) && neighbor.getValue(AXIS) == axis;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        Direction.Axis axis = state.getValue(AXIS);
        if (side == Direction.UP) return state.setValue(UP, matchesPanel(neighbor, axis));
        if (side == Direction.DOWN) return state.setValue(DOWN, matchesPanel(neighbor, axis));
        Direction left = leftOf(axis);
        if (side == left) return state.setValue(LEFT, edge(neighbor, axis));
        if (side == left.getOpposite()) return state.setValue(RIGHT, edge(neighbor, axis));
        return state;
    }

    private static Direction.Axis across(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    private static Direction leftOf(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.NORTH : Direction.WEST;
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return transform(state, rotation::rotate);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return transform(state, mirror::mirror);
    }

    private static BlockState transform(BlockState state, UnaryOperator<Direction> operation) {
        Direction newLeft = operation.apply(leftOf(state.getValue(AXIS)));
        Direction.Axis axis = across(newLeft.getAxis());
        BlockState rotated = state.setValue(AXIS, axis);
        return newLeft == leftOf(axis) ? rotated : rotated.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = (state.getValue(LEFT) == Edge.POST ? 1 : 0) | (state.getValue(RIGHT) == Edge.POST ? 2 : 0)
                | (state.getValue(UP) ? 4 : 0) | (state.getValue(DOWN) ? 8 : 0);
        return SHAPES[mask][state.getValue(AXIS) == Direction.Axis.X ? 1 : 0];
    }
}
