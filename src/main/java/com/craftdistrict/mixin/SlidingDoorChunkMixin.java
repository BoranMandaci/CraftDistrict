package com.craftdistrict.mixin;

import com.craftdistrict.door.SlidingDoorSensors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class SlidingDoorChunkMixin {
    @Inject(method="unload(Lnet/minecraft/world/level/chunk/LevelChunk;)V",at=@At("HEAD"))
    private void craftdistrict$forgetDoorSensors(LevelChunk chunk,CallbackInfo ci){SlidingDoorSensors.unload((ServerLevel)(Object)this,chunk.getPos().toLong());}
}
