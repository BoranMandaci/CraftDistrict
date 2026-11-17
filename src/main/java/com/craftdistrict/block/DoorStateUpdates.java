package com.craftdistrict.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Install the whole entrance before rendering/network notifications or prediction acknowledgements. */
final class DoorStateUpdates {
    private static final int QUIET = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_INVISIBLE;
    private static final int NOTIFY = Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE | Block.UPDATE_KNOWN_SHAPE;

    private DoorStateUpdates() { }

    static boolean commit(Level level, BlockPos[] positions, BlockState[] before, BlockState[] after) {
        if (positions.length != before.length || positions.length != after.length)
            throw new IllegalArgumentException("Mismatched door transaction arrays");
        for (int i = 0; i < positions.length; i++)
            if (!level.hasChunkAt(positions[i]) || level.getBlockState(positions[i]) != before[i]) return false;
        boolean valid = true;
        int written = 0;
        for (int i = 0; i < positions.length; i++) {
            if (level.getBlockState(positions[i]) != before[i]) { valid = false; break; }
            written = i + 1;
            if (before[i] != after[i] && !level.setBlock(positions[i], after[i], QUIET)) {
                valid = false; break;
            }
        }
        if (valid) for (int i = 0; i < positions.length; i++)
            if (level.getBlockState(positions[i]) != after[i]) { valid = false; break; }
        if (!valid) for (int i = written - 1; i >= 0; i--)
            if (before[i] != after[i] && level.getBlockState(positions[i]) == after[i])
                level.setBlock(positions[i], before[i], QUIET);

        // Vanilla prediction tracks each setBlock above. Invalidate only after all halves
        // contain their final states, so an immediate rebuild cannot capture a half-open door.
        for (int i = 0; i < positions.length; i++) {
            BlockState current = level.getBlockState(positions[i]);
            if (before[i] != current || !valid && before[i] != after[i])
                level.sendBlockUpdated(positions[i], before[i], current, NOTIFY);
        }
        if (level instanceof ServerLevel server) {
            Map<ChunkPos, List<ClientboundBlockUpdatePacket>> chunks = new LinkedHashMap<>();
            for (int i = 0; i < positions.length; i++) {
                BlockState current = level.getBlockState(positions[i]);
                if (before[i] != current || !valid && before[i] != after[i])
                    chunks.computeIfAbsent(new ChunkPos(positions[i]), ignored -> new ArrayList<>())
                            .add(new ClientboundBlockUpdatePacket(positions[i], current));
            }
            Map<ServerPlayer, List<ClientboundBlockUpdatePacket>> recipients = new LinkedHashMap<>();
            chunks.forEach((chunk, packets) -> {
                for (ServerPlayer player : server.getChunkSource().chunkMap.getPlayers(chunk, false))
                    recipients.computeIfAbsent(player, ignored -> new ArrayList<>()).addAll(packets);
            });
            // Confirm every predicted half before the vanilla interaction acknowledgement.
            // Bundling also keeps observers from processing the panels in separate client tasks.
            recipients.forEach((player, packets) ->
                    player.connection.send(new ClientboundBundlePacket(new ArrayList<>(packets))));
        }
        return valid;
    }
}
