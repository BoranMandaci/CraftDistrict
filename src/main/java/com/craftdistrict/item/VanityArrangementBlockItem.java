package com.craftdistrict.item;

import com.craftdistrict.block.HotelBathroomAccessoryBlock;
import com.craftdistrict.block.HotelVanityMirrorBlock;
import com.craftdistrict.block.MultiBlockDecorationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Only a mirror and the existing amenities tray may share a vanity cell; neither stores items. */
public final class VanityArrangementBlockItem extends BlockItem {
    public VanityArrangementBlockItem(Block block, Properties properties) { super(block, properties); }
    private static boolean tray(BlockState state) {
        return state.getBlock() instanceof HotelBathroomAccessoryBlock block && block.isToiletries();
    }
    private static boolean allowed(UseOnContext context, BlockPos pos) {
        Player player = context.getPlayer(); Level level = context.getLevel();
        return player != null && !player.isSpectator() && level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos)
                && level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand());
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        for (BlockPos pos : new BlockPos[]{context.getClickedPos(), context.getClickedPos().relative(context.getClickedFace())}) {
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (getBlock() instanceof HotelVanityMirrorBlock mirror && tray(state)) return placeMirror(context, pos, state, mirror);
            if (getBlock() instanceof HotelBathroomAccessoryBlock accessory && accessory.isToiletries()
                    && state.getBlock() instanceof HotelVanityMirrorBlock mirror
                    && state.getValue(HotelVanityMirrorBlock.STACKED) && !state.getValue(HotelVanityMirrorBlock.TRAY))
                return addTray(context, pos, state, mirror);
        }
        return super.useOn(context);
    }
    private InteractionResult placeMirror(UseOnContext context, BlockPos pos, BlockState oldTray, HotelVanityMirrorBlock mirror) {
        Level level = context.getLevel(); Player player = context.getPlayer();
        if (!allowed(context, pos) || !allowed(context, pos.above())) return InteractionResult.FAIL;
        Direction facing = context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : oldTray.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockState lower = mirror.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(HotelVanityMirrorBlock.STACKED, true).setValue(HotelVanityMirrorBlock.TRAY, true)
                .setValue(HotelVanityMirrorBlock.TRAY_FACING, oldTray.getValue(BlockStateProperties.HORIZONTAL_FACING));
        BlockState upper = mirror.structurePartState(lower, 1);
        BlockPlaceContext placement = new BlockPlaceContext(context);
        if (!lower.canSurvive(level, pos) || !upper.canSurvive(level, pos.above())
                || !level.getBlockState(pos.above()).canBeReplaced(placement)
                || !level.isUnobstructed(lower, pos, CollisionContext.of(player))
                || !level.isUnobstructed(upper, pos.above(), CollisionContext.of(player))) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockState(pos) != oldTray || !level.setBlock(pos, lower, Block.UPDATE_ALL)) return InteractionResult.FAIL;
        mirror.setPlacedBy(level, pos, lower, player, context.getItemInHand());
        if (level.getBlockState(pos) != lower || level.getBlockState(pos.above()) != upper) {
            BlockState current = level.getBlockState(pos);
            if (current == lower || current.isAir()) level.setBlock(pos, oldTray, Block.UPDATE_ALL);
            return InteractionResult.FAIL;
        }
        return complete(context, pos, lower);
    }
    private InteractionResult addTray(UseOnContext context, BlockPos clickedPos, BlockState clicked, HotelVanityMirrorBlock mirror) {
        if (!clicked.getValue(HotelVanityMirrorBlock.STACKED)) return InteractionResult.FAIL;
        Level level = context.getLevel(); Player player = context.getPlayer();
        BlockPos pos = mirror.structureOrigin(clicked, clickedPos);
        if (!allowed(context, pos) || !allowed(context, pos.above())) return InteractionResult.FAIL;
        BlockState lower = level.getBlockState(pos), upper = level.getBlockState(pos.above());
        if (!lower.is(mirror) || lower.getValue(HotelVanityMirrorBlock.TRAY) || !lower.getValue(MultiBlockDecorationBlock.PRIMARY)
                || upper != mirror.structurePartState(lower, 1)) return InteractionResult.FAIL;
        BlockPlaceContext placement = new BlockPlaceContext(context) {
            @Override public BlockPos getClickedPos() { return pos; }
        };
        BlockState trayState = getBlock().getStateForPlacement(placement);
        if (trayState == null) return InteractionResult.FAIL;
        BlockState joined = lower.setValue(HotelVanityMirrorBlock.TRAY, true)
                .setValue(HotelVanityMirrorBlock.TRAY_FACING, trayState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        BlockState joinedUpper = mirror.structurePartState(joined, 1);
        if (!level.isUnobstructed(joined, pos, CollisionContext.of(player))) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        if (!level.setBlock(pos, joined, flags)) return InteractionResult.FAIL;
        boolean valid = level.getBlockState(pos) == joined && level.getBlockState(pos.above()) == upper
                && level.setBlock(pos.above(), joinedUpper, flags)
                && level.getBlockState(pos) == joined && level.getBlockState(pos.above()) == joinedUpper;
        if (!valid) {
            if (level.getBlockState(pos.above()) == joinedUpper) level.setBlock(pos.above(), upper, flags);
            if (level.getBlockState(pos) == joined) level.setBlock(pos, lower, flags);
            return InteractionResult.FAIL;
        }
        return complete(context, pos, trayState);
    }
    private InteractionResult complete(UseOnContext context, BlockPos pos, BlockState placed) {
        Player player = context.getPlayer(); ItemStack stack = context.getItemInHand();
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        SoundType sound = placed.getSoundType();
        context.getLevel().playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * .8F);
        context.getLevel().gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        return InteractionResult.CONSUME;
    }
}
