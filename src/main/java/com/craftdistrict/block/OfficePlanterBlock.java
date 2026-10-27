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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-high lobby planter; saved single-cell plants retain their original ownership. */
public final class OfficePlanterBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    //? if >=1.21 {
    public static final MapCodec<OfficePlanterBlock> CODEC = simpleCodec(OfficePlanterBlock::new);
    //?}
    private static final VoxelShape[] SHAPES = {
            Shapes.or(box(0, 0, 0, 16, 14.5, 16), box(1, 14.5, 1, 15, 32, 15)).optimize(),
            Shapes.or(box(0, 0, 0, 16, 14.5, 16), box(1, 14.5, 1, 15, 16, 15)).optimize(),
            box(1, 0, 1, 15, 16, 15)};

    public OfficePlanterBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(EXPANDED, false));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(EXPANDED);
    }

    @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 2 : 1; }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(EXPANDED, true);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(EXPANDED) && !state.getValue(PRIMARY)) return false;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        if (state.getValue(EXPANDED)) {
            Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
            if (side == partner && neighbor != stateForPart(state, 1 - partIndex(state))) return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(EXPANDED) ? (state.getValue(PRIMARY) ? 1 : 2) : 0];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
