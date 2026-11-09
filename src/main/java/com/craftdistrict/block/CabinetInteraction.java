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
        BlockState[] before = new BlockState[parts.size()];
        for (int index = 0; index < parts.size(); index++) {
            before[index] = level.getBlockState(parts.get(index));
            if (before[index] != block.structurePartState(primary, index)) return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean open = !primary.getValue(BlockStateProperties.OPEN);
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
}
