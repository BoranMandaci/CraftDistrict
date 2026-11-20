package com.craftdistrict.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererFrustumAccessor {
    @Accessor("cullingFrustum")
    Frustum craftdistrict$getCullingFrustum();

    @Accessor("capturedFrustum")
    Frustum craftdistrict$getCapturedFrustum();
}
