package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two linked sections of a decorative locker with outward-opening doors. */
public final class GymLockersBlock extends GymEquipmentBlock {
    public GymLockersBlock(Properties properties) {
        super(Design.GYM_LOCKERS, properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN, false));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(BlockStateProperties.OPEN);
    }

    //? if >=1.21 {
    @Override public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                    Player player, InteractionHand hand, BlockHitResult hit) {
        if (placesOnTop(stack, state, hit)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggle(level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND && !placesOnTop(player.getItemInHand(hand), state, hit)
                ? toggle(level, pos, player) : InteractionResult.PASS;
    }
    *///?}

    private static boolean placesOnTop(ItemStack stack, BlockState state, BlockHitResult hit) {
        return !state.getValue(PRIMARY) && hit.getDirection() == Direction.UP && stack.getItem() instanceof BlockItem;
    }

    private InteractionResult toggle(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) || (!state.getValue(BlockStateProperties.OPEN) && !hasDoorClearance(level, pos, state))) {
            return InteractionResult.PASS;
        }
        return CabinetInteraction.toggleLinked(this, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
    }

    private boolean hasDoorClearance(Level level, BlockPos pos, BlockState state) {
        BlockPos origin = structureOrigin(state, pos);
        Direction front = state.getValue(FACING);
        int turn = RestaurantKitchenBlock.turn(state);
        for (int part = 0; part < partCount(state); part++) {
            BlockPos ahead = origin.offset(partOffset(state, part)).relative(front);
            if (!level.hasChunkAt(ahead)) return false;
            VoxelShape obstacle = level.getBlockState(ahead).getShape(level, ahead);
            if (Shapes.joinIsNotEmpty(GymEquipmentShapes.GYM_LOCKERS_OPEN[part][turn],
                    obstacle.move(front.getStepX(), 0, front.getStepZ()), BooleanOp.AND)) return false;
        }
        return true;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(BlockStateProperties.OPEN)
                ? GymEquipmentShapes.GYM_LOCKERS_OPEN[partIndex(state)][RestaurantKitchenBlock.turn(state)]
                : super.getShape(state, level, pos, context);
    }

    @Override public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape shape = super.getBlockSupportShape(state, level, pos);
        return state.getValue(PRIMARY) ? shape : DecorationSupport.withTop(shape);
    }

    //? if >=1.21 {
    public static final MapCodec<GymLockersBlock> CODEC = simpleCodec(GymLockersBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
