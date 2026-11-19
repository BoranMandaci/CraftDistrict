package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.block.BarDoorBlockEntity;
import com.craftdistrict.block.SlidingDoorBlockEntity;
import com.craftdistrict.menu.SlidingDoorMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class CraftDistrictDoors {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(CraftDistrict.MOD_ID,Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(CraftDistrict.MOD_ID,Registries.MENU);
    public static final RegistrySupplier<BlockEntityType<BarDoorBlockEntity>> BAR=ENTITIES.register("bar_door",
            ()->BlockEntityType.Builder.of(BarDoorBlockEntity::new,CraftDistrictBlocks.Doors.BAR.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<SlidingDoorBlockEntity>> SLIDING=ENTITIES.register("sliding_door",
            ()->BlockEntityType.Builder.of(SlidingDoorBlockEntity::new,CraftDistrictBlocks.Doors.AUTOMATIC_GLASS_SLIDING.get(),
                    CraftDistrictBlocks.Doors.HOSPITAL_SLIDING.get()).build(null));
    public static final RegistrySupplier<MenuType<SlidingDoorMenu>> SETTINGS=MENUS.register("sliding_door_settings",
            ()->MenuRegistry.ofExtended(SlidingDoorMenu::new));
    private CraftDistrictDoors(){ }
    public static void register(){ENTITIES.register();MENUS.register();}
}
