package com.craftdistrict.client;

import com.craftdistrict.registry.CraftDistrictBlocks;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;

public final class CraftDistrictClient {
    private CraftDistrictClient() {
    }

    public static void init() {
        ClientLifecycleEvent.CLIENT_SETUP.register(client -> RenderTypeRegistry.register(
                RenderType.cutout(), CraftDistrictBlocks.Bakery.DISPLAY_COUNTER.get(),
                CraftDistrictBlocks.Bakery.INDUSTRIAL_OVEN.get()));
    }
}
