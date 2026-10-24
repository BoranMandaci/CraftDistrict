package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/** Ordinary resources and the existing block items share one item registry. */
public final class CraftDistrictItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> FURNITURE_PARTS = ITEMS.register("furniture_parts", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> METAL_PARTS = ITEMS.register("metal_parts", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ELECTRONIC_PARTS = ITEMS.register("electronic_parts", () -> new Item(new Item.Properties()));

    private CraftDistrictItems() { }

    static void registerBlockItem(String id, Supplier<Item> factory) {
        ITEMS.register(id, factory);
    }

    public static void register() {
        ITEMS.register();
    }
}
