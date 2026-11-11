package com.craftdistrict.network;

import com.craftdistrict.block.RoomNumberPlaqueBlockEntity;
import com.craftdistrict.menu.RoomNumberMenu;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
//? if >=1.21 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

public final class RoomNumberNetworking {
    private static final ResourceLocation ID = ResourceLocation.tryParse("craftdistrict:room_number");
    private RoomNumberNetworking() { }
    private record Request(int container, long session, String number, boolean reversed)
            //? if >=1.21 {
            implements CustomPacketPayload
            //?}
    {
        private static Request read(FriendlyByteBuf buffer) {
            if (buffer.readableBytes() > 32) throw new IllegalArgumentException("Room number request too large");
            Request request = new Request(buffer.readVarInt(), buffer.readLong(), buffer.readUtf(9), buffer.readBoolean());
            if (request.container < 0 || !(RoomNumberPlaqueBlockEntity.valid(0, request.number) || RoomNumberPlaqueBlockEntity.valid(1, request.number)
                    || RoomNumberPlaqueBlockEntity.valid(2, request.number)) || buffer.isReadable())
                throw new IllegalArgumentException("Invalid room number request");
            return request;
        }
        private void write(FriendlyByteBuf buffer) { buffer.writeVarInt(container); buffer.writeLong(session); buffer.writeUtf(number, 9); buffer.writeBoolean(reversed); }
        //? if >=1.21 {
        private static final Type<Request> TYPE = new Type<>(ID);
        private static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = new StreamCodec<>() {
            @Override public Request decode(RegistryFriendlyByteBuf buffer) { return read(buffer); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Request request) { request.write(buffer); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        //?}
    }
    public static void register() {
        //? if >=1.21 {
        NetworkManager.registerReceiver(NetworkManager.c2s(), Request.TYPE, Request.CODEC, (request, context) -> context.queue(() -> handle(context.getPlayer(), request)));
        //?} else {
        /*NetworkManager.registerReceiver(NetworkManager.c2s(), ID, (buffer, context) -> {
            try { Request request = Request.read(buffer); context.queue(() -> handle(context.getPlayer(), request)); }
            catch (RuntimeException ignored) { }
        });
        *///?}
    }
    private static void handle(Player sender, Request request) {
        if (sender instanceof ServerPlayer player && player.containerMenu instanceof RoomNumberMenu menu && menu.containerId == request.container)
            menu.confirm(player, request.session, request.number, request.reversed);
    }
    public static void request(int container, long session, String number, boolean reversed) {
        Request request = new Request(container, session, number, reversed);
        //? if >=1.21 {
        NetworkManager.sendToServer(request);
        //?} else {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer()); request.write(buffer); NetworkManager.sendToServer(ID, buffer);
        *///?}
    }
}
