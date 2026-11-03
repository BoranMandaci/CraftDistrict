package com.craftdistrict.mixin.client;

import com.craftdistrict.client.LightingColors;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class LightChunkCacheMixin {
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void craftdistrict$newLightPalette(ClientboundLoginPacket packet, CallbackInfo ci) {
        LightingColors.clear();
    }
    @Inject(method = "handleForgetLevelChunk", at = @At("TAIL"))
    private void craftdistrict$forgetLightColors(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        //? if >=1.21 {
        LightingColors.forget(packet.pos());
        //?} else {
        /*LightingColors.forget(new ChunkPos(packet.getX(), packet.getZ()));
        *///?}
    }
}
