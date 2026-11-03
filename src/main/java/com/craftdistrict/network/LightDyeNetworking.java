package com.craftdistrict.network;

import com.craftdistrict.lighting.LightDyeData;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
//? if >=1.21 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

/** Only painted positions are synchronized, on chunk delivery or a real dye change. */
public final class LightDyeNetworking {
    private static final ResourceLocation ID = ResourceLocation.tryParse("craftdistrict:light_dyes");
    private static final int LIMIT = 512;
    private static Consumer<Palette> receiver = packet -> { };
    private LightDyeNetworking() { }

    public record Color(long pos, int dye) { }
    public record Palette(ResourceLocation dimension, int x, int z, boolean reset, List<Color> colors)
            //? if >=1.21 {
            implements CustomPacketPayload
            //?}
    {
        public static Palette read(FriendlyByteBuf buffer) {
            ResourceLocation dimension = ResourceLocation.tryParse(buffer.readUtf(256));
            int x = buffer.readInt(), z = buffer.readInt(); boolean reset = buffer.readBoolean();
            int count = buffer.readVarInt();
            if (dimension == null || count < 0 || count > LIMIT) throw new IllegalArgumentException("Invalid light palette");
            List<Color> colors = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                long packed = buffer.readLong(); int dye = buffer.readByte(); BlockPos pos = BlockPos.of(packed);
                if (dye < -1 || dye > 15 || pos.getX() >> 4 != x || pos.getZ() >> 4 != z)
                    throw new IllegalArgumentException("Invalid light palette position/color");
                colors.add(new Color(packed, dye));
            }
            if (buffer.isReadable()) throw new IllegalArgumentException("Trailing light palette data");
            return new Palette(dimension, x, z, reset, List.copyOf(colors));
        }
        public void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(dimension.toString(), 256);buffer.writeInt(x);buffer.writeInt(z);buffer.writeBoolean(reset);
            buffer.writeVarInt(colors.size());
            for (Color color : colors) { buffer.writeLong(color.pos);buffer.writeByte(color.dye); }
        }
        //? if >=1.21 {
        private static final Type<Palette> TYPE = new Type<>(ID);
        private static final StreamCodec<RegistryFriendlyByteBuf, Palette> CODEC = new StreamCodec<>() {
            @Override public Palette decode(RegistryFriendlyByteBuf buffer) { return read(buffer); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Palette packet) { packet.write(buffer); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        //?}
    }

    public static void register() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            //? if >=1.21 {
            NetworkManager.registerReceiver(NetworkManager.s2c(), Palette.TYPE, Palette.CODEC,
                    (packet, context) -> context.queue(() -> receiver.accept(packet)));
            //?} else {
            /*NetworkManager.registerReceiver(NetworkManager.s2c(), ID, (buffer, context) -> {
                Palette packet = Palette.read(buffer);
                context.queue(() -> receiver.accept(packet));
            });
            *///?}
        } else {
            //? if >=1.21 {
            NetworkManager.registerS2CPayloadType(Palette.TYPE, Palette.CODEC);
            //?}
        }
    }

    public static void receiveWith(Consumer<Palette> handler) { receiver = handler; }

    private static void send(Iterable<ServerPlayer> players, Palette packet) {
        if (!players.iterator().hasNext()) return;
        //? if >=1.21 {
        NetworkManager.sendToPlayers(players, packet);
        //?} else {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(buffer);
        NetworkManager.sendToPlayers(players, ID, buffer);
        *///?}
    }

    public static void chunkSent(ServerPlayer player, int x, int z) {
        ServerLevel level = player.serverLevel();
        if (!level.getServer().isSameThread()) {
            level.getServer().execute(() -> {
                if (player.serverLevel() == level) chunkSent(player, x, z);
            });
            return;
        }
        var colors = LightDyeData.get(level).snapshot(new ChunkPos(x, z));
        // Empty chunks need no packet: the client discards a chunk's palette when unloading it.
        if (colors.isEmpty()) return;
        List<Color> batch = new ArrayList<>(LIMIT);boolean reset = true;
        for (var entry : colors.long2ByteEntrySet()) {
            batch.add(new Color(entry.getLongKey(), entry.getByteValue()));
            if (batch.size() == LIMIT) {
                send(List.of(player), new Palette(level.dimension().location(), x, z, reset, List.copyOf(batch)));
                reset = false;batch.clear();
            }
        }
        if (!batch.isEmpty()) send(List.of(player), new Palette(level.dimension().location(), x, z, reset, List.copyOf(batch)));
    }

    public static void changed(ServerLevel level, List<BlockPos> positions) {
        LightDyeData data = LightDyeData.get(level);
        if (positions.size() == 1) {
            BlockPos pos = positions.get(0);
            ChunkPos chunk = new ChunkPos(pos);
            send(level.getChunkSource().chunkMap.getPlayers(chunk, false), new Palette(level.dimension().location(),
                    chunk.x, chunk.z, false, List.of(new Color(pos.asLong(), data.color(pos)))));
            return;
        }
        var updates = new LinkedHashMap<ChunkPos, List<Color>>();
        for (BlockPos pos : positions) {
            ChunkPos chunk = new ChunkPos(pos);
            updates.computeIfAbsent(chunk, ignored -> new ArrayList<>(3)).add(new Color(pos.asLong(), data.color(pos)));
        }
        updates.forEach((chunk, colors) -> send(level.getChunkSource().chunkMap.getPlayers(chunk, false),
                new Palette(level.dimension().location(), chunk.x, chunk.z, false, List.copyOf(colors))));
    }
}
