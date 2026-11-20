package com.craftdistrict.client;

import com.craftdistrict.mixin.client.LevelRendererFrustumAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;

import java.util.List;

final class DoorRenderSupport {
    //? if <1.21 {
    /*private static final boolean NATIVE_BLOCK_ENTITY_CULLING = dev.architectury.platform.Platform.isForge();
    *///?}

    private DoorRenderSupport() { }

    static boolean inFrustum(AABB bounds) {
        //? if <1.21 {
        /*// Forge already tests the block entity's bounding box before dispatching the renderer.
        if (NATIVE_BLOCK_ENTITY_CULLING) return true;
        *///?}
        var levelRenderer = (LevelRendererFrustumAccessor) Minecraft.getInstance().levelRenderer;
        Frustum frustum = levelRenderer.craftdistrict$getCapturedFrustum();
        if (frustum == null) frustum = levelRenderer.craftdistrict$getCullingFrustum();
        return frustum == null || frustum.isVisible(bounds);
    }

    static void emit(List<BakedQuad> quads, PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay) {
        //? if >=1.21 {
        if (quads.isEmpty()) return;
        // Same full-strength color and packed light as the short overload, supplied once per mesh.
        float[] brightness = {1, 1, 1, 1};
        int[] lightmap = {light, light, light, light};
        for (int i = 0; i < quads.size(); i++)
            consumer.putBulkData(pose, quads.get(i), brightness, 1, 1, 1, 1, lightmap, overlay, false);
        //?} else {
        /*for (int i = 0; i < quads.size(); i++)
            consumer.putBulkData(pose, quads.get(i), 1, 1, 1, light, overlay);
        *///?}
    }
}
