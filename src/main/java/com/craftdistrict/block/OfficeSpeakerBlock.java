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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A static tabletop speaker; the old lowered state remains readable after the table-height update. */
public final class OfficeSpeakerBlock extends CashRegisterBlock {
    public static final BooleanProperty LOWERED = BooleanProperty.create("lowered");
    //? if >=1.21 {
    public static final MapCodec<OfficeSpeakerBlock> CODEC = simpleCodec(OfficeSpeakerBlock::new);
    //?}
    private static final VoxelShape NORMAL = box(2, 0, 3.625, 14, 6.75, 12);
    private static final VoxelShape[] ORIENTED = RestaurantKitchenBlock.rotations(NORMAL);
    private static final VoxelShape[][] SHAPES = {ORIENTED, ORIENTED};

    public OfficeSpeakerBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(LOWERED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LOWERED);
    }

    private static boolean lowTable(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState state = level.getBlockState(below);
        return state.getBlock() instanceof OfficeConnectedBlock table && table.supportsTabletopProps()
                && Math.abs(state.getCollisionShape(level, below).max(Direction.Axis.Y) - 14.5 / 16) < 1e-7;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return lowTable(level, pos) || super.canSurvive(state, level, pos);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LOWERED, lowTable(context.getLevel(), context.getClickedPos()));
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState supported = super.updateShape(state, side, neighbor, level, pos, neighborPos);
        if (supported.isAir() || side != Direction.DOWN) return supported;
        boolean lowered = lowTable(level, pos);
        if (!lowered && !neighbor.isFaceSturdy(level, neighborPos, Direction.UP)) return supported;
        return supported.setValue(LOWERED, lowered);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LOWERED) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
