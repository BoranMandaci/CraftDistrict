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
            // Counters and displays.
            CraftDistrictBlocks.Bakery.DISPLAY_COUNTER,
            CraftDistrictBlocks.Bakery.REFRIGERATED_CAKE_DISPLAY,
            CraftDistrictBlocks.Bakery.REFRIGERATED_PASTRY_DISPLAY,
            CraftDistrictBlocks.Bakery.CASH_REGISTER,
            // Bread storage and pastry trays.
            CraftDistrictBlocks.Bakery.BREAD_SHELF,
            CraftDistrictBlocks.Bakery.TRAY_RACK_EMPTY,
            CraftDistrictBlocks.Bakery.TRAY_RACK_BAKED,
            CraftDistrictBlocks.Bakery.EMPTY_BAKING_TRAY,
            CraftDistrictBlocks.Bakery.CROISSANT_TRAY,
            CraftDistrictBlocks.Bakery.COOKIE_TRAY,
            CraftDistrictBlocks.Bakery.BREAD_ROLLS_TRAY,
            // Baking machines.
            CraftDistrictBlocks.Bakery.INDUSTRIAL_OVEN,
            CraftDistrictBlocks.Bakery.INDUSTRIAL_DOUGH_MIXER,
            CraftDistrictBlocks.Bakery.BREAD_SLICER,
            // Preparation, ingredients and tools.
            CraftDistrictBlocks.Bakery.DOUGH_PREPARATION_TABLE,
            CraftDistrictBlocks.Bakery.FLOUR_SACKS_SMALL,
            CraftDistrictBlocks.Bakery.FLOUR_SACKS_STACKED,
            CraftDistrictBlocks.Bakery.FLOUR_SACKS_PILE,
            CraftDistrictBlocks.Bakery.BREAD_PEEL_RACK,
            CraftDistrictBlocks.Bakery.UTENSIL_WALL_RACK,
            CraftDistrictBlocks.Bakery.ROLLING_PIN_RACK,
            // Shop signs and accessories.
            CraftDistrictBlocks.Bakery.HANGING_SHOP_SIGN,
            CraftDistrictBlocks.Bakery.WALL_SHOP_SIGN,
            CraftDistrictBlocks.Bakery.WALL_MENU_BOARD,
            CraftDistrictBlocks.Bakery.DAILY_SPECIAL_BOARD,
            CraftDistrictBlocks.Bakery.SIDEWALK_MENU_BOARD,
            CraftDistrictBlocks.FireStation.WALL_CLOCK),
    POLICE_STATION("police_station", CraftDistrictBlocks.Police.RECEPTION_DESK,
            // Reception and waiting.
            CraftDistrictBlocks.Police.RECEPTION_DESK,
            CraftDistrictBlocks.Police.WAITING_BENCH,
            // Office and dispatch.
            CraftDistrictBlocks.Police.DISPATCH_CONSOLE,
            CraftDistrictBlocks.Police.OFFICE_CHAIR,
            // Investigation and interrogation.
            CraftDistrictBlocks.Police.INVESTIGATION_BOARD,
            CraftDistrictBlocks.Police.EVIDENCE_STORAGE_SHELF,
            CraftDistrictBlocks.Police.INTERROGATION_TABLE,
            CraftDistrictBlocks.Police.BLUE_CHAIR,
            CraftDistrictBlocks.Police.ONE_WAY_OBSERVATION_GLASS,
            CraftDistrictBlocks.Police.ONE_WAY_GLASS_BLOCK,
            // Booking and holding cells.
            CraftDistrictBlocks.Police.FINGERPRINT_STATION,
            CraftDistrictBlocks.Police.MUGSHOT_HEIGHT_CHART,
            CraftDistrictBlocks.Police.BOOKING_CAMERA,
            CraftDistrictBlocks.Police.HOLDING_CELL_BARS,
            CraftDistrictBlocks.Police.HOLDING_CELL_DOOR,
            CraftDistrictBlocks.Police.HOLDING_CELL_BED,
            CraftDistrictBlocks.Police.HOLDING_CELL_TOILET,
            // Equipment storage and armory.
            CraftDistrictBlocks.Police.LOCKERS,
            CraftDistrictBlocks.FireStation.LOCKER_BENCH,
            CraftDistrictBlocks.Police.RADIO_CHARGING_STATION,
            CraftDistrictBlocks.Police.EQUIPMENT_RACK,
            CraftDistrictBlocks.Police.PISTOL_RACK,
            CraftDistrictBlocks.Police.RIFLE_RACK,
            // Signs, security and safety.
            CraftDistrictBlocks.Police.DEPARTMENT_SIGN,
            CraftDistrictBlocks.Police.SECURITY_CAMERA,
            CraftDistrictBlocks.FireStation.EXTINGUISHER,
            CraftDistrictBlocks.FireStation.FIRST_AID_CABINET,
            CraftDistrictBlocks.FireStation.WALL_CLOCK),
    FIRE_STATION("fire_station", CraftDistrictBlocks.FireStation.EXTINGUISHER,
            // Gear storage.
            CraftDistrictBlocks.FireStation.GEAR_LOCKER,
            CraftDistrictBlocks.FireStation.BREATHING_APPARATUS_RACK,
            CraftDistrictBlocks.FireStation.LOCKER_BENCH,
            // Hose and fire suppression equipment.
            CraftDistrictBlocks.FireStation.HOSE_STORAGE_RACK,
            CraftDistrictBlocks.FireStation.HOSE_REEL,
            CraftDistrictBlocks.FireStation.EXTINGUISHER,
            // Rescue tools and first aid.
            CraftDistrictBlocks.FireStation.RESCUE_TOOLS_RACK,
            CraftDistrictBlocks.FireStation.HYDRAULIC_RESCUE_TOOL,
            CraftDistrictBlocks.FireStation.RESCUE_STRETCHER,
            CraftDistrictBlocks.FireStation.FIRST_AID_CABINET,
            // Dispatch and alarms.
            CraftDistrictBlocks.FireStation.DISPATCH_DESK,
            CraftDistrictBlocks.Police.OFFICE_CHAIR,
            CraftDistrictBlocks.Police.RADIO_CHARGING_STATION,
            CraftDistrictBlocks.FireStation.ALARM_CONTROL_PANEL,
            CraftDistrictBlocks.FireStation.INCIDENT_BOARD,
            // Station fixtures.
            CraftDistrictBlocks.FireStation.STATION_POLE,
            CraftDistrictBlocks.FireStation.DEPARTMENT_SIGN,
            CraftDistrictBlocks.FireStation.WALL_CLOCK),
    SUPERMARKET("supermarket", CraftDistrictBlocks.Supermarket.SHOPPING_CART,
            // Aisle shelving and stocked displays.
            CraftDistrictBlocks.Supermarket.GROCERY_SHELF,
            CraftDistrictBlocks.Supermarket.CANNED_SHELF,
            CraftDistrictBlocks.Supermarket.SNACK_SHELF,
            CraftDistrictBlocks.Supermarket.CANDY_SHELF,
            CraftDistrictBlocks.Supermarket.DOUBLE_SIDED_GONDOLA,
            CraftDistrictBlocks.Supermarket.CLEANING_SUPPLIES_SHELF,
            CraftDistrictBlocks.Supermarket.PERSONAL_CARE_SHELF,
            CraftDistrictBlocks.Supermarket.PET_SUPPLIES_SHELF,
            CraftDistrictBlocks.Supermarket.PAPER_PRODUCTS_SHELF,
            CraftDistrictBlocks.Supermarket.HOUSEHOLD_SUPPLIES_SHELF,
            CraftDistrictBlocks.Supermarket.STATIONERY_SHELF,
            CraftDistrictBlocks.Supermarket.HANGING_PRODUCTS_DISPLAY,
            // Fresh produce, bakery and promotions.
            CraftDistrictBlocks.Supermarket.FRUIT_STAND,
            CraftDistrictBlocks.Supermarket.VEGETABLE_STAND,
            CraftDistrictBlocks.Bakery.BREAD_SHELF,
            CraftDistrictBlocks.Supermarket.PROMOTIONAL_DISPLAY,
            // Refrigeration and beverage displays.
            CraftDistrictBlocks.Supermarket.DAIRY_REFRIGERATOR,
            CraftDistrictBlocks.Supermarket.DELI_DISPLAY_COUNTER,
            CraftDistrictBlocks.Supermarket.DRINKS_FRIDGE_A,
            CraftDistrictBlocks.Supermarket.DRINKS_FRIDGE_B,
            CraftDistrictBlocks.Supermarket.COLA_FRIDGE,
            CraftDistrictBlocks.Supermarket.BEER_FRIDGE,
            CraftDistrictBlocks.Supermarket.WINE_FRIDGE,
            CraftDistrictBlocks.Supermarket.WHISKEY_DISPLAY_CABINET,
            CraftDistrictBlocks.Supermarket.CHEST_FREEZER_FROZEN_FOOD,
            CraftDistrictBlocks.Supermarket.CHEST_FREEZER_ICE_CREAM,
            // Checkout equipment.
            CraftDistrictBlocks.Supermarket.CHECKOUT,
            CraftDistrictBlocks.Supermarket.SELF_CHECKOUT,
            CraftDistrictBlocks.Police.OFFICE_CHAIR,
            CraftDistrictBlocks.Supermarket.CHECKOUT_CANDY_RACK,
            // Shopping equipment.
            CraftDistrictBlocks.Supermarket.SHOPPING_CART,
            CraftDistrictBlocks.Supermarket.FILLED_SHOPPING_CART,
            CraftDistrictBlocks.Supermarket.BASKET_STACK,
            // Stockroom, entrance and safety.
            CraftDistrictBlocks.Supermarket.STOCK_PALLET,
            CraftDistrictBlocks.Supermarket.SECURITY_GATE,
            CraftDistrictBlocks.FireStation.EXTINGUISHER,
            CraftDistrictBlocks.FireStation.WALL_CLOCK),
    LIGHTING("lighting", CraftDistrictBlocks.Lighting.DESK_LAMP,
            // Ceiling lights.
            CraftDistrictBlocks.Lighting.LED_CEILING_PANEL,
            CraftDistrictBlocks.Lighting.FLUORESCENT_TUBE,
            CraftDistrictBlocks.Lighting.RECESSED_SPOTLIGHT,
            CraftDistrictBlocks.Lighting.TRACK_LIGHT,
            CraftDistrictBlocks.Lighting.INDUSTRIAL_PENDANT,
            CraftDistrictBlocks.Lighting.MODERN_CHANDELIER,
            CraftDistrictBlocks.Lighting.CEILING_FAN_WITH_LIGHT,
            // Wall lights.
            CraftDistrictBlocks.Lighting.MODERN_WALL_LIGHT,
            CraftDistrictBlocks.Lighting.INDUSTRIAL_FLOODLIGHT,
            CraftDistrictBlocks.Lighting.SECURITY_WALL_LIGHT,
            CraftDistrictBlocks.Lighting.DECORATIVE_WALL_SCONCE,
            // Desk and task lights.
            CraftDistrictBlocks.Lighting.DESK_LAMP,
            CraftDistrictBlocks.Lighting.UNDER_CABINET_STRIP,
            CraftDistrictBlocks.Lighting.MODERN_FLOOR_LAMP,
            // Outdoor lighting.
            CraftDistrictBlocks.Lighting.OUTDOOR_BOLLARD,
            CraftDistrictBlocks.Lighting.VINTAGE_STREET_LAMP,
            CraftDistrictBlocks.Lighting.GARDEN_PATH_LIGHT,
            // Signs and indicators.
            CraftDistrictBlocks.Lighting.EXIT_SIGN,
            CraftDistrictBlocks.Lighting.EXIT_SIGN_LEFT,
            CraftDistrictBlocks.Lighting.NEON_OPEN_SIGN,
            CraftDistrictBlocks.Lighting.EMERGENCY_BEACON,
            CraftDistrictBlocks.Lighting.INDUSTRIAL_WARNING_LIGHT,
            // Connected decorative lighting.
            CraftDistrictBlocks.Lighting.NEON_LIGHT_STRIP,
            CraftDistrictBlocks.Lighting.HANGING_STRING_LIGHTS),
    RESTAURANT("restaurant", CraftDistrictBlocks.Restaurant.SQUARE_DINING_TABLE,
            // Dining tables.
            CraftDistrictBlocks.Restaurant.SQUARE_DINING_TABLE,
            CraftDistrictBlocks.Restaurant.ROUND_CAFE_TABLE,
            CraftDistrictBlocks.Restaurant.HIGH_BAR_TABLE,
            CraftDistrictBlocks.Restaurant.WOODEN_MODULAR_TABLE,
            CraftDistrictBlocks.Restaurant.INDUSTRIAL_MODULAR_TABLE,
            CraftDistrictBlocks.Restaurant.DINER_MODULAR_TABLE,
            CraftDistrictBlocks.Restaurant.TABLECLOTH_MODULAR_TABLE,
            // Chairs and seating.
            CraftDistrictBlocks.Restaurant.WOODEN_DINING_CHAIR,
            CraftDistrictBlocks.Restaurant.MODERN_DINING_CHAIR,
            CraftDistrictBlocks.Restaurant.CUSHIONED_DINING_CHAIR,
            CraftDistrictBlocks.Restaurant.DINER_CHAIR,
            CraftDistrictBlocks.Restaurant.INDUSTRIAL_BAR_STOOL,
            CraftDistrictBlocks.Restaurant.CUSHIONED_BAR_CHAIR,
            CraftDistrictBlocks.Restaurant.DINER_BOOTH_SEAT,
            CraftDistrictBlocks.Restaurant.MODERN_BOOTH_SEAT,
            CraftDistrictBlocks.Restaurant.WOODEN_DINING_BENCH,
            // Service counters and payment.
            CraftDistrictBlocks.Restaurant.RESTAURANT_SERVICE_COUNTER,
            CraftDistrictBlocks.Restaurant.FOOD_PICKUP_COUNTER,
            CraftDistrictBlocks.Restaurant.RESTAURANT_POS_TERMINAL,
            CraftDistrictBlocks.Bakery.CASH_REGISTER,
            // Service accessories.
            CraftDistrictBlocks.Restaurant.RESTAURANT_SERVING_CART,
            CraftDistrictBlocks.Restaurant.RESTAURANT_MENU_BOARD,
            CraftDistrictBlocks.Restaurant.PLATE_CUTLERY_RACK,
            CraftDistrictBlocks.Restaurant.CONDIMENT_STATION,
            CraftDistrictBlocks.Restaurant.BEVERAGE_DISPENSER,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_COFFEE_MACHINE,
            // Cooking equipment.
            CraftDistrictBlocks.Restaurant.COMMERCIAL_COOKING_RANGE,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_GRIDDLE,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_DEEP_FRYER,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_PIZZA_OVEN,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_MICROWAVE,
            // Kitchen workstations.
            CraftDistrictBlocks.Restaurant.STAINLESS_STEEL_PREP_TABLE,
            CraftDistrictBlocks.Restaurant.KITCHEN_SINK_STATION,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_DISHWASHER,
            CraftDistrictBlocks.Restaurant.CUTTING_BOARD_STATION,
            // Storage and refrigeration.
            CraftDistrictBlocks.Restaurant.COMMERCIAL_REFRIGERATOR,
            CraftDistrictBlocks.Restaurant.INGREDIENT_STORAGE_SHELF,
            // Kitchen accessories.
            CraftDistrictBlocks.Restaurant.POT_PAN_RACK,
            CraftDistrictBlocks.Restaurant.KITCHEN_KNIFE_RACK,
            CraftDistrictBlocks.Restaurant.SPICE_RACK,
            CraftDistrictBlocks.Restaurant.DISH_DRYING_RACK,
            CraftDistrictBlocks.Restaurant.KITCHEN_TRASH_BIN,
            // Ventilation and displays.
            CraftDistrictBlocks.Restaurant.KITCHEN_EXHAUST_HOOD,
            CraftDistrictBlocks.Restaurant.KITCHEN_EXHAUST_DUCT,
            CraftDistrictBlocks.Restaurant.KITCHEN_ORDER_DISPLAY,
            CraftDistrictBlocks.FireStation.WALL_CLOCK),
    SURVIVAL_CRAFTING("survival_crafting", CraftDistrictBlocks.Survival.ASSEMBLY_STATION,
            CraftDistrictItems.FURNITURE_PARTS,
            CraftDistrictItems.METAL_PARTS,
            CraftDistrictItems.ELECTRONIC_PARTS,
            CraftDistrictBlocks.Survival.ASSEMBLY_STATION);

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
