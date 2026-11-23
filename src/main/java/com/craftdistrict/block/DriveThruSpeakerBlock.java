package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DriveThruSpeakerBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty TALL = BooleanProperty.create("tall");
    private static final VoxelShape[] LEGACY = RestaurantKitchenBlock.rotations(Shapes.or(
            box(4.85, 0, 5, 11.15, .85, 11.8), box(5.72, .85, 6.4, 10.28, 9.1, 10.95),
            box(5.16, 9.1, 5.93, 10.84, 15.5, 10.98)));
    private static final VoxelShape[] LOWER = RestaurantKitchenBlock.rotations(Shapes.or(
            box(4.08, 0, 4.7, 11.92, 1.05, 12.1), box(5.2, 1.05, 6.19, 10.8, 14.5, 10.9),
            box(4.52, 14.5, 5.58, 11.48, 16, 11.25)));
    private static final VoxelShape[] UPPER = RestaurantKitchenBlock.rotations(Shapes.or(
            box(4.52, 0, 5.58, 11.48, 7, 11.25), box(4.52, 7, 5.36, 11.48, 7.4, 11.42),
            box(5.21, .26, 5.42, 10.79, 2.85, 5.58), box(5.21, 3.13, 5.4, 10.79, 6.61, 5.58)));

    public DriveThruSpeakerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TALL, false));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(TALL);
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return super.getPlacementState(context).setValue(TALL, true);
    }

    @Override protected int partCount(BlockState state) {
        return state.getValue(TALL) ? 2 : 1;
    }

    @Override protected Direction getExtensionDirection(BlockState state) {
        return Direction.UP;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (state.getValue(TALL) && side == partner && !isRemovingLinkedStructure(state, level, pos)
                && neighbor != state.setValue(PRIMARY, !state.getValue(PRIMARY))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape[] shape = !state.getValue(TALL) ? LEGACY : state.getValue(PRIMARY) ? LOWER : UPPER;
        return shape[RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<DriveThruSpeakerBlock> CODEC = simpleCodec(DriveThruSpeakerBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
