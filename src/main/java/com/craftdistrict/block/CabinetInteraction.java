package com.craftdistrict.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import java.util.List;

/** One server-owned state update; no inventory, block entity, or client-side sound. */
final class CabinetInteraction {
    private CabinetInteraction() { }

    static InteractionResult toggle(BlockState clicked, Level level, BlockPos pos, Player player,
                                    SoundEvent openSound, SoundEvent closeSound) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(clicked.getBlock())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean open = !current.getValue(BlockStateProperties.OPEN);
        if (!level.setBlock(pos, current.setValue(BlockStateProperties.OPEN, open), Block.UPDATE_ALL)) {
            return InteractionResult.PASS;
        }
        com.craftdistrict.item.WrenchItem.usedIfHeld(player);
        level.playSound(null, pos, open ? openSound : closeSound, SoundSource.BLOCKS, .6F, 1.1F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.CONSUME;
    }

    /** Commit a complete linked door together; a failed write restores only our own changes. */
    static InteractionResult toggleLinked(MultiBlockDecorationBlock block, Level level, BlockPos pos, Player player,
                                          SoundEvent openSound, SoundEvent closeSound) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(block)) return InteractionResult.PASS;
        BlockPos origin = block.structureOrigin(current, pos);
        BlockState primary = level.getBlockState(origin);
        if (!primary.is(block)) return InteractionResult.PASS;
        List<BlockPos> parts = block.structureParts(primary, origin);
        boolean door = block instanceof HotelElevatorDoorsBlock || block instanceof VaultDoorBlock
                || block instanceof HotelShowerEnclosureBlock;
        BlockState[] before = new BlockState[parts.size()];
        for (int index = 0; index < parts.size(); index++) {
            if (door && !level.hasChunkAt(parts.get(index))) return InteractionResult.PASS;
            before[index] = level.getBlockState(parts.get(index));
            if (before[index] != block.structurePartState(primary, index)) return InteractionResult.PASS;
        }
        if (level.isClientSide && !door) return InteractionResult.SUCCESS;
        boolean open = !primary.getValue(BlockStateProperties.OPEN);
        if (door) {
            if (!commitDoor(level, parts, before, origin, player, open, openSound, closeSound)) return InteractionResult.PASS;
            if (!level.isClientSide) {
                com.craftdistrict.item.WrenchItem.usedIfHeld(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        int written = 0;
        boolean valid = true;
        for (int index = 0; index < parts.size(); index++) {
            if (level.getBlockState(parts.get(index)) != before[index]
                    || !level.setBlock(parts.get(index), before[index].setValue(BlockStateProperties.OPEN, open), flags)) {
                valid = false;
                break;
            }
            written++;
            for (int checked = 0; checked < written; checked++) {
                if (level.getBlockState(parts.get(checked)) != before[checked].setValue(BlockStateProperties.OPEN, open)) {
                    valid = false;
                    break;
                }
            }
            if (!valid) break;
        }
        if (!valid) {
            for (int index = written - 1; index >= 0; index--) {
                if (level.getBlockState(parts.get(index)) == before[index].setValue(BlockStateProperties.OPEN, open))
                    level.setBlock(parts.get(index), before[index], flags);
            }
            return InteractionResult.PASS;
        }
        com.craftdistrict.item.WrenchItem.usedIfHeld(player);
        level.playSound(null, origin, open ? openSound : closeSound, SoundSource.BLOCKS, .6F, 1.1F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, origin);
        return InteractionResult.CONSUME;
    }

    static boolean setLinkedDoor(MultiBlockDecorationBlock block, Level level, BlockPos root, Player source,
                                 boolean open, SoundEvent openSound, SoundEvent closeSound) {
        BlockState primary = level.getBlockState(root);
        if (!primary.is(block)) return false;
        List<BlockPos> parts = block.structureParts(primary, root);
        BlockState[] before = new BlockState[parts.size()];
        for (int i = 0; i < before.length; i++) {
            BlockPos part = parts.get(i);
            if (!level.hasChunkAt(part)) return false;
            before[i] = level.getBlockState(part);
            if (before[i] != block.structurePartState(primary, i)) return false;
        }
        return primary.getValue(BlockStateProperties.OPEN) == open
                || commitDoor(level, parts, before, root, source, open, openSound, closeSound);
    }

    private static boolean commitDoor(Level level, List<BlockPos> parts, BlockState[] before, BlockPos root, Player source,
                                      boolean open, SoundEvent openSound, SoundEvent closeSound) {
        BlockState[] after = new BlockState[before.length];
        for (int i = 0; i < before.length; i++) after[i] = before[i].setValue(BlockStateProperties.OPEN, open);
        if (!DoorStateUpdates.commit(level, parts.toArray(BlockPos[]::new), before, after)) return false;
        level.playSound(source, root, open ? openSound : closeSound, SoundSource.BLOCKS, .6F, 1.1F);
        if (!level.isClientSide) level.gameEvent(source, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, root);
        return true;
    }
}
