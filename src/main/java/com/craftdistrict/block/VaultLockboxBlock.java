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

/** The existing lockbox item now opens a hinged briefcase, with decorative cash and no inventory. */
public final class VaultLockboxBlock extends VaultValuablesBlock {
    private static final VoxelShape[] CLOSED = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1.5, 0, 3, 14.5, 4.3, 13.5), box(5, 1, 1.1, 11, 2.6, 3)).optimize());
    private static final VoxelShape[] OPEN = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1.5, 0, 3, 14.5, 2.8, 13.5), box(5, 1, 1.1, 11, 2.6, 3),
            box(1.5, 2.8, 13.45, 14.5, 13.8, 15)).optimize());
    //? if >=1.21 {
    public static final MapCodec<VaultLockboxBlock> CODEC = simpleCodec(VaultLockboxBlock::new);
    //?}
    public VaultLockboxBlock(Properties properties) {
        super(Design.LOCKBOX, properties); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(BlockStateProperties.OPEN);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return CabinetInteraction.toggle(state, level, pos, player, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND
                ? CabinetInteraction.toggle(state, level, pos, player, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE)
                : InteractionResult.PASS;
    }
    *///?}
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(BlockStateProperties.OPEN) ? OPEN : CLOSED)[RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
