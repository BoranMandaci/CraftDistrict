package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.block.OfficeBadgeReaderBlockEntity;
import com.craftdistrict.menu.AccessCardMenu;
import com.craftdistrict.menu.CardReaderMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class CraftDistrictAccessControl {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.MENU);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(CraftDistrict.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<MenuType<AccessCardMenu>> ACCESS_CARD = MENUS.register("access_card", () -> MenuRegistry.ofExtended(AccessCardMenu::new));
    public static final RegistrySupplier<MenuType<CardReaderMenu>> CARD_READER = MENUS.register("card_reader", () -> MenuRegistry.ofExtended(CardReaderMenu::new));
    public static final RegistrySupplier<BlockEntityType<OfficeBadgeReaderBlockEntity>> BADGE_INDICATOR =
            ENTITIES.register("employee_badge_reader", () -> BlockEntityType.Builder
                    .of(OfficeBadgeReaderBlockEntity::new, CraftDistrictBlocks.Office.EMPLOYEE_BADGE_READER.get()).build(null));

    private CraftDistrictAccessControl() { }
    public static void register() { MENUS.register(); ENTITIES.register(); }
}
