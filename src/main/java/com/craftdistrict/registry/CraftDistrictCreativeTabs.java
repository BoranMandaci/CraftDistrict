package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public final class CraftDistrictCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(CraftDistrict.MOD_ID, Registries.CREATIVE_MODE_TAB);

    private CraftDistrictCreativeTabs() {
    }

    public static final RegistrySupplier<CreativeModeTab> DECORATIONS =
            TABS.register("decorations", () -> CreativeTabRegistry.create(builder -> builder
                    .title(Component.translatable("itemGroup.craftdistrict"))
                    .icon(() -> new ItemStack(CraftDistrictBlocks.Bakery.DISPLAY_COUNTER.get()))
                    .displayItems((parameters, output) -> {
                        Set<Item> added = new HashSet<>();
                        for (DecorationCollection collection : DecorationCollection.values()) {
                            for (Item item : collection.items()) {
                                if (added.add(item)) {
                                    output.accept(item);
                                }
                            }
                        }
                    })));

    public static void register() {
        TABS.register();
    }
}
