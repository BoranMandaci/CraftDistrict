package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** One-cell shelves join aligned neighbors locally, horizontally and vertically.
 * Former two-part states load as two independent stacked shelves under the same ID. */
public final class OfficeBookshelfBlock extends RotatableDecorationBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right"),
            UP = BooleanProperty.create("up"), DOWN = BooleanProperty.create("down");
    //? if >=1.21 {
    public static final MapCodec<OfficeBookshelfBlock> CODEC = simpleCodec(OfficeBookshelfBlock::new);
    //?}
    public OfficeBookshelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false).setValue(UP, false).setValue(DOWN, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LEFT, RIGHT, UP, DOWN);
    }
    private boolean matches(BlockState other, Direction facing) {
        return other.is(this) && other.getValue(FACING) == facing;
    }
    public BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, matches(level.getBlockState(pos.relative(facing.getCounterClockWise())), facing))
                .setValue(RIGHT, matches(level.getBlockState(pos.relative(facing.getClockWise())), facing))
                .setValue(UP, matches(level.getBlockState(pos.above()), facing))
                .setValue(DOWN, matches(level.getBlockState(pos.below()), facing));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : layout(state, context.getLevel(), context.getClickedPos());
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Each shelf owns its own vanilla loot. A join/support change must never
        // destroy another unit (including after Creative-mode removal below it).
        return layout(state, level, pos);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? result : result.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
