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
                    output.accept(CraftDistrictBlocks.Bakery.INDUSTRIAL_OVEN.get());
                    output.accept(CraftDistrictBlocks.Bakery.CASH_REGISTER.get());
                    output.accept(CraftDistrictBlocks.Bakery.DOUGH_PREPARATION_TABLE.get());
                    output.accept(CraftDistrictBlocks.Bakery.FLOUR_SACKS_SMALL.get());
                    output.accept(CraftDistrictBlocks.Bakery.FLOUR_SACKS_STACKED.get());
                    output.accept(CraftDistrictBlocks.Bakery.FLOUR_SACKS_PILE.get());
                    output.accept(CraftDistrictBlocks.Bakery.CROISSANT_TRAY.get());
                    output.accept(CraftDistrictBlocks.Bakery.COOKIE_TRAY.get());
                    output.accept(CraftDistrictBlocks.Bakery.BREAD_ROLLS_TRAY.get());
                    output.accept(CraftDistrictBlocks.Bakery.EMPTY_BAKING_TRAY.get());
                    output.accept(CraftDistrictBlocks.Bakery.INDUSTRIAL_DOUGH_MIXER.get());
                    output.accept(CraftDistrictBlocks.Bakery.HANGING_SHOP_SIGN.get());
                    output.accept(CraftDistrictBlocks.Bakery.WALL_SHOP_SIGN.get());
                    output.accept(CraftDistrictBlocks.Bakery.WALL_MENU_BOARD.get());
                    output.accept(CraftDistrictBlocks.Bakery.DAILY_SPECIAL_BOARD.get());
                    output.accept(CraftDistrictBlocks.Bakery.SIDEWALK_MENU_BOARD.get());
                })));
        TABS.register();
    }
}
