package com.craftdistrict.mixin.client;

import com.craftdistrict.client.DecorationBreakParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restricts the destruction burst only; mining-hit particles and other blocks retain vanilla behavior. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow private ClientLevel level;
    @Shadow public abstract void add(Particle particle);
    @Unique private DecorationBreakParticles craftdistrict$breakParticles;

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void craftdistrict$clearBurstIdentity(ClientLevel nextLevel, CallbackInfo ci) {
        craftdistrict$breakParticles = null;
    }

    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    private void craftdistrict$sampleDestruction(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!DecorationBreakParticles.handles(state)) return;
        ci.cancel();
        //? if >=1.21 {
        if (!state.shouldSpawnTerrainParticles()) return;
        //?} else {
        /*if (!state.shouldSpawnParticlesOnBreak()) return;
        *///?}
        if (craftdistrict$breakParticles == null) craftdistrict$breakParticles = new DecorationBreakParticles();
        if (craftdistrict$breakParticles.duplicate(level, level.getGameTime(), state, pos)) return;
        DecorationBreakParticles.sample(state, state.getShape(level, pos), (x, y, z, vx, vy, vz) ->
                add(new TerrainParticle(level, pos.getX() + x, pos.getY() + y, pos.getZ() + z,
                        vx, vy, vz, state, pos)));
    }
}
