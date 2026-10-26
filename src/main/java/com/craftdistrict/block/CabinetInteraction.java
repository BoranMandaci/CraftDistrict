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
        level.playSound(null, pos, open ? openSound : closeSound, SoundSource.BLOCKS, .6F, 1.1F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.CONSUME;
    }
}
