package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Shared creative tab for CraftDistrict's decoration collections. */
public final class CraftDistrictCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(CraftDistrict.MOD_ID, Registries.CREATIVE_MODE_TAB);

    private CraftDistrictCreativeTabs() {
    }

    public static void register() {
        TABS.register("decorations", () -> CreativeTabRegistry.create(builder -> builder
                .title(Component.translatable("itemGroup.craftdistrict"))
                .icon(() -> new ItemStack(CraftDistrictBlocks.Bakery.DISPLAY_COUNTER.get()))
                .displayItems((parameters, output) -> {
                    output.accept(CraftDistrictBlocks.Bakery.DISPLAY_COUNTER.get());
                    output.accept(CraftDistrictBlocks.Bakery.BREAD_SHELF.get());
                })));
        TABS.register();
    }
}
