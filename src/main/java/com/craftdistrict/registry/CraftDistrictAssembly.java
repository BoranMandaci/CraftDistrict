package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.assembly.AssemblyRecipe;
import com.craftdistrict.assembly.AssemblyRecipeSerializer;
import com.craftdistrict.menu.AssemblyStationMenu;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class CraftDistrictAssembly {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.RECIPE_SERIALIZER);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.MENU);
    public static final RegistrySupplier<RecipeType<AssemblyRecipe>> RECIPE_TYPE = TYPES.register("assembly", () -> new RecipeType<>() {
        @Override public String toString() { return "craftdistrict:assembly"; }
    });
    public static final RegistrySupplier<RecipeSerializer<AssemblyRecipe>> SERIALIZER = SERIALIZERS.register("assembly", AssemblyRecipeSerializer::new);
    public static final RegistrySupplier<MenuType<AssemblyStationMenu>> MENU = MENUS.register("assembly_station", () -> MenuRegistry.ofExtended(AssemblyStationMenu::new));

    private CraftDistrictAssembly() { }
    public static void register() { TYPES.register(); SERIALIZERS.register(); MENUS.register(); }
}
