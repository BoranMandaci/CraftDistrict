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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A two-high brochure rack; old single-cell placements remain single-cell. */
public final class OfficeMagazineRackBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    //? if >=1.21 {
    public static final MapCodec<OfficeMagazineRackBlock> CODEC = simpleCodec(OfficeMagazineRackBlock::new);
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 3, 14, 1, 13), box(2, 1, 8, 14, 16, 12))),
            RestaurantKitchenBlock.rotations(Shapes.or(box(1.5, 0, 3, 14.5, 1, 14), box(2, 1, 5, 14, 16, 13.25))),
            RestaurantKitchenBlock.rotations(box(2, 0, 7, 14, 15.5, 13.25))
    };
    public OfficeMagazineRackBlock(Properties properties) {
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
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        if (state.getValue(EXPANDED)) {
            Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
            if (side == partner && other != stateForPart(state, 1 - partIndex(state))) return Blocks.AIR.defaultBlockState();
        }
        return state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(EXPANDED) ? (state.getValue(PRIMARY) ? 1 : 2) : 0][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
