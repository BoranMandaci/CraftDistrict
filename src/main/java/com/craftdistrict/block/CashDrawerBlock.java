package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CashDrawerBlock extends CashRegisterBlock {
    private static final VoxelShape CASE = box(1.6, 0, 6.7, 14.4, 3.7, 14.2);
    private static final VoxelShape[] CLOSED = RestaurantKitchenBlock.rotations(Shapes.or(CASE,
            box(1.75, .55, 5.84, 14.25, 3.03, 6.7)));
    private static final VoxelShape[] OPEN = RestaurantKitchenBlock.rotations(Shapes.or(CASE,
            box(2.3, .7, 1.5, 13.7, 2.45, 10.25), box(1.75, .55, .64, 14.25, 3.03, 1.5)));

    public CashDrawerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.OPEN);
    }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggle(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? toggle(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    private InteractionResult toggle(BlockState state, Level level, BlockPos pos, Player player) {
        return CabinetInteraction.toggle(state, level, pos, player, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(BlockStateProperties.OPEN) ? OPEN : CLOSED)[RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    public static final MapCodec<CashDrawerBlock> CODEC = simpleCodec(CashDrawerBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
