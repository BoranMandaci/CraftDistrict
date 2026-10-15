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
                    output.accept(CraftDistrictBlocks.Bakery.TRAY_RACK_EMPTY.get());
                    output.accept(CraftDistrictBlocks.Bakery.TRAY_RACK_BAKED.get());
                    output.accept(CraftDistrictBlocks.Bakery.REFRIGERATED_CAKE_DISPLAY.get());
                    output.accept(CraftDistrictBlocks.Bakery.REFRIGERATED_PASTRY_DISPLAY.get());
                    output.accept(CraftDistrictBlocks.Bakery.BREAD_PEEL_RACK.get());
                    output.accept(CraftDistrictBlocks.Bakery.UTENSIL_WALL_RACK.get());
                    output.accept(CraftDistrictBlocks.Bakery.ROLLING_PIN_RACK.get());
                    output.accept(CraftDistrictBlocks.Bakery.BREAD_SLICER.get());
                    output.accept(CraftDistrictBlocks.Police.RECEPTION_DESK.get());
                    output.accept(CraftDistrictBlocks.Police.DEPARTMENT_SIGN.get());
                    output.accept(CraftDistrictBlocks.Police.WAITING_BENCH.get());
                    output.accept(CraftDistrictBlocks.Police.DISPATCH_CONSOLE.get());
                    output.accept(CraftDistrictBlocks.Police.EVIDENCE_STORAGE_SHELF.get());
                    output.accept(CraftDistrictBlocks.Police.OFFICE_CHAIR.get());
                    output.accept(CraftDistrictBlocks.Police.INVESTIGATION_BOARD.get());
                    output.accept(CraftDistrictBlocks.Police.INTERROGATION_TABLE.get());
                    output.accept(CraftDistrictBlocks.Police.FINGERPRINT_STATION.get());
                    output.accept(CraftDistrictBlocks.Police.INTERROGATION_CHAIR.get());
                    output.accept(CraftDistrictBlocks.Police.HOLDING_CELL_BARS.get());
                    output.accept(CraftDistrictBlocks.Police.HOLDING_CELL_DOOR.get());
                    output.accept(CraftDistrictBlocks.Police.SECURITY_CAMERA.get());
                    output.accept(CraftDistrictBlocks.Police.LOCKERS.get());
                    output.accept(CraftDistrictBlocks.Police.HOLDING_CELL_BED.get());
                    output.accept(CraftDistrictBlocks.Police.HOLDING_CELL_TOILET_SINK.get());
                    output.accept(CraftDistrictBlocks.Police.ONE_WAY_GLASS.get());
                    output.accept(CraftDistrictBlocks.Police.MUGSHOT_HEIGHT_CHART.get());
                    output.accept(CraftDistrictBlocks.Police.BOOKING_CAMERA.get());
                    output.accept(CraftDistrictBlocks.Police.RADIO_CHARGING_STATION.get());
                    output.accept(CraftDistrictBlocks.Police.EQUIPMENT_RACK.get());
                    output.accept(CraftDistrictBlocks.Police.PISTOL_RACK.get());
                    output.accept(CraftDistrictBlocks.Police.RIFLE_RACK.get());
                    output.accept(CraftDistrictBlocks.FireStation.GEAR_LOCKER.get());
                    output.accept(CraftDistrictBlocks.FireStation.BREATHING_APPARATUS_RACK.get());
                    output.accept(CraftDistrictBlocks.FireStation.EXTINGUISHER.get());
                    output.accept(CraftDistrictBlocks.FireStation.HOSE_REEL.get());
                    output.accept(CraftDistrictBlocks.FireStation.HOSE_STORAGE_RACK.get());
                    output.accept(CraftDistrictBlocks.FireStation.RESCUE_TOOLS_RACK.get());
                    output.accept(CraftDistrictBlocks.FireStation.HYDRAULIC_RESCUE_TOOL.get());
                    output.accept(CraftDistrictBlocks.FireStation.RESCUE_STRETCHER.get());
                    output.accept(CraftDistrictBlocks.FireStation.DEPARTMENT_SIGN.get());
                    output.accept(CraftDistrictBlocks.FireStation.ALARM_CONTROL_PANEL.get());
                    output.accept(CraftDistrictBlocks.FireStation.STATION_POLE.get());
                    output.accept(CraftDistrictBlocks.FireStation.DISPATCH_DESK.get());
                    output.accept(CraftDistrictBlocks.FireStation.WALL_CLOCK.get());
                    output.accept(CraftDistrictBlocks.FireStation.INCIDENT_BOARD.get());
                    output.accept(CraftDistrictBlocks.FireStation.LOCKER_BENCH.get());
                    output.accept(CraftDistrictBlocks.FireStation.FIRST_AID_CABINET.get());
                    output.accept(CraftDistrictBlocks.Supermarket.GROCERY_SHELF.get());
                    output.accept(CraftDistrictBlocks.Supermarket.CANNED_SHELF.get());
                    output.accept(CraftDistrictBlocks.Supermarket.SNACK_SHELF.get());
                    output.accept(CraftDistrictBlocks.Supermarket.CANDY_SHELF.get());
                    output.accept(CraftDistrictBlocks.Supermarket.FRUIT_STAND.get());
                    output.accept(CraftDistrictBlocks.Supermarket.VEGETABLE_STAND.get());
                    output.accept(CraftDistrictBlocks.Supermarket.PROMOTIONAL_DISPLAY.get());
                    output.accept(CraftDistrictBlocks.Supermarket.DRINKS_FRIDGE_A.get());
                    output.accept(CraftDistrictBlocks.Supermarket.DRINKS_FRIDGE_B.get());
                    output.accept(CraftDistrictBlocks.Supermarket.DAIRY_REFRIGERATOR.get());
                    output.accept(CraftDistrictBlocks.Supermarket.CHEST_FREEZER_FROZEN_FOOD.get());
                    output.accept(CraftDistrictBlocks.Supermarket.CHEST_FREEZER_ICE_CREAM.get());
                    output.accept(CraftDistrictBlocks.Supermarket.DELI_DISPLAY_COUNTER.get());
                    output.accept(CraftDistrictBlocks.Supermarket.CHECKOUT.get());
                    output.accept(CraftDistrictBlocks.Supermarket.SHOPPING_CART.get());
                    output.accept(CraftDistrictBlocks.Supermarket.FILLED_SHOPPING_CART.get());
                    output.accept(CraftDistrictBlocks.Supermarket.BASKET_STACK.get());
                    output.accept(CraftDistrictBlocks.Supermarket.SELF_CHECKOUT.get());
                    output.accept(CraftDistrictBlocks.Supermarket.STOCK_PALLET.get());
                    output.accept(CraftDistrictBlocks.Supermarket.SECURITY_GATE.get());
                    output.accept(CraftDistrictBlocks.Supermarket.CHECKOUT_CANDY_RACK.get());
                })));
        TABS.register();
    }
}
