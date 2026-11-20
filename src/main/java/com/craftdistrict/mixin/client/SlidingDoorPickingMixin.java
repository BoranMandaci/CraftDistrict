package com.craftdistrict.mixin.client;

import com.craftdistrict.block.SlidingEntranceBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pick the real leaf overhang without inflating the outline or replacing vanilla reach/occlusion. */
@Mixin(GameRenderer.class)
public abstract class SlidingDoorPickingMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method="pick(F)V",at=@At("RETURN"))
    private void craftdistrict$slidingLeaf(float partialTick,CallbackInfo ci) {
        Entity camera=minecraft.getCameraEntity();
        if(camera==null||minecraft.level==null||minecraft.player==null||minecraft.gameMode==null)return;
        //? if >=1.21 {
        double reach=minecraft.player.blockInteractionRange();
        //?} else {
        /*double reach=minecraft.gameMode.getPickRange();
        *///?}
        Vec3 start = camera.getEyePosition(partialTick);
        Vec3 view = camera.getViewVector(partialTick);
        Vec3 end = start.add(view.x * reach, view.y * reach, view.z * reach);
        HitResult original=minecraft.hitResult;
        double nearest=reach*reach+1e-8;
        if(original!=null)
            nearest=Math.min(nearest,start.distanceToSqr(original.getLocation())
                    +(original.getType()==HitResult.Type.MISS?1e-8:0));
        var hit=SlidingEntranceBlock.pickMovedPanel(minecraft.level,start,end,nearest);
        if(hit!=null){minecraft.hitResult=hit;minecraft.crosshairPickEntity=null;}
    }
}
