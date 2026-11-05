package com.craftdistrict.client;

import com.craftdistrict.network.LightDyeNetworking;
import com.craftdistrict.registry.DecorationCollection;
import com.craftdistrict.registry.CraftDistrictBlocks;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** Vanilla quad tinting only; the ordinary Minecraft light engine is unchanged. */
public final class LightingColors {
    // Render workers only read immutable chunk palettes; packets publish a new copy.
    private static final Map<ResourceLocation, ConcurrentHashMap<Long, Long2ByteOpenHashMap>> COLORS = new ConcurrentHashMap<>();
    private LightingColors() { }

    public static void init() {
        Block[] lights = Stream.concat(DecorationCollection.LIGHTING.items().stream()
                .map(item -> ((BlockItem)item).getBlock()),
                Stream.of(CraftDistrictBlocks.Hospital.SURGICAL_LIGHT.get())).toArray(Block[]::new);
        ColorHandlerRegistry.registerBlockColors((state, level, pos, tint) -> color(tint,
                level == null || pos == null ? -1 : dye(pos)), lights);
        ColorHandlerRegistry.registerItemColors((stack, tint) -> color(tint, -1), lights);
        LightDyeNetworking.receiveWith(LightingColors::receive);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> COLORS.clear());
    }
    public static void clear() { COLORS.clear(); }

    private static int dye(BlockPos pos) {
        var level = Minecraft.getInstance().level;
        if (level == null) return -1;
        var dimension = COLORS.get(level.dimension().location());
        if (dimension == null) return -1;
        var chunk = dimension.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return chunk == null ? -1 : chunk.get(pos.asLong());
    }

    public static int color(int tint, int dye) {
        if (tint < 0 || (tint & 0x1000000) == 0) return -1;
        int rgb = tint & 0xFFFFFF;
        // Warning/sign faces keep their original multicolor artwork, even if a stale palette exists.
        if ((tint & 0x4000000) == 0 && dye >= 0 && dye <= 15) {
            //? if >=1.21 {
            rgb = DyeColor.byId(dye).getTextureDiffuseColor();
            //?} else {
            /*float[] channels = DyeColor.byId(dye).getTextureDiffuseColors();
            rgb = Math.round(channels[0]*255) << 16 | Math.round(channels[1]*255) << 8 | Math.round(channels[2]*255);
            *///?}
        }
        if ((tint & 0x2000000) != 0) {
            rgb = (int)((rgb >> 16 & 255)*.30) << 16 | (int)((rgb >> 8 & 255)*.30) << 8 | (int)((rgb & 255)*.30);
        }
        return 0xFF000000 | rgb;
    }

    public static void receive(LightDyeNetworking.Palette packet) {
        var dimension = COLORS.computeIfAbsent(packet.dimension(), key -> new ConcurrentHashMap<>());
        long key = ChunkPos.asLong(packet.x(), packet.z());
        var previous = dimension.get(key);
        var chunk = packet.reset() || previous == null ? new Long2ByteOpenHashMap() : previous.clone();
        chunk.defaultReturnValue((byte)-1);
        for (var entry : packet.colors()) {
            if (entry.dye() < 0) chunk.remove(entry.pos());
            else chunk.put(entry.pos(), (byte)entry.dye());
        }
        if (chunk.isEmpty()) dimension.remove(key);else dimension.put(key, chunk);
        if (dimension.isEmpty()) COLORS.remove(packet.dimension());
        if (packet.reset() && previous != null)
            for (long pos : previous.keySet()) dirty(packet.dimension(), BlockPos.of(pos));
        for (var entry : packet.colors()) dirty(packet.dimension(), BlockPos.of(entry.pos()));
    }

    private static void dirty(ResourceLocation dimension, BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.level.dimension().location().equals(dimension))
            client.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static void forget(ChunkPos pos) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var dimension = COLORS.get(level.dimension().location());
        if (dimension != null) {
            dimension.remove(pos.toLong());
            if (dimension.isEmpty()) COLORS.remove(level.dimension().location());
        }
    }
}
