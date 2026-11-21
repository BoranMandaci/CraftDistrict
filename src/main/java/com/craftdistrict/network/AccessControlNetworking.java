package com.craftdistrict.network;

import com.craftdistrict.access.AccessControl;
import com.craftdistrict.access.AccessDoorSecurity;
import com.craftdistrict.item.AccessCardItem;
import com.craftdistrict.menu.AccessCardMenu;
import com.craftdistrict.menu.CardReaderMenu;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
//? if >=1.21 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

import java.util.Arrays;
import java.util.List;

/** Menu mutations carry no card IDs, door coordinates or reader coordinates. */
public final class AccessControlNetworking {
    private static final ResourceLocation REQUEST = ResourceLocation.tryParse("craftdistrict:access_control_request");
    private static final ResourceLocation UPDATE = ResourceLocation.tryParse("craftdistrict:access_control_update");
    private static final int LOCKS_PER_PACKET = 512;

    public enum Action { SAVE_CARD, PAGE, ENROLLMENT, ENABLED, DURATION, UNLINK, ADMINISTRATOR, DOOR_ACCESS, REMOVE }

    private AccessControlNetworking() { }

    private record Request(int container, long session, int version, Action action, int value, String name)
            //? if >=1.21 {
            implements CustomPacketPayload
            //?}
    {
        private static Request read(FriendlyByteBuf buffer) {
            if (buffer.readableBytes() > 128) throw new IllegalArgumentException("Oversized access control request");
            int container = buffer.readVarInt();
            long session = buffer.readLong();
            int version = buffer.readVarInt();
            Action action = buffer.readEnum(Action.class);
            int value = buffer.readVarInt();
            String name = action == Action.SAVE_CARD ? buffer.readUtf(AccessCardItem.MAX_NAME) : "";
            if (container < 0 || buffer.isReadable()) throw new IllegalArgumentException("Invalid access control request");
            return new Request(container, session, version, action, value, name);
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(container);
            buffer.writeLong(session);
            buffer.writeVarInt(version).writeEnum(action).writeVarInt(value);
            if (action == Action.SAVE_CARD) buffer.writeUtf(name, AccessCardItem.MAX_NAME);
        }

        //? if >=1.21 {
        private static final Type<Request> TYPE = new Type<>(REQUEST);
        private static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> p.write(b), Request::read);
        @Override public Type<Request> type() { return TYPE; }
        //?}
    }

    private record Locks(ResourceLocation dimension, long chunk, boolean reset, long[] blocks) {
        private static Locks read(FriendlyByteBuf buffer) {
            ResourceLocation dimension = buffer.readResourceLocation();
            long chunk = buffer.readLong();
            boolean reset = buffer.readBoolean();
            int count = buffer.readVarInt();
            if (count < 0 || count > LOCKS_PER_PACKET) throw new IllegalArgumentException("Invalid door lock packet size");
            long[] blocks = new long[count];
            for (int i = 0; i < count; i++) {
                blocks[i] = buffer.readLong();
                BlockPos pos = BlockPos.of(blocks[i]);
                if (pos.getX() >> 4 != ChunkPos.getX(chunk) || pos.getZ() >> 4 != ChunkPos.getZ(chunk))
                    throw new IllegalArgumentException("Invalid door lock position");
            }
            return new Locks(dimension, chunk, reset, blocks);
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(dimension);
            buffer.writeLong(chunk);
            buffer.writeBoolean(reset);
            buffer.writeVarInt(blocks.length);
            for (long pos : blocks) buffer.writeLong(pos);
        }
    }

    private record Update(int container, long session, CardReaderMenu.View view, int selectionTicks, Locks locks)
            //? if >=1.21 {
            implements CustomPacketPayload
            //?}
    {
        private static Update read(FriendlyByteBuf buffer) {
            return switch (buffer.readUnsignedByte()) {
                case 0 -> new Update(0, 0, null, buffer.readVarInt(), null);
                case 1 -> new Update(buffer.readVarInt(), buffer.readLong(), CardReaderMenu.View.read(buffer), 0, null);
                case 2 -> new Update(0, 0, null, 0, Locks.read(buffer));
                default -> throw new IllegalArgumentException("Invalid access control update");
            };
        }

        private void write(FriendlyByteBuf buffer) {
            if (locks != null) {
                buffer.writeByte(2);
                locks.write(buffer);
            } else if (view == null) {
                buffer.writeByte(0);
                buffer.writeVarInt(selectionTicks);
            } else {
                buffer.writeByte(1);
                buffer.writeVarInt(container);
                buffer.writeLong(session);
                view.write(buffer);
            }
        }

        //? if >=1.21 {
        private static final Type<Update> TYPE = new Type<>(UPDATE);
        private static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC = StreamCodec.of((b, p) -> p.write(b), Update::read);
        @Override public Type<Update> type() { return TYPE; }
        //?}
    }

    public static void register() {
        //? if >=1.21 {
        NetworkManager.registerReceiver(NetworkManager.c2s(), Request.TYPE, Request.CODEC,
                (payload, context) -> context.queue(() -> handle(context.getPlayer(), payload)));
        if (Platform.getEnvironment() == Env.CLIENT) NetworkManager.registerReceiver(NetworkManager.s2c(), Update.TYPE, Update.CODEC,
                (payload, context) -> context.queue(() -> receive(context.getPlayer(), payload)));
        else NetworkManager.registerS2CPayloadType(Update.TYPE, Update.CODEC);
        //?} else {
        /*NetworkManager.registerReceiver(NetworkManager.c2s(), REQUEST, (buffer, context) -> {
            Request payload;
            try { payload = Request.read(buffer); }
            catch (RuntimeException invalid) { return; }
            context.queue(() -> handle(context.getPlayer(), payload));
        });
        if (Platform.getEnvironment() == Env.CLIENT) NetworkManager.registerReceiver(NetworkManager.s2c(), UPDATE, (buffer, context) -> {
            Update payload = Update.read(buffer);
            context.queue(() -> receive(context.getPlayer(), payload));
        });
        *///?}
    }

    private static void handle(Player sender, Request payload) {
        if (!(sender instanceof ServerPlayer player) || player.containerMenu.containerId != payload.container()) return;
        if (payload.action() == Action.SAVE_CARD && player.containerMenu instanceof AccessCardMenu card)
            card.save(player, payload.session(), payload.name(), payload.value());
        else if (payload.action() != Action.SAVE_CARD && player.containerMenu instanceof CardReaderMenu reader)
            reader.handle(player, payload.session(), payload.version(), payload.action(), payload.value());
    }

    private static void receive(Player player, Update payload) {
        if (player == null || !player.level().isClientSide) return;
        if (payload.locks() != null) {
            Locks locks = payload.locks();
            AccessDoorSecurity.receiveLocks(player.level(), locks.dimension(), locks.chunk(), locks.reset(), locks.blocks());
        } else if (payload.view() == null) AccessControl.receiveSelection(player, payload.selectionTicks());
        else if (player.containerMenu instanceof CardReaderMenu reader && reader.containerId == payload.container()
                && reader.session() == payload.session()) reader.receive(payload.view());
    }

    public static void saveCard(AccessCardMenu menu, String name, int color) {
        send(new Request(menu.containerId, menu.session(), 0, Action.SAVE_CARD, color, name));
    }

    public static void editReader(CardReaderMenu menu, Action action, int value) {
        send(new Request(menu.containerId, menu.session(), menu.view().version(), action, value, ""));
    }

    private static void send(Request payload) {
        //? if >=1.21 {
        NetworkManager.sendToServer(payload);
        //?} else {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buffer);
        NetworkManager.sendToServer(REQUEST, buffer);
        *///?}
    }

    public static void sendSelection(ServerPlayer player, int ticks) { send(List.of(player), new Update(0, 0, null, ticks, null)); }

    public static void sendView(ServerPlayer player, CardReaderMenu menu, CardReaderMenu.View view) {
        send(List.of(player), new Update(menu.containerId, menu.session(), view, 0, null));
    }

    public static void sendLocks(ServerPlayer player, long chunk, long[] positions) {
        sendLocks(List.of(player), player.serverLevel(), chunk, positions);
    }

    public static void syncLocks(ServerLevel level, long chunk, long[] positions) {
        var players = level.getChunkSource().chunkMap.getPlayers(new ChunkPos(chunk), false);
        if (!players.isEmpty()) sendLocks(players, level, chunk, positions);
    }

    private static void sendLocks(Iterable<ServerPlayer> players, ServerLevel level, long chunk, long[] positions) {
        for (int i = 0; i < Math.max(1, positions.length); i += LOCKS_PER_PACKET) {
            long[] batch = positions.length <= LOCKS_PER_PACKET ? positions : Arrays.copyOfRange(positions, i, Math.min(positions.length, i + LOCKS_PER_PACKET));
            send(players, new Update(0, 0, null, 0, new Locks(level.dimension().location(), chunk, i == 0, batch)));
        }
    }

    private static void send(Iterable<ServerPlayer> players, Update payload) {
        //? if >=1.21 {
        NetworkManager.sendToPlayers(players, payload);
        //?} else {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buffer);
        NetworkManager.sendToPlayers(players, UPDATE, buffer);
        *///?}
    }
}
