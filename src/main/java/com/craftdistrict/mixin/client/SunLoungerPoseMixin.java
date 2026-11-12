package com.craftdistrict.mixin.client;

import com.craftdistrict.block.HotelSunLoungerBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the lounger tilts the existing synchronized decorative resting pose. */
@Mixin(LivingEntityRenderer.class)
public abstract class SunLoungerPoseMixin {
    @Unique private static final Quaternionf craftdistrict$recline = Axis.XP.rotationDegrees(-8);
    //? if >=1.21 {
    @Inject(method = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("TAIL"))
    private void craftdistrict$reclineLounger(LivingEntity entity, PoseStack pose, float age, float yaw,
                                             float partialTick, float scale, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V", at = @At("TAIL"))
    private void craftdistrict$reclineLounger(LivingEntity entity, PoseStack pose, float age, float yaw,
                                             float partialTick, CallbackInfo ci) {
    *///?}
        if (!entity.isSleeping()) return;
        BlockPos head = entity.getSleepingPos().orElse(null);
        if (head != null && entity.level().getBlockState(head).getBlock() instanceof HotelSunLoungerBlock)
            pose.mulPose(craftdistrict$recline);
    }
}
