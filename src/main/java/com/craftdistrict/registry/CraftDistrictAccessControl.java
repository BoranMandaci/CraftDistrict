package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.menu.AccessCardMenu;
import com.craftdistrict.menu.CardReaderMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class CraftDistrictAccessControl {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.MENU);
    public static final RegistrySupplier<MenuType<AccessCardMenu>> ACCESS_CARD = MENUS.register("access_card", () -> MenuRegistry.ofExtended(AccessCardMenu::new));
    public static final RegistrySupplier<MenuType<CardReaderMenu>> CARD_READER = MENUS.register("card_reader", () -> MenuRegistry.ofExtended(CardReaderMenu::new));

    private CraftDistrictAccessControl() { }
    public static void register() { MENUS.register(); }
}
