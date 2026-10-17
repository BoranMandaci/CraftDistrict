package com.craftdistrict.client;

import com.craftdistrict.registry.CraftDistrictBlocks;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;

public final class CraftDistrictClient {
    private CraftDistrictClient() {
    }

    public static void init() {
        ClientLifecycleEvent.CLIENT_SETUP.register(client -> {
            RenderTypeRegistry.register(
                RenderType.cutout(), CraftDistrictBlocks.Bakery.DISPLAY_COUNTER.get(),
                CraftDistrictBlocks.Bakery.INDUSTRIAL_OVEN.get(),
                CraftDistrictBlocks.Bakery.REFRIGERATED_CAKE_DISPLAY.get(),
                CraftDistrictBlocks.Bakery.REFRIGERATED_PASTRY_DISPLAY.get(),
                CraftDistrictBlocks.Supermarket.DRINKS_FRIDGE_A.get(),
                CraftDistrictBlocks.Supermarket.DRINKS_FRIDGE_B.get(),
                CraftDistrictBlocks.Supermarket.COLA_FRIDGE.get(),
                CraftDistrictBlocks.Supermarket.BEER_FRIDGE.get(),
                CraftDistrictBlocks.Supermarket.WINE_FRIDGE.get(),
                CraftDistrictBlocks.Supermarket.WHISKEY_DISPLAY_CABINET.get(),
                CraftDistrictBlocks.Supermarket.DAIRY_REFRIGERATOR.get(),
                CraftDistrictBlocks.Supermarket.CHEST_FREEZER_FROZEN_FOOD.get(),
                CraftDistrictBlocks.Supermarket.CHEST_FREEZER_ICE_CREAM.get(),
                CraftDistrictBlocks.Supermarket.DELI_DISPLAY_COUNTER.get(),
                CraftDistrictBlocks.Supermarket.CHECKOUT.get(),
                CraftDistrictBlocks.Supermarket.SHOPPING_CART.get(),
                CraftDistrictBlocks.Supermarket.FILLED_SHOPPING_CART.get(),
                CraftDistrictBlocks.Supermarket.BASKET_STACK.get(),
                CraftDistrictBlocks.Supermarket.SELF_CHECKOUT.get());
            RenderTypeRegistry.register(RenderType.translucent(), CraftDistrictBlocks.Police.ONE_WAY_OBSERVATION_GLASS.get());
        });
    }
}
