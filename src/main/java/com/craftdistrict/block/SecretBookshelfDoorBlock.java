package com.craftdistrict.block;

import com.craftdistrict.access.AccessDoorSecurity;
import com.craftdistrict.item.WrenchItem;
//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two linked halves swing a door-thick leaf within their own cells. */
public final class SecretBookshelfDoorBlock extends MultiBlockDecorationBlock {
    private static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    private static final VoxelShape[] CLOSED = RestaurantKitchenBlock.rotations(box(0, 0, 0, 16, 16, 3));
    private static final VoxelShape[] OPEN_LEFT = RestaurantKitchenBlock.rotations(box(0, 0, 0, 3, 16, 16));
    private static final VoxelShape[] OPEN_RIGHT = RestaurantKitchenBlock.rotations(box(13, 0, 0, 16, 16, 16));

    //? if >=1.21 {
    public static final MapCodec<SecretBookshelfDoorBlock> CODEC = simpleCodec(SecretBookshelfDoorBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}

    public SecretBookshelfDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OPEN, false).setValue(HINGE, DoorHingeSide.LEFT));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(OPEN, HINGE);
    }

    @Override protected BlockPos partOffset(BlockState state, int index) { return BlockPos.ZERO.above(index); }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) return null;
        Direction right = state.getValue(FACING).getClockWise();
        double x = context.getClickLocation().x - context.getClickedPos().getX() - .5;
        double z = context.getClickLocation().z - context.getClickedPos().getZ() - .5;
        return state.setValue(HINGE, x * right.getStepX() + z * right.getStepZ() > 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state : super.mirror(state, mirror).cycle(HINGE);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return leafShape(state);
    }

    @Override public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return leafShape(state);
    }

    private static VoxelShape leafShape(BlockState state) {
        int turn = RestaurantKitchenBlock.turn(state);
        if (!state.getValue(OPEN)) return CLOSED[turn];
        return (state.getValue(HINGE) == DoorHingeSide.LEFT ? OPEN_LEFT : OPEN_RIGHT)[turn];
    }

    private InteractionResult toggle(BlockState state, Level level, BlockPos pos, Player player) {
        if (AccessDoorSecurity.secured(level, pos, state)) return AccessDoorSecurity.denied(level, player);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (move(state, level, pos, player, !state.getValue(OPEN))) WrenchItem.usedIfHeld(player);
        return InteractionResult.CONSUME;
    }

    boolean move(BlockState state, Level level, BlockPos pos, Player player, boolean open) {
        if (!isStructureComplete(state, level, pos)) return false;
        if (state.getValue(OPEN) == open) return true;
        BlockPos bottom = structureOrigin(state, pos);
        BlockPos[] parts = {bottom, bottom.above()};
        if (player != null && (!level.mayInteract(player, parts[0]) || !level.mayInteract(player, parts[1]))) return false;

        BlockState target = state.setValue(OPEN, open);
        if (!clear(level, parts, target)) {
            if (!open) return false;
            target = target.cycle(HINGE);
            if (!clear(level, parts, target)) return false;
        }

        BlockState[] before = {stateForPart(state, 0), stateForPart(state, 1)};
        BlockState[] after = {stateForPart(target, 0), stateForPart(target, 1)};
        if (DoorStateUpdates.commit(level, parts, before, after)) {
            level.playSound(null, bottom, open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
                    SoundSource.BLOCKS, .8F, .85F);
            level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, bottom);
            return true;
        }
        return false;
    }

    private static boolean clear(Level level, BlockPos[] parts, BlockState target) {
        // The destination leaf remains within the two already-owned cells.
        for (BlockPos part : parts)
            if (!level.isUnobstructed(target, part, CollisionContext.empty())) return false;
        return true;
    }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggle(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return toggle(state, level, pos, player);
    }
    *///?}
}
