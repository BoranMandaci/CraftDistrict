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

/** A static tabletop speaker, seated flush on the lower Office conference worktop. */
public final class OfficeSpeakerBlock extends CashRegisterBlock {
    public static final BooleanProperty LOWERED = BooleanProperty.create("lowered");
    //? if >=1.21 {
    public static final MapCodec<OfficeSpeakerBlock> CODEC = simpleCodec(OfficeSpeakerBlock::new);
    //?}
    private static final VoxelShape NORMAL = box(2, 0, 3.625, 14, 6.75, 12);
    private static final VoxelShape[][] SHAPES = {RestaurantKitchenBlock.rotations(NORMAL),
            RestaurantKitchenBlock.rotations(NORMAL.move(0, -1.5 / 16, 0))};

    public OfficeSpeakerBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(LOWERED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(LOWERED);
    }

    private static boolean lowTable(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof OfficeConnectedBlock table
                && table.supportsTabletopProps();
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
        return supported.isAir() || side != Direction.DOWN ? supported
                : supported.setValue(LOWERED, lowTable(level, pos));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LOWERED) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
