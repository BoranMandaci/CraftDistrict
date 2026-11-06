package com.craftdistrict.mixin;

import com.craftdistrict.entity.MedicalResting;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MedicalRestingPlayerMixin {
    // Forge replaces the day check with a sleep event; all targets retain this wake call.
    // Redirect only tick's automatic wake-up, leaving manual exits and removal cleanup intact.
    @Redirect(method = "tick()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;stopSleepInBed(ZZ)V"), require = 1, allow = 1)
    private void craftdistrict$keepResting(Player player, boolean wakeImmediately, boolean updateSleepingPlayers) {
        if (!MedicalResting.isResting(player)
                || !MedicalResting.valid(player.level(), player.getSleepingPos().orElseThrow())) {
            player.stopSleepInBed(wakeImmediately, updateSleepingPlayers);
        }
    }

    @Inject(method = "isSleepingLongEnough()Z", at = @At("HEAD"), cancellable = true)
    private void craftdistrict$restIsNotNightSleep(CallbackInfoReturnable<Boolean> cir) {
        if (MedicalResting.isResting((Player)(Object)this)) cir.setReturnValue(false);
    }
}
