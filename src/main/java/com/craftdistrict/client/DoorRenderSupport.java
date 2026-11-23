package com.craftdistrict.client;

import com.craftdistrict.mixin.client.LevelRendererFrustumAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.List;

final class DoorRenderSupport {
    //? if <1.21 {
    /*private static final boolean NATIVE_BLOCK_ENTITY_CULLING = dev.architectury.platform.Platform.isForge();
    *///?}

    // Each door renderer owns one set of inputs for the vanilla bulk-quad path.
    private final float[] brightness = {1, 1, 1, 1};
    private final int[] lightmap = new int[4];
    private BakedModel missingModel;

    boolean modelsChanged() {
        // The model manager replaces its fallback together with the baked model cache on reload.
        BakedModel current = Minecraft.getInstance().getModelManager().getMissingModel();
        if (missingModel == current) return false;
        missingModel = current;
        return true;
    }

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

    void setLight(int light) {
        if (lightmap[0] != light) Arrays.fill(lightmap, light);
    }

    void emit(List<BakedQuad> quads, PoseStack.Pose pose, VertexConsumer consumer, int overlay) {
        for (int i = 0; i < quads.size(); i++)
            consumer.putBulkData(pose, quads.get(i), brightness, 1, 1, 1,
                    //? if >=1.21 {
                    1,
                    //?}
                    lightmap, overlay, false);
    }
}
