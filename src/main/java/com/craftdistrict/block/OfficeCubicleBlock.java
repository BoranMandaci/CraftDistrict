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

/** Two-wide cubicle; old single-cell placements never claim an occupied neighboring cell. */
public final class OfficeCubicleBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    //? if >=1.21 {
    public static final MapCodec<OfficeCubicleBlock> CODEC = simpleCodec(OfficeCubicleBlock::new);
    //?}
    private static final VoxelShape[][] SHAPES = makeShapes();

    public OfficeCubicleBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(EXPANDED, false));
    }

    private static VoxelShape[][] makeShapes() {
        VoxelShape desk = Shapes.or(box(0, 13.5, 1, 16, 14.5, 15.25),
                box(0, 6.25, 15.25, 16, 26.5, 16));
        VoxelShape left = Shapes.or(box(0, 0, 4, 1, 13.5, 15.25),
                box(0, 14.5, 5, .75, 26.5, 15.25));
        VoxelShape right = Shapes.or(box(15, 0, 4, 16, 13.5, 15.25),
                box(15.25, 14.5, 5, 16, 26.5, 15.25));
        return new VoxelShape[][] {
                RestaurantKitchenBlock.rotations(Shapes.or(desk, left, right)),
                RestaurantKitchenBlock.rotations(Shapes.or(desk, left)),
                RestaurantKitchenBlock.rotations(Shapes.or(desk, right))};
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(EXPANDED);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 2 : 1; }
    @Override protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(FACING).getClockWise();
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(EXPANDED, true);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(EXPANDED) && !state.getValue(PRIMARY)) return false;
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        if (state.getValue(EXPANDED)) {
            Direction extension = getExtensionDirection(state);
            Direction partner = state.getValue(PRIMARY) ? extension : extension.getOpposite();
            if (side == partner && other != stateForPart(state, 1 - partIndex(state))) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(EXPANDED) ? (state.getValue(PRIMARY) ? 1 : 2) : 0]
                [RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
