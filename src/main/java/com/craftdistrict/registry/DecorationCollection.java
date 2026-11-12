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
            CraftDistrictBlocks.Bakery.BREAD_STACK,
            // Baking machines.
            CraftDistrictBlocks.Bakery.INDUSTRIAL_OVEN,
            CraftDistrictBlocks.Bakery.INDUSTRIAL_DOUGH_MIXER,
            CraftDistrictBlocks.Bakery.DOUGH_SHEETER,
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
            CraftDistrictBlocks.Police.CHIEF_DESK,
            CraftDistrictBlocks.Police.DETECTIVE_DESK,
            CraftDistrictBlocks.Police.DISPATCH_CONSOLE,
            CraftDistrictBlocks.Police.OFFICE_CHAIR,
            CraftDistrictBlocks.Police.CASE_FILE_CABINET,
            // Investigation and interrogation.
            CraftDistrictBlocks.Police.INVESTIGATION_BOARD,
            CraftDistrictBlocks.Police.WANTED_POSTERS,
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
            CraftDistrictBlocks.Police.SECURITY_MONITOR_WALL_UNIT,
            CraftDistrictBlocks.Police.VISITOR_SECURITY_SCANNER,
            CraftDistrictBlocks.FireStation.EXTINGUISHER,
            CraftDistrictBlocks.Lighting.PULL_DOWN_FIRE_ALARM,
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
            CraftDistrictBlocks.Lighting.PULL_DOWN_FIRE_ALARM,
            // Rescue tools and first aid.
            CraftDistrictBlocks.FireStation.RESCUE_TOOLS_RACK,
            CraftDistrictBlocks.FireStation.HYDRAULIC_RESCUE_TOOL,
            CraftDistrictBlocks.FireStation.RESCUE_STRETCHER,
            CraftDistrictBlocks.FireStation.FIRST_AID_CABINET,
            // Dispatch and alarms.
            CraftDistrictBlocks.FireStation.CHIEF_DESK,
            CraftDistrictBlocks.FireStation.INVESTIGATOR_DESK,
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
            CraftDistrictBlocks.Supermarket.ICE_MERCHANDISER,
            // Checkout equipment.
            CraftDistrictBlocks.Supermarket.CHECKOUT,
            CraftDistrictBlocks.Supermarket.SELF_CHECKOUT,
            CraftDistrictBlocks.Supermarket.BOTTLE_RETURN_MACHINE,
            CraftDistrictBlocks.Supermarket.QUEUE_BARRIER,
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
            CraftDistrictBlocks.Lighting.EXIT_SIGN_NO_ARROW,
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
            CraftDistrictBlocks.Restaurant.RESTAURANT_HOST_STAND,
            CraftDistrictBlocks.Restaurant.RESTAURANT_SERVICE_COUNTER,
            CraftDistrictBlocks.Restaurant.FOOD_PICKUP_COUNTER,
            CraftDistrictBlocks.Restaurant.SALAD_BAR_COUNTER,
            CraftDistrictBlocks.Restaurant.HOT_BUFFET_SERVING_STATION,
            CraftDistrictBlocks.Restaurant.RESTAURANT_POS_TERMINAL,
            CraftDistrictBlocks.Bakery.CASH_REGISTER,
            // Service accessories.
            CraftDistrictBlocks.Restaurant.RESTAURANT_SERVING_CART,
            CraftDistrictBlocks.Restaurant.RESTAURANT_MENU_BOARD,
            CraftDistrictBlocks.Restaurant.PLATE_CUTLERY_RACK,
            CraftDistrictBlocks.Restaurant.CONDIMENT_STATION,
            CraftDistrictBlocks.Restaurant.BEVERAGE_DISPENSER,
            CraftDistrictBlocks.Restaurant.SOFT_DRINK_DISPENSER,
            CraftDistrictBlocks.Restaurant.DISPOSABLE_CUPS,
            CraftDistrictBlocks.Restaurant.BLENDER,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_COFFEE_MACHINE,
            // Cooking equipment.
            CraftDistrictBlocks.Restaurant.COMMERCIAL_COOKING_RANGE,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_GRIDDLE,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_DEEP_FRYER,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_PIZZA_OVEN,
            CraftDistrictBlocks.Restaurant.EMPTY_COMMERCIAL_PIZZA_OVEN,
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
    OFFICE("office", CraftDistrictBlocks.Office.WORKSTATION,
            // Desks and meeting tables.
            CraftDistrictBlocks.Office.WORKSTATION,
            CraftDistrictBlocks.Office.CUBICLE,
            CraftDistrictBlocks.Office.EXECUTIVE_DESK,
            CraftDistrictBlocks.Office.RECEPTION_DESK,
            CraftDistrictBlocks.Office.CONFERENCE_TABLE,
            CraftDistrictBlocks.Office.CONFERENCE_TABLE_CENTER,
            // Seating and lounge furniture.
            CraftDistrictBlocks.Police.OFFICE_CHAIR,
            CraftDistrictBlocks.Police.WAITING_BENCH,
            CraftDistrictBlocks.Office.LOUNGE_SOFA,
            CraftDistrictBlocks.Office.COFFEE_TABLE,
            // Office equipment and electronics.
            CraftDistrictBlocks.Office.PRINTER,
            CraftDistrictBlocks.Office.SERVER_RACK,
            CraftDistrictBlocks.Office.PRESENTATION_SCREEN,
            CraftDistrictBlocks.Office.MOBILE_WHITEBOARD,
            CraftDistrictBlocks.Office.CONFERENCE_SPEAKER,
            // Storage and reading materials.
            CraftDistrictBlocks.Police.CASE_FILE_CABINET,
            CraftDistrictBlocks.Office.MAIL_SORTING_CABINET,
            CraftDistrictBlocks.Office.SUPPLY_CABINET,
            CraftDistrictBlocks.Office.BOOKSHELF,
            CraftDistrictBlocks.Office.MAGAZINE_RACK,
            // Kitchenette and break-room utilities.
            CraftDistrictBlocks.Office.KITCHENETTE_BASE_COUNTER,
            CraftDistrictBlocks.Office.KITCHENETTE_WALL_COUNTER,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_COFFEE_MACHINE,
            CraftDistrictBlocks.Restaurant.COMMERCIAL_MICROWAVE,
            CraftDistrictBlocks.Office.WATER_COOLER,
            CraftDistrictBlocks.Office.SNACK_VENDING_MACHINE,
            CraftDistrictBlocks.Office.DRINK_VENDING_MACHINE,
            CraftDistrictBlocks.Office.MIXED_VENDING_MACHINE,
            CraftDistrictBlocks.Office.RECYCLING_STATION,
            // Safety, accessories and access control.
            CraftDistrictBlocks.FireStation.WALL_CLOCK,
            CraftDistrictBlocks.FireStation.EXTINGUISHER,
            CraftDistrictBlocks.Lighting.PULL_DOWN_FIRE_ALARM,
            CraftDistrictBlocks.FireStation.FIRST_AID_CABINET,
            CraftDistrictBlocks.Office.COAT_RACK,
            CraftDistrictBlocks.Office.PLANTER,
            CraftDistrictBlocks.Office.SECURITY_TURNSTILE,
            CraftDistrictBlocks.Office.EMPLOYEE_BADGE_READER,
            // Partitions and architectural pieces.
            CraftDistrictBlocks.Office.ACOUSTIC_DIVIDER,
            CraftDistrictBlocks.Office.GLASS_PARTITION,
            CraftDistrictBlocks.Office.GLASS_PARTITION_DOOR,
            CraftDistrictBlocks.Office.WINDOW_BLINDS),
    SURVIVAL_CRAFTING("survival_crafting", CraftDistrictBlocks.Survival.ASSEMBLY_STATION,
            CraftDistrictItems.FURNITURE_PARTS,
            CraftDistrictItems.METAL_PARTS,
            CraftDistrictItems.ELECTRONIC_PARTS,
            CraftDistrictItems.WRENCH,
            CraftDistrictBlocks.Survival.ASSEMBLY_STATION),
    HOSPITAL("hospital", CraftDistrictBlocks.Hospital.BED,
            // Patient rooms and bedside furniture.
            CraftDistrictBlocks.Hospital.BED,
            CraftDistrictBlocks.Hospital.BED_HEAD_PANEL,
            CraftDistrictBlocks.Hospital.BEDSIDE_CABINET,
            CraftDistrictBlocks.Hospital.OVERBED_TABLE,
            CraftDistrictBlocks.Hospital.INCUBATOR,
            CraftDistrictBlocks.Hospital.PRIVACY_CURTAIN,
            // Reception, nursing and waiting areas.
            CraftDistrictBlocks.Hospital.RECEPTION_DESK,
            CraftDistrictBlocks.Hospital.NURSE_COUNTER,
            CraftDistrictBlocks.Police.WAITING_BENCH,
            // Examination and treatment.
            CraftDistrictBlocks.Hospital.EXAMINATION_TABLE,
            CraftDistrictBlocks.Hospital.DENTAL_CHAIR,
            CraftDistrictBlocks.Hospital.INSTRUMENT_TROLLEY,
            CraftDistrictBlocks.Hospital.SUPPLY_CABINET,
            // Monitoring, patient transport and emergency equipment.
            CraftDistrictBlocks.Hospital.PATIENT_MONITOR,
            CraftDistrictBlocks.Hospital.IV_STAND,
            CraftDistrictBlocks.Hospital.EKG_MACHINE,
            CraftDistrictBlocks.Hospital.WHEELCHAIR,
            CraftDistrictBlocks.Hospital.STRETCHER,
            CraftDistrictBlocks.FireStation.RESCUE_STRETCHER,
            CraftDistrictBlocks.Hospital.DEFIBRILLATOR_CART,
            CraftDistrictBlocks.Hospital.CRASH_CART,
            // Operating rooms and sterile equipment.
            CraftDistrictBlocks.Hospital.SURGICAL_TABLE,
            CraftDistrictBlocks.Hospital.SURGICAL_LIGHT,
            CraftDistrictBlocks.Hospital.ANESTHESIA_MACHINE,
            CraftDistrictBlocks.Hospital.ELECTROSURGICAL_UNIT,
            CraftDistrictBlocks.Hospital.ENDOSCOPY_UNIT,
            CraftDistrictBlocks.Hospital.INSTRUMENT_TRAY,
            CraftDistrictBlocks.Hospital.STERILE_CABINET,
            CraftDistrictBlocks.Hospital.AUTOCLAVE,
            // Radiology and imaging.
            CraftDistrictBlocks.Hospital.MRI_SCANNER,
            CraftDistrictBlocks.Hospital.CT_SCANNER,
            CraftDistrictBlocks.Hospital.MOBILE_XRAY_MACHINE,
            CraftDistrictBlocks.Hospital.C_ARM_XRAY_MACHINE,
            CraftDistrictBlocks.Hospital.ULTRASOUND_MACHINE,
            CraftDistrictBlocks.Hospital.MAMMOGRAPHY_MACHINE,
            CraftDistrictBlocks.Hospital.RADIOLOGY_CONSOLE,
            CraftDistrictBlocks.Hospital.LEAD_APRON_RACK,
            CraftDistrictBlocks.Hospital.IMAGING_DISPLAY,
            // Hygiene, wall utilities and safety accessories.
            CraftDistrictBlocks.Hospital.HANDWASH_STATION,
            CraftDistrictBlocks.Hospital.SANITIZER_DISPENSER,
            CraftDistrictBlocks.Hospital.DEPARTMENT_SIGN,
            CraftDistrictBlocks.FireStation.WALL_CLOCK,
            CraftDistrictBlocks.FireStation.EXTINGUISHER,
            CraftDistrictBlocks.Lighting.PULL_DOWN_FIRE_ALARM,
            CraftDistrictBlocks.FireStation.FIRST_AID_CABINET),
    PHARMACY("pharmacy", CraftDistrictBlocks.Pharmacy.SIGN,
            CraftDistrictBlocks.Pharmacy.SIGN,
            CraftDistrictBlocks.Pharmacy.COUNTER,
            CraftDistrictBlocks.Pharmacy.INFORMATION_BOARD,
            CraftDistrictBlocks.Pharmacy.QUEUE_TICKET_DISPENSER,
            CraftDistrictBlocks.Pharmacy.BLOOD_PRESSURE_KIOSK,
            CraftDistrictBlocks.Pharmacy.MEDICINE_WALL_SHELF,
            CraftDistrictBlocks.Pharmacy.OTC_DISPLAY_SHELF,
            CraftDistrictBlocks.Pharmacy.PROMOTIONAL_STAND,
            CraftDistrictBlocks.Pharmacy.GLASS_DISPLAY,
            CraftDistrictBlocks.Pharmacy.PREPARATION_STATION,
            CraftDistrictBlocks.Pharmacy.PILL_COUNTING_STATION,
            CraftDistrictBlocks.Pharmacy.MEDICINE_STACK,
            CraftDistrictBlocks.Pharmacy.DELIVERY_CART,
            CraftDistrictBlocks.Pharmacy.DRAWER_CABINET,
            CraftDistrictBlocks.Pharmacy.REFRIGERATOR,
            CraftDistrictBlocks.Pharmacy.SECURED_CABINET),
    HOTEL("hotel", CraftDistrictBlocks.Hotel.RECEPTION_COUNTER,
            CraftDistrictBlocks.Hotel.RECEPTION_COUNTER,
            CraftDistrictBlocks.Hotel.BACK_CABINET,
            CraftDistrictBlocks.Hotel.SERVICE_BELL,
            CraftDistrictBlocks.Hotel.KEY_CARD_ENCODER,
            CraftDistrictBlocks.Hotel.LOGO_SIGN,
            CraftDistrictBlocks.Hotel.LOBBY_SOFA,
            CraftDistrictBlocks.Hotel.LOBBY_ARMCHAIR,
            CraftDistrictBlocks.Hotel.COFFEE_TABLE,
            CraftDistrictBlocks.Hotel.BROCHURE_STAND,
            CraftDistrictBlocks.Hotel.LUGGAGE_CART,
            CraftDistrictBlocks.Hotel.LUGGAGE_CART_LOADED,
            CraftDistrictBlocks.Hotel.LUGGAGE_STACK,
            CraftDistrictBlocks.Hotel.SINGLE_BED,
            CraftDistrictBlocks.Hotel.DOUBLE_BED,
            CraftDistrictBlocks.Hotel.KING_BED,
            CraftDistrictBlocks.Hotel.HEADBOARD,
            CraftDistrictBlocks.Hotel.BEDSIDE_CABINET,
            CraftDistrictBlocks.Hotel.BEDSIDE_LAMP,
            CraftDistrictBlocks.Hotel.READING_LIGHT,
            CraftDistrictBlocks.Hotel.FOOT_BENCH,
            CraftDistrictBlocks.Hotel.WRITING_DESK,
            CraftDistrictBlocks.Hotel.DESK_CHAIR,
            CraftDistrictBlocks.Hotel.WARDROBE,
            CraftDistrictBlocks.Hotel.TV_CONSOLE,
            CraftDistrictBlocks.Hotel.STANDING_TV,
            CraftDistrictBlocks.Hotel.WALL_TV,
            CraftDistrictBlocks.Hotel.MINI_FRIDGE,
            CraftDistrictBlocks.Hotel.ROOM_SAFE,
            CraftDistrictBlocks.Hotel.TELEPHONE,
            CraftDistrictBlocks.Hotel.KETTLE_SET,
            CraftDistrictBlocks.Hotel.SERVICE_TRAY,
            CraftDistrictBlocks.Hotel.AC_UNIT,
            CraftDistrictBlocks.Hotel.THERMOSTAT,
            CraftDistrictBlocks.Hotel.CURTAINS,
            CraftDistrictBlocks.Hotel.MIRROR,
            CraftDistrictBlocks.Hotel.RUG,
            CraftDistrictBlocks.Hotel.VANITY_SINK,
            CraftDistrictBlocks.Hotel.VANITY_MIRROR,
            CraftDistrictBlocks.Hotel.TOILETRIES,
            CraftDistrictBlocks.Hotel.SHOWER_ENCLOSURE,
            CraftDistrictBlocks.Hotel.FIXED_SHOWER_GLASS,
            CraftDistrictBlocks.Hotel.SHOWER_FIXTURE,
            CraftDistrictBlocks.Hotel.SHOWER_BASE,
            CraftDistrictBlocks.Hotel.SHOWER_BASE_PLAIN,
            CraftDistrictBlocks.Hotel.BATHTUB,
            CraftDistrictBlocks.Hotel.JACUZZI,
            CraftDistrictBlocks.Hotel.MASSAGE_TABLE,
            CraftDistrictBlocks.Hotel.SAUNA_BENCH,
            CraftDistrictBlocks.Hotel.SAUNA_HEATER,
            CraftDistrictBlocks.Hotel.POOL_WATERFALL,
            CraftDistrictBlocks.Hotel.POOL_OVERFLOW_DRAIN,
            CraftDistrictBlocks.Hotel.SWIMMING_POOL_LADDER,
            CraftDistrictBlocks.Hotel.POOLSIDE_SHOWER,
            CraftDistrictBlocks.Hotel.SUN_LOUNGER,
            CraftDistrictBlocks.Hotel.POOL_UMBRELLA,
            CraftDistrictBlocks.Hotel.HEATED_TOWEL_RACK,
            CraftDistrictBlocks.Hotel.PUBLIC_SINK_COUNTER,
            CraftDistrictBlocks.Hotel.RESTROOM_MIRROR,
            CraftDistrictBlocks.Hotel.WALL_SOAP_DISPENSER,
            CraftDistrictBlocks.Hotel.PAPER_TOWEL_DISPENSER,
            CraftDistrictBlocks.Hotel.RESTROOM_TRASH_BIN,
            CraftDistrictBlocks.Hotel.TOILET,
            CraftDistrictBlocks.Hotel.URINAL,
            CraftDistrictBlocks.Hotel.URINAL_DIVIDER,
            CraftDistrictBlocks.Hotel.HAND_DRYER,
            CraftDistrictBlocks.Hotel.RESTROOM_SIGN_WOMEN,
            CraftDistrictBlocks.Hotel.RESTROOM_SIGN_MEN,
            CraftDistrictBlocks.Hotel.RESTROOM_SIGN_ACCESSIBLE,
            CraftDistrictBlocks.Hotel.PAPER_HOLDER,
            CraftDistrictBlocks.Hotel.TOWEL_RAIL,
            CraftDistrictBlocks.Hotel.TOWEL_STACK,
            CraftDistrictBlocks.Hotel.BATHROBE_HOOK,
            CraftDistrictBlocks.Hotel.HAIR_DRYER,
            CraftDistrictBlocks.Hotel.TRASH_BIN,
            CraftDistrictBlocks.Hotel.ROOM_DOOR,
            CraftDistrictBlocks.Hotel.ROOM_NUMBER_PLAQUE,
            CraftDistrictBlocks.Hotel.ELEVATOR_DOORS,
            CraftDistrictBlocks.Hotel.ELEVATOR_CALL_PANEL,
            CraftDistrictBlocks.Hotel.ELEVATOR_INDICATOR,
            CraftDistrictBlocks.Hotel.DIRECTIONAL_SIGN,
            CraftDistrictBlocks.Hotel.EVACUATION_MAP,
            CraftDistrictBlocks.Hotel.WALL_ART,
            CraftDistrictBlocks.Hotel.CARPET_RUNNER,
            CraftDistrictBlocks.Hotel.HOUSEKEEPING_CART,
            CraftDistrictBlocks.Hotel.LINEN_TROLLEY,
            CraftDistrictBlocks.Hotel.CLEANING_CABINET,
            CraftDistrictBlocks.Hotel.WASHING_MACHINE,
            CraftDistrictBlocks.Hotel.DRYER,
            CraftDistrictBlocks.Hotel.LAUNDRY_BASKET,
            CraftDistrictBlocks.Hotel.IRONING_BOARD);

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
