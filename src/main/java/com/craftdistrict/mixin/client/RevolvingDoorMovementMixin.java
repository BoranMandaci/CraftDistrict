package com.craftdistrict.mixin.client;

import com.craftdistrict.client.RevolvingWingContact;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LocalPlayer.class)
public abstract class RevolvingDoorMovementMixin {
    @Unique private long craftdistrict$lastWingContactTick = Long.MIN_VALUE;

    @ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 craftdistrict$revolvingWingContact(Vec3 movement, MoverType type) {
        if (type != MoverType.SELF) return movement;
        LocalPlayer player = (LocalPlayer) (Object) this;
        long gameTime = player.level().getGameTime();
        boolean advance = gameTime != craftdistrict$lastWingContactTick;
        craftdistrict$lastWingContactTick = gameTime;
        return RevolvingWingContact.adjust(player, movement, gameTime, advance);
    }
}
