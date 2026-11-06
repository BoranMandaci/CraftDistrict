package com.craftdistrict.mixin;

import com.craftdistrict.entity.MedicalResting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the three opted-in medical tables extend vanilla sleep-pose handling. */
@Mixin(LivingEntity.class)
public abstract class MedicalRestingEntityMixin {
    @Inject(method = "checkBedExists()Z", at = @At("HEAD"), cancellable = true)
    private void craftdistrict$medicalSupport(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (MedicalResting.isResting(entity)) cir.setReturnValue(MedicalResting.valid(entity.level(), entity.getSleepingPos().orElseThrow()));
    }

    @Inject(method = "getBedOrientation()Lnet/minecraft/core/Direction;", at = @At("HEAD"), cancellable = true)
    private void craftdistrict$medicalFacing(CallbackInfoReturnable<Direction> cir) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (MedicalResting.isResting(entity)) cir.setReturnValue(entity.level().getBlockState(entity.getSleepingPos().orElseThrow())
                .getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    @Inject(method = "setPosToBed(Lnet/minecraft/core/BlockPos;)V", at = @At("TAIL"))
    private void craftdistrict$medicalHeight(BlockPos pos, CallbackInfo ci) {
        MedicalResting.position((LivingEntity)(Object)this, pos);
    }

    @Inject(method = "stopSleeping()V", at = @At("HEAD"), cancellable = true)
    private void craftdistrict$medicalExit(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (MedicalResting.isResting(entity)) { MedicalResting.wake(entity); ci.cancel(); }
    }
}
