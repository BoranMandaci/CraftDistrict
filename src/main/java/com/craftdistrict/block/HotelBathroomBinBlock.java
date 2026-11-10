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

/** A saved decorative pedal lid reuses the ordinary cabinet/Wrench interaction. */
public final class HotelBathroomBinBlock extends CashRegisterBlock {
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(4, 0, 3.5, 12, 11, 12.5)),
            RestaurantKitchenBlock.rotations(Shapes.or(box(4, 0, 3.5, 12, 9.5, 12.5),
                    box(4.125, 9.5, 8.5, 11.875, 13, 13), box(4.125, 13, 6, 11.875, 16, 10)).optimize())};
    //? if >=1.21 {
    public static final MapCodec<HotelBathroomBinBlock> CODEC = simpleCodec(HotelBathroomBinBlock::new);
    //?}
    public HotelBathroomBinBlock(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(BlockStateProperties.OPEN);
    }
    private InteractionResult toggle(BlockState state, Level level, BlockPos pos, Player player) {
        return CabinetInteraction.toggle(state, level, pos, player, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return toggle(state, level, pos, player); }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? toggle(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BlockStateProperties.OPEN) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
