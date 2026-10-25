package com.craftdistrict.network;

import com.craftdistrict.menu.AssemblyStationMenu;
import com.craftdistrict.assembly.AssemblyCrafting;
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

public final class AssemblyNetworking {
    private static final ResourceLocation CRAFT = ResourceLocation.tryParse("craftdistrict:assembly_craft");
    private AssemblyNetworking() { }

    private record Request(int container, long session, int sequence, ResourceLocation recipe, int operations)
            //? if >=1.21 {
            implements CustomPacketPayload
            //?}
    {
        private static Request read(FriendlyByteBuf buffer) {
            if (buffer.readableBytes() > 512) throw new IllegalArgumentException("Assembly request too large");
            int container = buffer.readVarInt(); long session = buffer.readLong(); int sequence = buffer.readVarInt();
            ResourceLocation recipe = ResourceLocation.tryParse(buffer.readUtf(256));
            int operations = buffer.readVarInt();
            if (container < 0 || sequence < 0 || recipe == null || !AssemblyCrafting.validQuantity(operations) || buffer.isReadable())
                throw new IllegalArgumentException("Invalid assembly request");
            return new Request(container, session, sequence, recipe, operations);
        }
        private void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(container); buffer.writeLong(session); buffer.writeVarInt(sequence);
            buffer.writeUtf(recipe.toString(), 256); buffer.writeVarInt(operations);
        }
        //? if >=1.21 {
        private static final Type<Request> TYPE = new Type<>(CRAFT);
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
                context.queue(() -> handle(context.getPlayer(), request.container, request.session, request.sequence, request.recipe, request.operations)));
        //?} else {
        /*
        NetworkManager.registerReceiver(NetworkManager.c2s(), CRAFT, (buffer, context) -> {
            try {
                Request request = Request.read(buffer);
                context.queue(() -> {
                    handle(context.getPlayer(), request.container, request.session, request.sequence, request.recipe, request.operations);
                });
            } catch (RuntimeException ignored) {
                // Malformed requests never reach inventory code.
            }
        });
        *///?}
    }

    private static void handle(Player sender, int container, long session, int sequence, ResourceLocation recipe, int operations) {
        if (sender instanceof ServerPlayer player && player.containerMenu instanceof AssemblyStationMenu menu
                && menu.containerId == container) menu.craft(player, session, sequence, recipe, operations);
    }

    public static void request(Player player, int container, long session, int sequence, ResourceLocation recipe, int operations) {
        Request request = new Request(container, session, sequence, recipe, operations);
        //? if >=1.21 {
        NetworkManager.sendToServer(request);
        //?} else {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        request.write(buffer);
        NetworkManager.sendToServer(CRAFT, buffer);
        *///?}
    }
}
