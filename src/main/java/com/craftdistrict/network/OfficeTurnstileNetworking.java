package com.craftdistrict.network;

import com.craftdistrict.block.OfficeTurnstileBlock;
import com.craftdistrict.block.OfficeTurnstileBlockEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
//? if >=1.21 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

public final class OfficeTurnstileNetworking {
    private static final ResourceLocation ID = ResourceLocation.tryParse("craftdistrict:office_turnstile_push");

    private OfficeTurnstileNetworking() { }

    private record Push(BlockPos pos, int direction)
            //? if >=1.21 {
            implements CustomPacketPayload
            //?}
    {
        private static Push read(FriendlyByteBuf buffer) {
            if (buffer.readableBytes() != 9) throw new IllegalArgumentException("Invalid turnstile request size");
            BlockPos pos = buffer.readBlockPos();
            int direction = buffer.readByte();
            if (direction != 1 && direction != -1) throw new IllegalArgumentException("Invalid turnstile push direction");
            return new Push(pos, direction);
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeByte(direction);
        }

        //? if >=1.21 {
        private static final Type<Push> TYPE = new Type<>(ID);
        private static final StreamCodec<RegistryFriendlyByteBuf, Push> CODEC = new StreamCodec<>() {
            @Override public Push decode(RegistryFriendlyByteBuf buffer) { return read(buffer); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Push push) { push.write(buffer); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        //?}
    }

    public static void register() {
        //? if >=1.21 {
        NetworkManager.registerReceiver(NetworkManager.c2s(), Push.TYPE, Push.CODEC,
                (push, context) -> context.queue(() -> handle(context.getPlayer(), push)));
        //?} else {
        /*NetworkManager.registerReceiver(NetworkManager.c2s(), ID, (buffer, context) -> {
            try {
                Push push = Push.read(buffer);
                context.queue(() -> handle(context.getPlayer(), push));
            } catch (IllegalArgumentException ignored) { }
        });
        *///?}
    }

    private static void handle(Player sender, Push push) {
        if (!(sender instanceof ServerPlayer player) || !OfficeTurnstileBlock.canPush(player)) return;
        BlockPos pos = push.pos();
        if (player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 4) return;
        var level = player.serverLevel();
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return;
        var state = chunk.getBlockState(pos);
        if (state.getBlock() instanceof OfficeTurnstileBlock gate && gate.approach(state, pos, player) == push.direction()
                && chunk.getBlockEntity(pos) instanceof OfficeTurnstileBlockEntity turnstile)
            turnstile.tryStart(level, player, push.direction());
    }

    public static void push(BlockPos pos, int direction) {
        Push push = new Push(pos, direction);
        //? if >=1.21 {
        NetworkManager.sendToServer(push);
        //?} else {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer(9));
        push.write(buffer);
        NetworkManager.sendToServer(ID, buffer);
        *///?}
    }
}
