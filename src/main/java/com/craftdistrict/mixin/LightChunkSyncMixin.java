package com.craftdistrict.mixin;

import com.craftdistrict.network.LightDyeNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
//? if >=1.21 {
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.21 {
@Mixin(ServerCommonPacketListenerImpl.class)
//?} else {
/*@Mixin(ServerGamePacketListenerImpl.class)
*///?}
public abstract class LightChunkSyncMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("TAIL"))
    private void craftdistrict$lightPalette(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundLevelChunkWithLightPacket chunk
                && (Object)this instanceof ServerGamePacketListenerImpl game)
            LightDyeNetworking.chunkSent(game.player, chunk.getX(), chunk.getZ());
    }
}
