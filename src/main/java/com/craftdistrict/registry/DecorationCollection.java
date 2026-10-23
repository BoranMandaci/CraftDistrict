package com.craftdistrict.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public enum DecorationCollection {
    BAKERY("bakery", CraftDistrictBlocks.Bakery.BREAD_SHELF,
            CraftDistrictBlocks.Bakery.DISPLAY_COUNTER,
            CraftDistrictBlocks.Bakery.BREAD_SHELF,
            CraftDistrictBlocks.Bakery.REFRIGERATED_CAKE_DISPLAY,
            CraftDistrictBlocks.Bakery.REFRIGERATED_PASTRY_DISPLAY,
            CraftDistrictBlocks.Bakery.CASH_REGISTER,
            CraftDistrictBlocks.Bakery.INDUSTRIAL_OVEN,
            CraftDistrictBlocks.Bakery.DOUGH_PREPARATION_TABLE,
            CraftDistrictBlocks.Bakery.INDUSTRIAL_DOUGH_MIXER,
            CraftDistrictBlocks.Bakery.BREAD_SLICER,
            CraftDistrictBlocks.Bakery.FLOUR_SACKS_SMALL,
            CraftDistrictBlocks.Bakery.FLOUR_SACKS_STACKED,
            CraftDistrictBlocks.Bakery.FLOUR_SACKS_PILE,
            CraftDistrictBlocks.Bakery.TRAY_RACK_EMPTY,
            CraftDistrictBlocks.Bakery.TRAY_RACK_BAKED,
            CraftDistrictBlocks.Bakery.CROISSANT_TRAY,
            CraftDistrictBlocks.Bakery.COOKIE_TRAY,
            CraftDistrictBlocks.Bakery.BREAD_ROLLS_TRAY,
            CraftDistrictBlocks.Bakery.EMPTY_BAKING_TRAY,
            CraftDistrictBlocks.Bakery.BREAD_PEEL_RACK,
            CraftDistrictBlocks.Bakery.UTENSIL_WALL_RACK,
            CraftDistrictBlocks.Bakery.ROLLING_PIN_RACK,
            CraftDistrictBlocks.Bakery.HANGING_SHOP_SIGN,
            CraftDistrictBlocks.Bakery.WALL_SHOP_SIGN,
            CraftDistrictBlocks.Bakery.WALL_MENU_BOARD,
            CraftDistrictBlocks.Bakery.DAILY_SPECIAL_BOARD,
            CraftDistrictBlocks.Bakery.SIDEWALK_MENU_BOARD,
            CraftDistrictBlocks.FireStation.WALL_CLOCK),
    POLICE_STATION("police_station", CraftDistrictBlocks.Police.RECEPTION_DESK,
            CraftDistrictBlocks.Police.RECEPTION_DESK,
            CraftDistrictBlocks.Police.DISPATCH_CONSOLE,
            CraftDistrictBlocks.Police.WAITING_BENCH,
            CraftDistrictBlocks.Police.OFFICE_CHAIR,
            CraftDistrictBlocks.Police.BLUE_CHAIR,
            CraftDistrictBlocks.Police.FINGERPRINT_STATION,
            CraftDistrictBlocks.Police.RADIO_CHARGING_STATION,
            CraftDistrictBlocks.Police.HOLDING_CELL_BARS,
            CraftDistrictBlocks.Police.HOLDING_CELL_DOOR,
            CraftDistrictBlocks.Police.HOLDING_CELL_BED,
            CraftDistrictBlocks.Police.HOLDING_CELL_TOILET,
            CraftDistrictBlocks.Police.EVIDENCE_STORAGE_SHELF,
            CraftDistrictBlocks.Police.LOCKERS,
            CraftDistrictBlocks.FireStation.LOCKER_BENCH,
            CraftDistrictBlocks.Police.EQUIPMENT_RACK,
            CraftDistrictBlocks.Police.PISTOL_RACK,
            CraftDistrictBlocks.Police.RIFLE_RACK,
            CraftDistrictBlocks.Police.INTERROGATION_TABLE,
            CraftDistrictBlocks.Police.ONE_WAY_OBSERVATION_GLASS,
            CraftDistrictBlocks.Police.ONE_WAY_GLASS_BLOCK,
            CraftDistrictBlocks.Police.MUGSHOT_HEIGHT_CHART,
            CraftDistrictBlocks.Police.BOOKING_CAMERA,
            CraftDistrictBlocks.Police.DEPARTMENT_SIGN,
            CraftDistrictBlocks.Police.INVESTIGATION_BOARD,
            CraftDistrictBlocks.Police.SECURITY_CAMERA,
            CraftDistrictBlocks.FireStation.EXTINGUISHER,
            CraftDistrictBlocks.FireStation.FIRST_AID_CABINET,
            CraftDistrictBlocks.FireStation.WALL_CLOCK),
    FIRE_STATION("fire_station", CraftDistrictBlocks.FireStation.EXTINGUISHER,
            CraftDistrictBlocks.FireStation.DISPATCH_DESK,
            CraftDistrictBlocks.Police.OFFICE_CHAIR,
            CraftDistrictBlocks.FireStation.GEAR_LOCKER,
            CraftDistrictBlocks.FireStation.BREATHING_APPARATUS_RACK,
            CraftDistrictBlocks.FireStation.RESCUE_TOOLS_RACK,
            CraftDistrictBlocks.Police.RADIO_CHARGING_STATION,
            CraftDistrictBlocks.FireStation.HYDRAULIC_RESCUE_TOOL,
            CraftDistrictBlocks.FireStation.RESCUE_STRETCHER,
            CraftDistrictBlocks.FireStation.LOCKER_BENCH,
            CraftDistrictBlocks.FireStation.HOSE_STORAGE_RACK,
            CraftDistrictBlocks.FireStation.HOSE_REEL,
            CraftDistrictBlocks.FireStation.EXTINGUISHER,
            CraftDistrictBlocks.FireStation.DEPARTMENT_SIGN,
            CraftDistrictBlocks.FireStation.ALARM_CONTROL_PANEL,
            CraftDistrictBlocks.FireStation.WALL_CLOCK,
            CraftDistrictBlocks.FireStation.INCIDENT_BOARD,
            CraftDistrictBlocks.FireStation.FIRST_AID_CABINET,
    CraftDistrictBlocks.FireStation.STATION_POLE),
    SUPERMARKET("supermarket", CraftDistrictBlocks.Supermarket.SHOPPING_CART,
            CraftDistrictBlocks.Supermarket.GROCERY_SHELF,
            CraftDistrictBlocks.Supermarket.CANNED_SHELF,
            CraftDistrictBlocks.Supermarket.SNACK_SHELF,
            CraftDistrictBlocks.Supermarket.CANDY_SHELF,
            CraftDistrictBlocks.Supermarket.CLEANING_SUPPLIES_SHELF,
            CraftDistrictBlocks.Supermarket.PERSONAL_CARE_SHELF,
            CraftDistrictBlocks.Supermarket.PET_SUPPLIES_SHELF,
            CraftDistrictBlocks.Supermarket.PAPER_PRODUCTS_SHELF,
            CraftDistrictBlocks.Supermarket.HOUSEHOLD_SUPPLIES_SHELF,
            CraftDistrictBlocks.Supermarket.STATIONERY_SHELF,
            CraftDistrictBlocks.Supermarket.DOUBLE_SIDED_GONDOLA,
            CraftDistrictBlocks.Supermarket.HANGING_PRODUCTS_DISPLAY,
            CraftDistrictBlocks.Supermarket.FRUIT_STAND,
            CraftDistrictBlocks.Supermarket.VEGETABLE_STAND,
            CraftDistrictBlocks.Bakery.BREAD_SHELF,
            CraftDistrictBlocks.Supermarket.PROMOTIONAL_DISPLAY,
            CraftDistrictBlocks.Supermarket.CHECKOUT_CANDY_RACK,
            CraftDistrictBlocks.Supermarket.DRINKS_FRIDGE_A,
            CraftDistrictBlocks.Supermarket.DRINKS_FRIDGE_B,
            CraftDistrictBlocks.Supermarket.COLA_FRIDGE,
            CraftDistrictBlocks.Supermarket.BEER_FRIDGE,
            CraftDistrictBlocks.Supermarket.WINE_FRIDGE,
            CraftDistrictBlocks.Supermarket.WHISKEY_DISPLAY_CABINET,
            CraftDistrictBlocks.Supermarket.DAIRY_REFRIGERATOR,
            CraftDistrictBlocks.Supermarket.CHEST_FREEZER_FROZEN_FOOD,
            CraftDistrictBlocks.Supermarket.CHEST_FREEZER_ICE_CREAM,
            CraftDistrictBlocks.Supermarket.DELI_DISPLAY_COUNTER,
            CraftDistrictBlocks.Police.OFFICE_CHAIR,
            CraftDistrictBlocks.Supermarket.CHECKOUT,
            CraftDistrictBlocks.Supermarket.SELF_CHECKOUT,
            CraftDistrictBlocks.Supermarket.SHOPPING_CART,
            CraftDistrictBlocks.Supermarket.FILLED_SHOPPING_CART,
            CraftDistrictBlocks.Supermarket.BASKET_STACK,
            CraftDistrictBlocks.Supermarket.SECURITY_GATE,
            CraftDistrictBlocks.Supermarket.STOCK_PALLET,
            CraftDistrictBlocks.FireStation.EXTINGUISHER,
            CraftDistrictBlocks.FireStation.WALL_CLOCK),
    LIGHTING("lighting", CraftDistrictBlocks.Lighting.DESK_LAMP,
            CraftDistrictBlocks.Lighting.LED_CEILING_PANEL,
            CraftDistrictBlocks.Lighting.FLUORESCENT_TUBE,
            CraftDistrictBlocks.Lighting.UNDER_CABINET_STRIP,
            CraftDistrictBlocks.Lighting.RECESSED_SPOTLIGHT,
            CraftDistrictBlocks.Lighting.TRACK_LIGHT,
            CraftDistrictBlocks.Lighting.INDUSTRIAL_PENDANT,
            CraftDistrictBlocks.Lighting.MODERN_WALL_LIGHT,
            CraftDistrictBlocks.Lighting.INDUSTRIAL_FLOODLIGHT,
            CraftDistrictBlocks.Lighting.SECURITY_WALL_LIGHT,
            CraftDistrictBlocks.Lighting.EXIT_SIGN,
            CraftDistrictBlocks.Lighting.EXIT_SIGN_LEFT,
            CraftDistrictBlocks.Lighting.NEON_OPEN_SIGN,
            CraftDistrictBlocks.Lighting.DESK_LAMP,
            CraftDistrictBlocks.Lighting.OUTDOOR_BOLLARD),
    RESTAURANT("restaurant", CraftDistrictBlocks.Restaurant.SQUARE_DINING_TABLE,
            CraftDistrictBlocks.Restaurant.SQUARE_DINING_TABLE,
            CraftDistrictBlocks.Restaurant.ROUND_CAFE_TABLE,
            CraftDistrictBlocks.Restaurant.HIGH_BAR_TABLE,
            CraftDistrictBlocks.Restaurant.WOODEN_MODULAR_TABLE,
            CraftDistrictBlocks.Restaurant.INDUSTRIAL_MODULAR_TABLE,
            CraftDistrictBlocks.Restaurant.DINER_MODULAR_TABLE,
            CraftDistrictBlocks.Restaurant.TABLECLOTH_MODULAR_TABLE,
            CraftDistrictBlocks.Restaurant.WOODEN_DINING_CHAIR,
            CraftDistrictBlocks.Restaurant.MODERN_DINING_CHAIR,
            CraftDistrictBlocks.Restaurant.CUSHIONED_DINING_CHAIR,
            CraftDistrictBlocks.Restaurant.DINER_CHAIR,
            CraftDistrictBlocks.Restaurant.INDUSTRIAL_BAR_STOOL,
            CraftDistrictBlocks.Restaurant.CUSHIONED_BAR_CHAIR,
            CraftDistrictBlocks.Restaurant.DINER_BOOTH_SEAT,
            CraftDistrictBlocks.Restaurant.MODERN_BOOTH_SEAT,
            CraftDistrictBlocks.Restaurant.WOODEN_DINING_BENCH,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_COOKING_RANGE,
            CraftDistrictBlocks.Restaurant.KITCHEN_EXHAUST_HOOD,
            CraftDistrictBlocks.Restaurant.KITCHEN_EXHAUST_DUCT,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_DEEP_FRYER,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_GRIDDLE,
            CraftDistrictBlocks.Restaurant.STAINLESS_STEEL_PREP_TABLE,
            CraftDistrictBlocks.Restaurant.KITCHEN_SINK_STATION,
            CraftDistrictBlocks.Restaurant.RESTAURANT_SERVICE_COUNTER,
            CraftDistrictBlocks.Restaurant.FOOD_PICKUP_COUNTER,
            CraftDistrictBlocks.Restaurant.RESTAURANT_POS_TERMINAL,
            CraftDistrictBlocks.Restaurant.RESTAURANT_SERVING_CART,
            CraftDistrictBlocks.Restaurant.PLATE_CUTLERY_RACK,
            CraftDistrictBlocks.Restaurant.BEVERAGE_DISPENSER,
            CraftDistrictBlocks.Restaurant.CONDIMENT_STATION,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_COFFEE_MACHINE,
            CraftDistrictBlocks.Restaurant.KITCHEN_ORDER_DISPLAY,
            CraftDistrictBlocks.Restaurant.RESTAURANT_MENU_BOARD);

    private final String id;
    private final Supplier<? extends ItemLike> icon;
    private final List<Supplier<? extends ItemLike>> entries;
    private List<Item> cachedItems;

    @SafeVarargs
    DecorationCollection(String id, Supplier<? extends ItemLike> icon,
                         Supplier<? extends ItemLike>... entries) {
        this.id = id;
        this.icon = icon;
        this.entries = List.of(entries);
    }

    public String id() {
        return id;
    }

    public Component title() {
        return Component.translatable("collection.craftdistrict." + id);
    }

    public ItemStack icon() {
        return new ItemStack(icon.get());
    }

    public List<Item> items() {
        if (cachedItems == null) {
            List<Item> items = new ArrayList<>(entries.size());
            for (Supplier<? extends ItemLike> entry : entries) {
                Item item = entry.get().asItem();
                if (item == Items.AIR || items.contains(item)) {
                    throw new IllegalStateException("Invalid or duplicate item in collection " + id);
                }
                items.add(item);
            }
            cachedItems = List.copyOf(items);
        }
        return cachedItems;
    }
}
