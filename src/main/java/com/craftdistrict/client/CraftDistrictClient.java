package com.craftdistrict.client;

import com.craftdistrict.registry.CraftDistrictBlocks;
import com.craftdistrict.registry.CraftDistrictEntities;
import com.craftdistrict.registry.CraftDistrictAssembly;
import com.craftdistrict.registry.CraftDistrictRoomNumbers;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;
//? if >=1.21 {
import dev.architectury.platform.Platform;
//?}

public final class CraftDistrictClient {
    private CraftDistrictClient() {
    }

    public static void init() {
        EntityRendererRegistry.register(CraftDistrictEntities.SEAT, NoopRenderer::new);
        // NeoForge's RegisterMenuScreensEvent runs before CLIENT_SETUP. Listen as soon as the menu types exist.
        // Fabric/Forge register directly, retaining their existing client-setup timing.
        //? if >=1.21 {
        final boolean earlyScreens = Platform.isNeoForge();
        //?} else {
        /*final boolean earlyScreens = false;
        *///?}
        if (earlyScreens) {
            CraftDistrictAssembly.MENU.listen(type -> MenuRegistry.registerScreenFactory(type, AssemblyStationScreen::new));
            CraftDistrictAssembly.LIGHT_COLOR_MENU.listen(type -> MenuRegistry.registerScreenFactory(type, LightColorScreen::new));
            CraftDistrictRoomNumbers.MENU.listen(type -> MenuRegistry.registerScreenFactory(type, RoomNumberScreen::new));
            com.craftdistrict.registry.CraftDistrictDoors.SETTINGS.listen(type -> MenuRegistry.registerScreenFactory(type,SlidingDoorScreen::new));
        }
        ClientLifecycleEvent.CLIENT_SETUP.register(client -> {
            LightingColors.init();
            RenderTypeRegistry.register(RenderType.cutout(), CraftDistrictBlocks.Doors.FIRE_EXIT.get(),
                    CraftDistrictBlocks.Doors.MESH_SECURITY.get(), CraftDistrictBlocks.Doors.KITCHEN_SERVICE.get(),
                    CraftDistrictBlocks.Doors.AUTOMATIC_GLASS_SLIDING.get(), CraftDistrictBlocks.Doors.HOSPITAL_SLIDING.get(),
                    CraftDistrictBlocks.Doors.WINDOWED_GARAGE.get(), CraftDistrictBlocks.Doors.CRAFTSMAN_FRONT.get(),
                    CraftDistrictBlocks.Doors.COTTAGE.get(), CraftDistrictBlocks.Doors.FRENCH_GLASS.get(),
                    CraftDistrictBlocks.Doors.GLASS_REVOLVING.get());
            RenderTypeRegistry.register(RenderType.translucent(), CraftDistrictBlocks.Casino.CASHIER_COUNTER.get(),
                    CraftDistrictBlocks.Casino.VIP_DIVIDER.get());
            BlockEntityRendererRegistry.register(CraftDistrictRoomNumbers.PLAQUE.get(), RoomNumberRenderer::new);
            BlockEntityRendererRegistry.register(com.craftdistrict.registry.CraftDistrictDoors.BAR.get(),BarDoorRenderer::new);
            BlockEntityRendererRegistry.register(com.craftdistrict.registry.CraftDistrictDoors.SLIDING.get(),SlidingDoorRenderer::new);
            BlockEntityRendererRegistry.register(com.craftdistrict.registry.CraftDistrictDoors.ROTARY.get(), RotaryEntranceRenderer::new);
            if (!earlyScreens) {
                MenuRegistry.registerScreenFactory(CraftDistrictAssembly.MENU.get(), AssemblyStationScreen::new);
                MenuRegistry.registerScreenFactory(CraftDistrictAssembly.LIGHT_COLOR_MENU.get(), LightColorScreen::new);
                MenuRegistry.registerScreenFactory(CraftDistrictRoomNumbers.MENU.get(), RoomNumberScreen::new);
                MenuRegistry.registerScreenFactory(com.craftdistrict.registry.CraftDistrictDoors.SETTINGS.get(),SlidingDoorScreen::new);
            }
            RenderTypeRegistry.register(
                RenderType.cutout(), CraftDistrictBlocks.Bakery.DISPLAY_COUNTER.get(),
                CraftDistrictBlocks.Bakery.INDUSTRIAL_OVEN.get(),
                CraftDistrictBlocks.Bakery.REFRIGERATED_CAKE_DISPLAY.get(),
                CraftDistrictBlocks.Bakery.REFRIGERATED_PASTRY_DISPLAY.get(),
                CraftDistrictBlocks.Restaurant.SALAD_BAR_COUNTER.get(),
                CraftDistrictBlocks.Restaurant.DISPOSABLE_CUPS.get(),
                CraftDistrictBlocks.Restaurant.UTENSIL_CONDIMENT_STATION.get(),
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
                CraftDistrictBlocks.Supermarket.SELF_CHECKOUT.get(),
                CraftDistrictBlocks.Office.WATER_COOLER.get(),
                CraftDistrictBlocks.Office.PLANTER.get(),
                CraftDistrictBlocks.Office.SNACK_VENDING_MACHINE.get(),
                CraftDistrictBlocks.Office.DRINK_VENDING_MACHINE.get(),
                CraftDistrictBlocks.Office.MIXED_VENDING_MACHINE.get(),
                CraftDistrictBlocks.Pharmacy.GLASS_DISPLAY.get(),
                CraftDistrictBlocks.Pharmacy.REFRIGERATOR.get());
            RenderTypeRegistry.register(RenderType.cutout(), CraftDistrictBlocks.Office.GLASS_PARTITION.get(),
                    CraftDistrictBlocks.Office.GLASS_PARTITION_DOOR.get(), CraftDistrictBlocks.Hotel.POOL_UMBRELLA.get(),
                    CraftDistrictBlocks.Casino.BIG_SIX_WHEEL.get());
            RenderTypeRegistry.register(RenderType.translucent(), CraftDistrictBlocks.Police.ONE_WAY_OBSERVATION_GLASS.get(),
                    CraftDistrictBlocks.Police.ONE_WAY_GLASS_BLOCK.get(), CraftDistrictBlocks.Hospital.IV_STAND.get(),
                    CraftDistrictBlocks.Hospital.MAMMOGRAPHY_MACHINE.get(), CraftDistrictBlocks.Hospital.INCUBATOR.get(),
                    CraftDistrictBlocks.Hotel.SHOWER_ENCLOSURE.get(), CraftDistrictBlocks.Hotel.FIXED_SHOWER_GLASS.get(),
                    CraftDistrictBlocks.Hotel.WASHING_MACHINE.get(), CraftDistrictBlocks.Hotel.DRYER.get(),
                    CraftDistrictBlocks.Hotel.JACUZZI.get(), CraftDistrictBlocks.Hotel.POOL_WATERFALL.get());
        });
    }
}
