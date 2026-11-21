package com.craftdistrict.access;

import com.craftdistrict.block.AccessDoorMotion;
import com.craftdistrict.network.AccessControlNetworking;
import dev.architectury.event.events.common.TickEvent;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.ref.WeakReference;

/** The client cache suppresses local door prediction. Only the saved server index grants or denies access. */
public final class AccessDoorSecurity {
    private static final Long2ObjectOpenHashMap<LongOpenHashSet> CLIENT_LOCKS = new Long2ObjectOpenHashMap<>();
    private static WeakReference<Level> clientLevel = new WeakReference<>(null);

    private AccessDoorSecurity() { }

    public static void register() { TickEvent.SERVER_LEVEL_POST.register(level -> AccessControlData.get(level).tick()); }

    public static boolean secured(Level level, BlockPos pos, BlockState state) {
        if (state == null) return false;
        if (level instanceof ServerLevel server) return AccessControlData.get(server).securedAt(pos, state) != null;
        if (clientLevel.get() != level) return false;
        LongOpenHashSet entries = CLIENT_LOCKS.get(AccessControlData.chunk(pos));
        return entries != null && entries.contains(pos.asLong());
    }

    public static InteractionResult denied(Level level, Player player) {
        if (player instanceof ServerPlayer server) server.displayClientMessage(Component.translatable("message.craftdistrict.access_control.locked"), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static boolean authorize(ServerPlayer player, AccessControlData.Reader reader) {
        ServerLevel level = player.serverLevel();
        AccessControlData.Door door = reader.link();
        if (door == null || !door.secured || reader.linkStatus(level) != AccessControlData.LinkStatus.LINKED) return false;
        for (BlockPos part : door.target.parts()) {
            if (!level.getWorldBorder().isWithinBounds(part) || !level.mayInteract(player, part)) return false;
        }
        if (!AccessDoorMotion.open(level, door.target, player)) return false;
        AccessControlData.get(level).hold(door, level.getGameTime() + reader.seconds() * 20L);
        return true;
    }

    /** Only the existing swept sliding-door check may interrupt a moving close. */
    public static void reopenForSafety(ServerLevel level, BlockPos root, BlockState state) {
        AccessControlData data = AccessControlData.get(level);
        AccessControlData.Door door = data.securedAt(root, state);
        if (door != null && data.ready(door) && AccessDoorMotion.open(level, door.target, null))
            data.hold(door, level.getGameTime() + 10);
    }

    public static void chunkSent(ServerPlayer player, int x, int z) {
        ServerLevel level = player.serverLevel();
        if (!level.getServer().isSameThread()) {
            level.getServer().execute(() -> { if (player.serverLevel() == level) chunkSent(player, x, z); });
            return;
        }
        long chunk = ChunkPos.asLong(x, z);
        AccessControlData data = AccessControlData.get(level);
        data.chunkReady(chunk);
        long[] locked = data.locksInChunk(chunk);
        if (locked.length != 0) AccessControlNetworking.sendLocks(player, chunk, locked);
    }

    public static void receiveLocks(Level level, ResourceLocation dimension, long chunk, boolean reset, long[] positions) {
        if (!level.dimension().location().equals(dimension)) return;
        if (clientLevel.get() != level) {
            CLIENT_LOCKS.clear();
            clientLevel = new WeakReference<>(level);
        }
        if (reset) CLIENT_LOCKS.remove(chunk);
        if (positions.length == 0) return;
        LongOpenHashSet entries = CLIENT_LOCKS.computeIfAbsent(chunk, ignored -> new LongOpenHashSet());
        for (long pos : positions) entries.add(pos);
    }

    public static void forgetChunk(Level level, long chunk) {
        if (clientLevel.get() == level) CLIENT_LOCKS.remove(chunk);
    }

    public static void clearClient() { CLIENT_LOCKS.clear(); clientLevel.clear(); }
}
