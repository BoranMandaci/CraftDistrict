package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.block.RoomNumberPlaqueBlockEntity;
import com.craftdistrict.menu.RoomNumberMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Room plaques and configurable corridor signs share one small persistent record and editor. */
public final class CraftDistrictRoomNumbers {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.MENU);
    public static final RegistrySupplier<BlockEntityType<RoomNumberPlaqueBlockEntity>> PLAQUE = BLOCK_ENTITIES.register("room_number_plaque",
            () -> BlockEntityType.Builder.of(RoomNumberPlaqueBlockEntity::new, CraftDistrictBlocks.Hotel.ROOM_NUMBER_PLAQUE.get(),
                    CraftDistrictBlocks.Hotel.ELEVATOR_INDICATOR.get(), CraftDistrictBlocks.Hotel.DIRECTIONAL_SIGN.get()).build(null));
    public static final RegistrySupplier<MenuType<RoomNumberMenu>> MENU = MENUS.register("room_number", () -> MenuRegistry.ofExtended(RoomNumberMenu::new));
    private CraftDistrictRoomNumbers() { }
    public static void register() { BLOCK_ENTITIES.register(); MENUS.register(); }
}
