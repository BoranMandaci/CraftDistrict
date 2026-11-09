package com.craftdistrict.network;

import com.craftdistrict.menu.LightColorMenu;
import com.craftdistrict.lighting.LightMode;
import com.craftdistrict.lighting.LightEffect;
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

public final class LightColorNetworking {
    private static final ResourceLocation ID = ResourceLocation.tryParse("craftdistrict:light_color");
    private LightColorNetworking() { }

    private record Request(int container, long session, int sequence, int color, int expected,
                           int mode, int effect, int expectedMode, int expectedEffect, boolean power, boolean expectedPower)
            //? if >=1.21 {
            implements CustomPacketPayload
            //?}
    {
        private static Request read(FriendlyByteBuf buffer) {
            if (buffer.readableBytes() > 64) throw new IllegalArgumentException("Light color request too large");
            Request request = new Request(buffer.readVarInt(), buffer.readLong(), buffer.readVarInt(), buffer.readByte(), buffer.readByte(),
                    buffer.readByte(), buffer.readByte(), buffer.readByte(), buffer.readByte(), buffer.readBoolean(), buffer.readBoolean());
            if (request.container < 0 || request.sequence < 0 || request.color < -1 || request.color > 15
                    || request.expected < -1 || request.expected > 15 || !LightMode.valid(request.mode)
                    || !LightEffect.valid(request.effect) || !LightMode.valid(request.expectedMode)
                    || !LightEffect.valid(request.expectedEffect) || buffer.isReadable())
                throw new IllegalArgumentException("Invalid light settings request");
            return request;
        }
        private void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(container); buffer.writeLong(session); buffer.writeVarInt(sequence);
            buffer.writeByte(color); buffer.writeByte(expected);
            buffer.writeByte(mode); buffer.writeByte(effect); buffer.writeByte(expectedMode); buffer.writeByte(expectedEffect);
            buffer.writeBoolean(power); buffer.writeBoolean(expectedPower);
        }
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
        NetworkManager.registerReceiver(NetworkManager.c2s(), Request.TYPE, Request.CODEC, (request, context) ->
                context.queue(() -> handle(context.getPlayer(), request)));
        //?} else {
        /*NetworkManager.registerReceiver(NetworkManager.c2s(), ID, (buffer, context) -> {
            try {
                Request request = Request.read(buffer);
                context.queue(() -> handle(context.getPlayer(), request));
            } catch (RuntimeException ignored) {
                // Invalid requests never reach palette or inventory code.
            }
        });
        *///?}
    }

    private static void handle(Player sender, Request request) {
        if (sender instanceof ServerPlayer player && player.containerMenu instanceof LightColorMenu menu
                && menu.containerId == request.container)
            menu.confirm(player, request.session, request.sequence, request.color, request.expected,
                    request.mode, request.effect, request.expectedMode, request.expectedEffect, request.power, request.expectedPower);
    }

    public static void request(int container, long session, int sequence, int color, int expected,
                               int mode, int effect, int expectedMode, int expectedEffect, boolean power, boolean expectedPower) {
        Request request = new Request(container, session, sequence, color, expected, mode, effect, expectedMode, expectedEffect, power, expectedPower);
        //? if >=1.21 {
        NetworkManager.sendToServer(request);
        //?} else {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        request.write(buffer); NetworkManager.sendToServer(ID, buffer);
        *///?}
    }
}
