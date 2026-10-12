package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Temporary access to the validation blocks until real collections are added. */
public final class CraftDistrictCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(CraftDistrict.MOD_ID, Registries.CREATIVE_MODE_TAB);

    private CraftDistrictCreativeTabs() {
    }

    public static void register() {
        TABS.register("test_decorations", () -> CreativeTabRegistry.create(builder -> builder
                .title(Component.translatable("itemGroup.craftdistrict"))
                .icon(() -> new ItemStack(CraftDistrictBlocks.Workshop.TEST_COUNTER.get()))
                .displayItems((parameters, output) -> {
                    output.accept(CraftDistrictBlocks.Workshop.TEST_STATIC.get());
                    output.accept(CraftDistrictBlocks.Workshop.TEST_ROTATABLE.get());
                    output.accept(CraftDistrictBlocks.Workshop.TEST_COUNTER.get());
                })));
        TABS.register();
    }
}
