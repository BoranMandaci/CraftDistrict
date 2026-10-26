package com.craftdistrict.mixin.client;

import com.craftdistrict.client.GuiItemGeometry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Keep the vanilla/loader renderer and its materials; trim only GUI quad submission. */
@Mixin(ItemRenderer.class)
public abstract class GuiItemRendererMixin {
    @Unique private final GuiItemGeometry craftdistrict$guiGeometry = new GuiItemGeometry();
    @Unique private boolean craftdistrict$gui;
    @Unique private PoseStack craftdistrict$guiPose;

    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V", at = @At("HEAD"))
    private void craftdistrict$beginGui(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo ci) {
        craftdistrict$gui = GuiItemGeometry.eligible(stack, context, model);
        craftdistrict$guiPose = craftdistrict$gui ? pose : null;
    }

    @ModifyVariable(method = "renderQuadList", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<BakedQuad> craftdistrict$visibleGuiQuads(List<BakedQuad> quads) {
        return craftdistrict$gui ? craftdistrict$guiGeometry.visible(quads, craftdistrict$guiPose.last().pose(),
                RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix()) : quads;
    }

    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V", at = @At("RETURN"))
    private void craftdistrict$endGui(CallbackInfo ci) {
        craftdistrict$gui = false;
        craftdistrict$guiPose = null;
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void craftdistrict$reloadGui(CallbackInfo ci) {
        craftdistrict$gui = false;
        craftdistrict$guiPose = null;
        craftdistrict$guiGeometry.clear();
    }
}
