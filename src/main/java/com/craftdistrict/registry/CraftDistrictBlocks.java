package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.block.WideDecorationBlock;
import com.craftdistrict.block.TallDecorationBlock;
import com.craftdistrict.block.WideTallDecorationBlock;
import com.craftdistrict.block.CashRegisterBlock;
import com.craftdistrict.block.DoughPreparationTableBlock;
import com.craftdistrict.block.DoughSheeterBlock;
import com.craftdistrict.block.FlourSacksBlock;
import com.craftdistrict.block.PastryTrayBlock;
import com.craftdistrict.block.IndustrialDoughMixerBlock;
import com.craftdistrict.block.BakerySignageBlock;
import com.craftdistrict.block.BakeryTrayRackBlock;
import com.craftdistrict.block.RefrigeratedDisplayBlock;
import com.craftdistrict.block.BakeryWallToolBlock;
import com.craftdistrict.block.BreadSlicerBlock;
import com.craftdistrict.block.PoliceEntranceBlock;
import com.craftdistrict.block.PoliceWaitingBenchBlock;
import com.craftdistrict.block.PoliceWorkroomBlock;
import com.craftdistrict.block.PoliceDispatchBlock;
import com.craftdistrict.block.OfficeChairBlock;
import com.craftdistrict.block.PoliceInvestigationBoardBlock;
import com.craftdistrict.block.PoliceInterrogationBlock;
import com.craftdistrict.block.PoliceFingerprintBlock;
import com.craftdistrict.block.PoliceInterrogationChairBlock;
import com.craftdistrict.block.PoliceSecurityPropBlock;
import com.craftdistrict.block.PoliceSecurityCameraBlock;
import com.craftdistrict.block.PoliceOfficeSecurityBlock;
import com.craftdistrict.block.SecurityScannerBlock;
import com.craftdistrict.block.PoliceHoldingCellDoorBlock;
import com.craftdistrict.block.HoldingCellBedBlock;
import com.craftdistrict.block.PoliceDetentionStructureBlock;
import com.craftdistrict.block.PoliceDetentionPropBlock;
import com.craftdistrict.block.OneWayGlassBlock;
import com.craftdistrict.block.PoliceArmoryRackBlock;
import com.craftdistrict.block.PoliceArmoryPropBlock;
import com.craftdistrict.block.FireStationBlock;
import com.craftdistrict.block.FireExtinguisherBlock;
import com.craftdistrict.block.FireRescuePropBlock;
import com.craftdistrict.block.FireRescueRackBlock;
import com.craftdistrict.block.RescueStretcherBlock;
import com.craftdistrict.block.FireDepartmentSignBlock;
import com.craftdistrict.block.FireAlarmControlPanelBlock;
import com.craftdistrict.block.FireStationPoleBlock;
import com.craftdistrict.block.FireDispatchDeskBlock;
import com.craftdistrict.block.FireWallFixtureBlock;
import com.craftdistrict.block.FireIncidentBoardBlock;
import com.craftdistrict.block.FireLockerBenchBlock;
import com.craftdistrict.block.SupermarketDecorationBlock;
import com.craftdistrict.block.SupermarketGondolaBlock;
import com.craftdistrict.block.SupermarketColdBlock;
import com.craftdistrict.block.SupermarketDairyBlock;
import com.craftdistrict.block.SupermarketCheckoutBlock;
import com.craftdistrict.block.SupermarketShoppingPropBlock;
import com.craftdistrict.block.SupermarketShoppingCartBlock;
import com.craftdistrict.block.SupermarketSelfCheckoutBlock;
import com.craftdistrict.block.SupermarketStorePropBlock;
import com.craftdistrict.block.CeilingLightBlock;
import com.craftdistrict.block.SurfaceLightBlock;
import com.craftdistrict.block.ExpansionLightBlock;
import com.craftdistrict.block.MultiMountLightBlock;
import com.craftdistrict.block.TallLightBlock;
import com.craftdistrict.block.ConnectedLightBlock;
import com.craftdistrict.block.HangingStringLightsBlock;
import com.craftdistrict.block.UnderCabinetLightBlock;
import com.craftdistrict.block.WallLightBlock;
import com.craftdistrict.block.FluorescentLightBlock;
import com.craftdistrict.block.ExitSignBlock;
import com.craftdistrict.block.TrackLightBlock;
import com.craftdistrict.block.FloorLightBlock;
import com.craftdistrict.block.OpenSignBlock;
import com.craftdistrict.block.DiningTableBlock;
import com.craftdistrict.block.ModularDiningTableBlock;
import com.craftdistrict.block.RestaurantSeatBlock;
import com.craftdistrict.block.ConnectedRestaurantSeatBlock;
import com.craftdistrict.block.RestaurantKitchenBlock;
import com.craftdistrict.block.KitchenExhaustHoodBlock;
import com.craftdistrict.block.KitchenExhaustDuctBlock;
import com.craftdistrict.block.RestaurantServiceCounterBlock;
import com.craftdistrict.block.RestaurantServicePropBlock;
import com.craftdistrict.block.RestaurantServiceWallBlock;
import com.craftdistrict.block.RestaurantServingCartBlock;
import com.craftdistrict.block.RestaurantStorageCabinetBlock;
import com.craftdistrict.block.RestaurantStoragePropBlock;
import com.craftdistrict.block.RestaurantStorageWallBlock;
import com.craftdistrict.block.CommercialPizzaOvenBlock;
import com.craftdistrict.block.KitchenTrashBinBlock;
import com.craftdistrict.block.KitchenOrderDisplayBlock;
import com.craftdistrict.block.StainlessSteelPrepTableBlock;
import com.craftdistrict.block.AssemblyStationBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public final class CraftDistrictBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.BLOCK);

    private CraftDistrictBlocks() {
    }

    public static void register() {
        Bakery.initialize();
        Police.initialize();
        FireStation.initialize();
        Supermarket.initialize();
        Lighting.initialize();
        Restaurant.initialize();
        Survival.initialize();
        BLOCKS.register();
    }

    private static <T extends Block> RegistrySupplier<T> register(String id, Supplier<T> factory) {
        RegistrySupplier<T> block = BLOCKS.register(id, factory);
        CraftDistrictItems.registerBlockItem(id, () -> block.get() instanceof DoorBlock
                ? new DoubleHighBlockItem(block.get(), new Item.Properties())
                : block.get() instanceof HoldingCellBedBlock
                ? new BedItem(block.get(), new Item.Properties())
                : new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static final class Survival {
        public static final RegistrySupplier<AssemblyStationBlock> ASSEMBLY_STATION = register("assembly_station",
                () -> new AssemblyStationBlock(BlockBehaviour.Properties.of()
                        .strength(2.5F).sound(SoundType.WOOD).noOcclusion()));

        private Survival() { }

        private static void initialize() { }
    }

    public static final class Restaurant {
        public static final RegistrySupplier<DiningTableBlock> SQUARE_DINING_TABLE = register("square_dining_table",
                () -> new DiningTableBlock(DiningTableBlock.Design.SQUARE, woodProperties()));
        public static final RegistrySupplier<DiningTableBlock> ROUND_CAFE_TABLE = register("round_cafe_table",
                () -> new DiningTableBlock(DiningTableBlock.Design.CAFE, woodProperties()));
        public static final RegistrySupplier<DiningTableBlock> HIGH_BAR_TABLE = register("high_bar_table",
                () -> new DiningTableBlock(DiningTableBlock.Design.BAR, metalProperties()));
        public static final RegistrySupplier<ModularDiningTableBlock> WOODEN_MODULAR_TABLE = register("wooden_modular_table",
                () -> new ModularDiningTableBlock(woodProperties()));
        public static final RegistrySupplier<ModularDiningTableBlock> INDUSTRIAL_MODULAR_TABLE = register("industrial_modular_table",
                () -> new ModularDiningTableBlock(metalProperties()));
        public static final RegistrySupplier<ModularDiningTableBlock> DINER_MODULAR_TABLE = register("diner_modular_table",
                () -> new ModularDiningTableBlock(metalProperties()));
        public static final RegistrySupplier<ModularDiningTableBlock> TABLECLOTH_MODULAR_TABLE = register("tablecloth_modular_table",
                () -> new ModularDiningTableBlock(woodProperties()));
        public static final RegistrySupplier<RestaurantSeatBlock> WOODEN_DINING_CHAIR = register("wooden_dining_chair",
                () -> new RestaurantSeatBlock(RestaurantSeatBlock.Design.WOODEN, woodProperties()));
        public static final RegistrySupplier<RestaurantSeatBlock> MODERN_DINING_CHAIR = register("modern_dining_chair",
                () -> new RestaurantSeatBlock(RestaurantSeatBlock.Design.MODERN, metalProperties()));
        public static final RegistrySupplier<RestaurantSeatBlock> CUSHIONED_DINING_CHAIR = register("cushioned_dining_chair",
                () -> new RestaurantSeatBlock(RestaurantSeatBlock.Design.CUSHIONED, woodProperties()));
        public static final RegistrySupplier<RestaurantSeatBlock> DINER_CHAIR = register("diner_chair",
                () -> new RestaurantSeatBlock(RestaurantSeatBlock.Design.DINER, metalProperties()));
        public static final RegistrySupplier<RestaurantSeatBlock> INDUSTRIAL_BAR_STOOL = register("industrial_bar_stool",
                () -> new RestaurantSeatBlock(RestaurantSeatBlock.Design.BAR_STOOL, metalProperties()));
        public static final RegistrySupplier<RestaurantSeatBlock> CUSHIONED_BAR_CHAIR = register("cushioned_bar_chair",
                () -> new RestaurantSeatBlock(RestaurantSeatBlock.Design.BAR_CHAIR, woodProperties()));
        public static final RegistrySupplier<ConnectedRestaurantSeatBlock> DINER_BOOTH_SEAT = register("diner_booth_seat",
                () -> new ConnectedRestaurantSeatBlock(RestaurantSeatBlock.Design.DINER_BOOTH, metalProperties()));
        public static final RegistrySupplier<ConnectedRestaurantSeatBlock> MODERN_BOOTH_SEAT = register("modern_booth_seat",
                () -> new ConnectedRestaurantSeatBlock(RestaurantSeatBlock.Design.MODERN_BOOTH, woodProperties()));
        public static final RegistrySupplier<ConnectedRestaurantSeatBlock> WOODEN_DINING_BENCH = register("wooden_dining_bench",
                () -> new ConnectedRestaurantSeatBlock(RestaurantSeatBlock.Design.BENCH, woodProperties()));
        public static final RegistrySupplier<RestaurantKitchenBlock> COMMERCIAL_COOKING_RANGE = register("commercial_cooking_range",
                () -> new RestaurantKitchenBlock(RestaurantKitchenBlock.Equipment.RANGE, metalProperties()));
        public static final RegistrySupplier<KitchenExhaustHoodBlock> KITCHEN_EXHAUST_HOOD = register("kitchen_exhaust_hood",
                () -> new KitchenExhaustHoodBlock(metalProperties()));
        public static final RegistrySupplier<KitchenExhaustDuctBlock> KITCHEN_EXHAUST_DUCT = register("kitchen_exhaust_duct",
                () -> new KitchenExhaustDuctBlock(metalProperties()));
        public static final RegistrySupplier<RestaurantKitchenBlock> COMMERCIAL_DEEP_FRYER = register("commercial_deep_fryer",
                () -> new RestaurantKitchenBlock(RestaurantKitchenBlock.Equipment.FRYER, metalProperties()));
        public static final RegistrySupplier<RestaurantKitchenBlock> COMMERCIAL_GRIDDLE = register("commercial_griddle",
                () -> new RestaurantKitchenBlock(RestaurantKitchenBlock.Equipment.GRIDDLE, metalProperties()));
        public static final RegistrySupplier<RestaurantKitchenBlock> STAINLESS_STEEL_PREP_TABLE = register("stainless_steel_prep_table",
                () -> new StainlessSteelPrepTableBlock(metalProperties()));
        public static final RegistrySupplier<RestaurantKitchenBlock> KITCHEN_SINK_STATION = register("kitchen_sink_station",
                () -> new RestaurantKitchenBlock(RestaurantKitchenBlock.Equipment.SINK, metalProperties()));
        public static final RegistrySupplier<RestaurantServiceCounterBlock> RESTAURANT_SERVICE_COUNTER = register("restaurant_service_counter",
                () -> new RestaurantServiceCounterBlock(woodProperties()));
        public static final RegistrySupplier<RestaurantServicePropBlock> FOOD_PICKUP_COUNTER = register("food_pickup_counter",
                () -> new RestaurantServicePropBlock(RestaurantServicePropBlock.Design.PICKUP_COUNTER, metalProperties()));
        public static final RegistrySupplier<RestaurantServicePropBlock> RESTAURANT_POS_TERMINAL = register("restaurant_pos_terminal",
                () -> new RestaurantServicePropBlock(RestaurantServicePropBlock.Design.POS_TERMINAL, metalProperties()));
        public static final RegistrySupplier<RestaurantServingCartBlock> RESTAURANT_SERVING_CART = register("restaurant_serving_cart",
                () -> new RestaurantServingCartBlock(metalProperties()));
        public static final RegistrySupplier<RestaurantServiceWallBlock> PLATE_CUTLERY_RACK = register("plate_cutlery_rack",
                () -> new RestaurantServiceWallBlock(RestaurantServiceWallBlock.Design.PLATE_CUTLERY_RACK, metalProperties()));
        public static final RegistrySupplier<RestaurantServicePropBlock> BEVERAGE_DISPENSER = register("beverage_dispenser",
                () -> new RestaurantServicePropBlock(RestaurantServicePropBlock.Design.BEVERAGE_DISPENSER, metalProperties()));
        public static final RegistrySupplier<RestaurantServicePropBlock> CONDIMENT_STATION = register("condiment_station",
                () -> new RestaurantServicePropBlock(RestaurantServicePropBlock.Design.CONDIMENT_STATION, woodProperties()));
        public static final RegistrySupplier<RestaurantServicePropBlock> COMMERCIAL_COFFEE_MACHINE = register("commercial_coffee_machine",
                () -> new RestaurantServicePropBlock(RestaurantServicePropBlock.Design.COFFEE_MACHINE, metalProperties()));
        public static final RegistrySupplier<KitchenOrderDisplayBlock> KITCHEN_ORDER_DISPLAY = register("kitchen_order_display",
                () -> new KitchenOrderDisplayBlock(metalProperties()));
        public static final RegistrySupplier<RestaurantServicePropBlock> RESTAURANT_MENU_BOARD = register("restaurant_menu_board",
                () -> new RestaurantServicePropBlock(RestaurantServicePropBlock.Design.MENU_BOARD, woodProperties()));
        public static final RegistrySupplier<RestaurantStorageCabinetBlock> COMMERCIAL_REFRIGERATOR = register("commercial_refrigerator",
                () -> new RestaurantStorageCabinetBlock(RestaurantStorageCabinetBlock.Design.REFRIGERATOR, metalProperties()));
        public static final RegistrySupplier<RestaurantStorageCabinetBlock> INGREDIENT_STORAGE_SHELF = register("ingredient_storage_shelf",
                () -> new RestaurantStorageCabinetBlock(RestaurantStorageCabinetBlock.Design.PANTRY, metalProperties()));
        public static final RegistrySupplier<RestaurantStorageWallBlock> POT_PAN_RACK = register("pot_pan_rack",
                () -> new RestaurantStorageWallBlock(RestaurantStorageWallBlock.Design.COOKWARE, metalProperties()));
        public static final RegistrySupplier<RestaurantStorageWallBlock> KITCHEN_KNIFE_RACK = register("kitchen_knife_rack",
                () -> new RestaurantStorageWallBlock(RestaurantStorageWallBlock.Design.KNIVES, metalProperties()));
        public static final RegistrySupplier<RestaurantStoragePropBlock> COMMERCIAL_DISHWASHER = register("commercial_dishwasher",
                () -> new RestaurantStoragePropBlock(RestaurantStoragePropBlock.Design.DISHWASHER, metalProperties()));
        public static final RegistrySupplier<RestaurantStoragePropBlock> COMMERCIAL_MICROWAVE = register("commercial_microwave",
                () -> new RestaurantStoragePropBlock(RestaurantStoragePropBlock.Design.MICROWAVE, metalProperties()));
        public static final RegistrySupplier<CommercialPizzaOvenBlock> COMMERCIAL_PIZZA_OVEN = register("commercial_pizza_oven",
                () -> new CommercialPizzaOvenBlock(metalProperties()));
        public static final RegistrySupplier<RestaurantStoragePropBlock> CUTTING_BOARD_STATION = register("cutting_board_station",
                () -> new RestaurantStoragePropBlock(RestaurantStoragePropBlock.Design.CUTTING_BOARD, woodProperties()));
        public static final RegistrySupplier<RestaurantStorageWallBlock> SPICE_RACK = register("spice_rack",
                () -> new RestaurantStorageWallBlock(RestaurantStorageWallBlock.Design.SPICES, metalProperties()));
        public static final RegistrySupplier<KitchenTrashBinBlock> KITCHEN_TRASH_BIN = register("kitchen_trash_bin",
                () -> new KitchenTrashBinBlock(metalProperties()));
        public static final RegistrySupplier<RestaurantStoragePropBlock> DISH_DRYING_RACK = register("dish_drying_rack",
                () -> new RestaurantStoragePropBlock(RestaurantStoragePropBlock.Design.DRYING_RACK, metalProperties()));

        private static BlockBehaviour.Properties woodProperties() {
            return BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOD).noOcclusion();
        }

        private static BlockBehaviour.Properties metalProperties() {
            return BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL).noOcclusion();
        }

        private Restaurant() { }

        private static void initialize() { }
    }

    public static final class Bakery {
        public static final RegistrySupplier<WideDecorationBlock> DISPLAY_COUNTER = register("bakery_display_counter",
                () -> new WideDecorationBlock(BlockBehaviour.Properties.of()
                        .strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
        public static final RegistrySupplier<TallDecorationBlock> BREAD_SHELF = register("bread_shelf",
                () -> new TallDecorationBlock(BlockBehaviour.Properties.of()
                        .strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
        public static final RegistrySupplier<WideTallDecorationBlock> INDUSTRIAL_OVEN = register("industrial_bakery_oven",
                () -> new WideTallDecorationBlock(BlockBehaviour.Properties.of()
                        .strength(3.0F).sound(SoundType.METAL).noOcclusion()));
        public static final RegistrySupplier<CashRegisterBlock> CASH_REGISTER = register("cash_register",
                () -> new CashRegisterBlock(BlockBehaviour.Properties.of()
                        .strength(1.0F).sound(SoundType.METAL).noOcclusion()));
        public static final RegistrySupplier<DoughPreparationTableBlock> DOUGH_PREPARATION_TABLE = register("dough_preparation_table",
                () -> new DoughPreparationTableBlock(BlockBehaviour.Properties.of()
                        .strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
        public static final RegistrySupplier<FlourSacksBlock> FLOUR_SACKS_SMALL = register("flour_sacks_small",
                () -> new FlourSacksBlock(FlourSacksBlock.Arrangement.SMALL, flourSackProperties()));
        public static final RegistrySupplier<FlourSacksBlock> FLOUR_SACKS_STACKED = register("flour_sacks_stacked",
                () -> new FlourSacksBlock(FlourSacksBlock.Arrangement.STACKED, flourSackProperties()));
        public static final RegistrySupplier<FlourSacksBlock> FLOUR_SACKS_PILE = register("flour_sacks_pile",
                () -> new FlourSacksBlock(FlourSacksBlock.Arrangement.PILE, flourSackProperties()));
        public static final RegistrySupplier<PastryTrayBlock> CROISSANT_TRAY = register("croissant_tray",
                () -> new PastryTrayBlock(PastryTrayBlock.Variant.CROISSANT, pastryTrayProperties()));
        public static final RegistrySupplier<PastryTrayBlock> COOKIE_TRAY = register("cookie_tray",
                () -> new PastryTrayBlock(PastryTrayBlock.Variant.COOKIE, pastryTrayProperties()));
        public static final RegistrySupplier<PastryTrayBlock> BREAD_ROLLS_TRAY = register("bread_rolls_tray",
                () -> new PastryTrayBlock(PastryTrayBlock.Variant.BREAD_ROLLS, pastryTrayProperties()));
        public static final RegistrySupplier<PastryTrayBlock> EMPTY_BAKING_TRAY = register("empty_baking_tray",
                () -> new PastryTrayBlock(PastryTrayBlock.Variant.EMPTY, pastryTrayProperties()));
        public static final RegistrySupplier<IndustrialDoughMixerBlock> INDUSTRIAL_DOUGH_MIXER = register("industrial_dough_mixer",
                () -> new IndustrialDoughMixerBlock(BlockBehaviour.Properties.of()
                        .strength(3.0F).sound(SoundType.METAL).noOcclusion()));
        public static final RegistrySupplier<DoughSheeterBlock> DOUGH_SHEETER = register("dough_sheeter",
                () -> new DoughSheeterBlock(BlockBehaviour.Properties.of()
                        .strength(2.5F).sound(SoundType.METAL).noOcclusion()));
        public static final RegistrySupplier<BakerySignageBlock> HANGING_SHOP_SIGN = register("bakery_hanging_shop_sign",
                () -> new BakerySignageBlock(BakerySignageBlock.Variant.HANGING_SHOP, signageProperties()));
        public static final RegistrySupplier<BakerySignageBlock> WALL_SHOP_SIGN = register("bakery_wall_shop_sign",
                () -> new BakerySignageBlock(BakerySignageBlock.Variant.WALL_SHOP, signageProperties()));
        public static final RegistrySupplier<BakerySignageBlock> WALL_MENU_BOARD = register("bakery_menu_board_wall",
                () -> new BakerySignageBlock(BakerySignageBlock.Variant.WALL_MENU, signageProperties()));
        public static final RegistrySupplier<BakerySignageBlock> DAILY_SPECIAL_BOARD = register("bakery_daily_special_board",
                () -> new BakerySignageBlock(BakerySignageBlock.Variant.DAILY_SPECIAL, signageProperties()));
        public static final RegistrySupplier<BakerySignageBlock> SIDEWALK_MENU_BOARD = register("bakery_sidewalk_menu_board",
                () -> new BakerySignageBlock(BakerySignageBlock.Variant.SIDEWALK_MENU, signageProperties()));
        public static final RegistrySupplier<BakeryTrayRackBlock> TRAY_RACK_EMPTY = register("bakery_tray_rack_empty",
                () -> new BakeryTrayRackBlock(BakeryTrayRackBlock.Variant.EMPTY, trayRackProperties()));
        public static final RegistrySupplier<BakeryTrayRackBlock> TRAY_RACK_BAKED = register("bakery_tray_rack_baked",
                () -> new BakeryTrayRackBlock(BakeryTrayRackBlock.Variant.BAKED, trayRackProperties()));
        public static final RegistrySupplier<RefrigeratedDisplayBlock> REFRIGERATED_CAKE_DISPLAY = register("refrigerated_cake_display",
                () -> new RefrigeratedDisplayBlock(RefrigeratedDisplayBlock.Variant.CAKE, refrigeratedDisplayProperties()));
        public static final RegistrySupplier<RefrigeratedDisplayBlock> REFRIGERATED_PASTRY_DISPLAY = register("refrigerated_pastry_display",
                () -> new RefrigeratedDisplayBlock(RefrigeratedDisplayBlock.Variant.PASTRY, refrigeratedDisplayProperties()));
        public static final RegistrySupplier<BakeryWallToolBlock> BREAD_PEEL_RACK = register("bakery_bread_peel_rack",
                () -> new BakeryWallToolBlock(BakeryWallToolBlock.Tool.BREAD_PEEL, wallToolProperties()));
        public static final RegistrySupplier<BakeryWallToolBlock> UTENSIL_WALL_RACK = register("bakery_utensil_wall_rack",
                () -> new BakeryWallToolBlock(BakeryWallToolBlock.Tool.UTENSIL, wallToolProperties()));
        public static final RegistrySupplier<BakeryWallToolBlock> ROLLING_PIN_RACK = register("bakery_rolling_pin_rack",
                () -> new BakeryWallToolBlock(BakeryWallToolBlock.Tool.ROLLING_PIN, wallToolProperties()));
        public static final RegistrySupplier<BreadSlicerBlock> BREAD_SLICER = register("bakery_bread_slicer",
                () -> new BreadSlicerBlock(BlockBehaviour.Properties.of()
                        .strength(2.5F).sound(SoundType.METAL).noOcclusion()));

        private static BlockBehaviour.Properties wallToolProperties() {
            return BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.WOOD).noOcclusion();
        }

        private static BlockBehaviour.Properties refrigeratedDisplayProperties() {
            return BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion();
        }

        private static BlockBehaviour.Properties trayRackProperties() {
            return BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.METAL).noOcclusion();
        }

        private static BlockBehaviour.Properties signageProperties() {
            return BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.WOOD).noOcclusion();
        }

        private static BlockBehaviour.Properties pastryTrayProperties() {
            return BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.METAL).noOcclusion();
        }

        private static BlockBehaviour.Properties flourSackProperties() {
            return BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.WOOL).noOcclusion();
        }

        private Bakery() {
        }

        private static void initialize() {
            // Initialize this collection before submitting the shared registries.
        }
    }

    public static final class Police {
        public static final RegistrySupplier<PoliceEntranceBlock> RECEPTION_DESK = register("police_reception_desk",
                () -> new PoliceEntranceBlock(PoliceEntranceBlock.Variant.RECEPTION_DESK, entranceProperties()));
        public static final RegistrySupplier<PoliceEntranceBlock> DEPARTMENT_SIGN = register("police_department_sign",
                () -> new PoliceEntranceBlock(PoliceEntranceBlock.Variant.DEPARTMENT_SIGN, entranceProperties()));
        public static final RegistrySupplier<PoliceWaitingBenchBlock> WAITING_BENCH = register("waiting_area_bench",
                () -> new PoliceWaitingBenchBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceDispatchBlock> DISPATCH_CONSOLE = register("police_dispatch_console",
                () -> new PoliceDispatchBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceWorkroomBlock> EVIDENCE_STORAGE_SHELF = register("police_evidence_storage_shelf",
                () -> new PoliceWorkroomBlock(PoliceWorkroomBlock.Variant.EVIDENCE, entranceProperties()));
        public static final RegistrySupplier<OfficeChairBlock> OFFICE_CHAIR = register("office_chair",
                () -> new OfficeChairBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceOfficeSecurityBlock> CASE_FILE_CABINET = register("case_file_cabinet",
                () -> new PoliceOfficeSecurityBlock(PoliceOfficeSecurityBlock.Design.FILE_CABINET, entranceProperties()));
        public static final RegistrySupplier<PoliceInvestigationBoardBlock> INVESTIGATION_BOARD = register("police_investigation_board",
                () -> new PoliceInvestigationBoardBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceInterrogationBlock> INTERROGATION_TABLE = register("police_interrogation_table",
                () -> new PoliceInterrogationBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceFingerprintBlock> FINGERPRINT_STATION = register("police_fingerprint_station",
                () -> new PoliceFingerprintBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceInterrogationChairBlock> BLUE_CHAIR = register("blue_chair",
                () -> new PoliceInterrogationChairBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceSecurityPropBlock> HOLDING_CELL_BARS = register("holding_cell_bars",
                () -> new PoliceSecurityPropBlock(PoliceSecurityPropBlock.Variant.BARS, entranceProperties()));
        public static final RegistrySupplier<PoliceHoldingCellDoorBlock> HOLDING_CELL_DOOR = register("holding_cell_door",
                () -> new PoliceHoldingCellDoorBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceSecurityCameraBlock> SECURITY_CAMERA = register("security_camera",
                () -> new PoliceSecurityCameraBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceOfficeSecurityBlock> SECURITY_MONITOR_WALL_UNIT = register("security_monitor_wall_unit",
                () -> new PoliceOfficeSecurityBlock(PoliceOfficeSecurityBlock.Design.MONITOR_WALL, entranceProperties()));
        public static final RegistrySupplier<SecurityScannerBlock> VISITOR_SECURITY_SCANNER = register("visitor_security_scanner",
                () -> new SecurityScannerBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceSecurityPropBlock> LOCKERS = register("lockers",
                () -> new PoliceSecurityPropBlock(PoliceSecurityPropBlock.Variant.LOCKERS,
                        BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL)));
        public static final RegistrySupplier<HoldingCellBedBlock> HOLDING_CELL_BED = register("holding_cell_bed",
                () -> new HoldingCellBedBlock(entranceProperties()));
        public static final RegistrySupplier<PoliceDetentionPropBlock> HOLDING_CELL_TOILET = register("holding_cell_toilet",
                () -> new PoliceDetentionPropBlock(PoliceDetentionPropBlock.Prop.TOILET_SINK, entranceProperties()));
        public static final RegistrySupplier<PoliceDetentionPropBlock> ONE_WAY_OBSERVATION_GLASS = register("one_way_observation_glass",
                () -> new PoliceDetentionPropBlock(PoliceDetentionPropBlock.Prop.GLASS,
                        BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.GLASS).noOcclusion()));
        public static final RegistrySupplier<OneWayGlassBlock> ONE_WAY_GLASS_BLOCK = register("one_way_glass_block",
                () -> new OneWayGlassBlock(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.GLASS)
                        .noOcclusion().isViewBlocking((state, level, pos) -> false)
                        .isSuffocating((state, level, pos) -> false)
                        .isRedstoneConductor((state, level, pos) -> false)
                        .isValidSpawn((state, level, pos, type) -> false)));
        public static final RegistrySupplier<PoliceDetentionStructureBlock> MUGSHOT_HEIGHT_CHART = register("police_mugshot_height_chart",
                () -> new PoliceDetentionStructureBlock(PoliceDetentionStructureBlock.Structure.CHART,
                        BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL).noOcclusion()));
        public static final RegistrySupplier<PoliceDetentionStructureBlock> BOOKING_CAMERA = register("booking_camera",
                () -> new PoliceDetentionStructureBlock(PoliceDetentionStructureBlock.Structure.CAMERA, entranceProperties()));
        public static final RegistrySupplier<PoliceArmoryPropBlock> RADIO_CHARGING_STATION = register("radio_charging_station",
                () -> new PoliceArmoryPropBlock(PoliceArmoryPropBlock.Prop.RADIO, entranceProperties()));
        public static final RegistrySupplier<PoliceArmoryRackBlock> EQUIPMENT_RACK = register("police_equipment_rack",
                () -> new PoliceArmoryRackBlock(PoliceArmoryRackBlock.Rack.EQUIPMENT, entranceProperties()));
        public static final RegistrySupplier<PoliceArmoryPropBlock> PISTOL_RACK = register("pistol_rack",
                () -> new PoliceArmoryPropBlock(PoliceArmoryPropBlock.Prop.PISTOL, entranceProperties()));
        public static final RegistrySupplier<PoliceArmoryRackBlock> RIFLE_RACK = register("rifle_rack",
                () -> new PoliceArmoryRackBlock(PoliceArmoryRackBlock.Rack.RIFLE, entranceProperties()));

        private static BlockBehaviour.Properties entranceProperties() {
            return BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL).noOcclusion();
        }

        private Police() {
        }

        private static void initialize() {
        }
    }

    public static final class Supermarket {
        public static final RegistrySupplier<SupermarketDecorationBlock> GROCERY_SHELF = register("grocery_shelf",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.AISLE, shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> CANNED_SHELF = register("canned_shelf",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.AISLE, shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> SNACK_SHELF = register("snack_shelf",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.AISLE, shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> CANDY_SHELF = register("candy_shelf",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.AISLE, shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> CLEANING_SUPPLIES_SHELF = register("supermarket_cleaning_supplies_shelf",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.AISLE, shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> PERSONAL_CARE_SHELF = register("supermarket_personal_care_shelf",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.AISLE, shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> PET_SUPPLIES_SHELF = register("supermarket_pet_supplies_shelf",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.AISLE, shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> PAPER_PRODUCTS_SHELF = register("supermarket_paper_products_shelf",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.AISLE, shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> HOUSEHOLD_SUPPLIES_SHELF = register("supermarket_household_supplies_shelf",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.AISLE, shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> STATIONERY_SHELF = register("supermarket_stationery_shelf",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.AISLE, shelfProperties()));
        public static final RegistrySupplier<SupermarketGondolaBlock> DOUBLE_SIDED_GONDOLA = register("supermarket_double_sided_gondola",
                () -> new SupermarketGondolaBlock(shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> HANGING_PRODUCTS_DISPLAY = register("supermarket_hanging_products_display",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.HANGING, shelfProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> FRUIT_STAND = register("fruit_stand",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.PRODUCE, produceProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> VEGETABLE_STAND = register("vegetable_stand",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.PRODUCE, produceProperties()));
        public static final RegistrySupplier<SupermarketDecorationBlock> PROMOTIONAL_DISPLAY = register("supermarket_promotional_display",
                () -> new SupermarketDecorationBlock(SupermarketDecorationBlock.Layout.PROMOTION, shelfProperties()));
        public static final RegistrySupplier<SupermarketColdBlock> DRINKS_FRIDGE_A = register("drinks_fridge_a",
                () -> new SupermarketColdBlock(SupermarketColdBlock.Layout.DRINKS, shelfProperties()));
        public static final RegistrySupplier<SupermarketColdBlock> DRINKS_FRIDGE_B = register("drinks_fridge_b",
                () -> new SupermarketColdBlock(SupermarketColdBlock.Layout.DRINKS, shelfProperties()));
        public static final RegistrySupplier<SupermarketColdBlock> COLA_FRIDGE = register("supermarket_cola_fridge",
                () -> new SupermarketColdBlock(SupermarketColdBlock.Layout.DRINKS, shelfProperties()));
        public static final RegistrySupplier<SupermarketColdBlock> BEER_FRIDGE = register("supermarket_beer_fridge",
                () -> new SupermarketColdBlock(SupermarketColdBlock.Layout.DRINKS, shelfProperties()));
        public static final RegistrySupplier<SupermarketColdBlock> WINE_FRIDGE = register("supermarket_wine_fridge",
                () -> new SupermarketColdBlock(SupermarketColdBlock.Layout.DRINKS, shelfProperties()));
        public static final RegistrySupplier<SupermarketColdBlock> WHISKEY_DISPLAY_CABINET = register("supermarket_whiskey_display_cabinet",
                () -> new SupermarketColdBlock(SupermarketColdBlock.Layout.DRINKS, shelfProperties()));
        public static final RegistrySupplier<SupermarketDairyBlock> DAIRY_REFRIGERATOR = register("dairy_refrigerator",
                () -> new SupermarketDairyBlock(shelfProperties()));
        public static final RegistrySupplier<SupermarketColdBlock> CHEST_FREEZER_FROZEN_FOOD = register("chest_freezer_frozen_food",
                () -> new SupermarketColdBlock(SupermarketColdBlock.Layout.CHEST, shelfProperties()));
        public static final RegistrySupplier<SupermarketColdBlock> CHEST_FREEZER_ICE_CREAM = register("chest_freezer_ice_cream",
                () -> new SupermarketColdBlock(SupermarketColdBlock.Layout.CHEST, shelfProperties()));
        public static final RegistrySupplier<SupermarketColdBlock> DELI_DISPLAY_COUNTER = register("deli_display_counter",
                () -> new SupermarketColdBlock(SupermarketColdBlock.Layout.DELI, shelfProperties()));
        public static final RegistrySupplier<SupermarketCheckoutBlock> CHECKOUT = register("supermarket_checkout",
                () -> new SupermarketCheckoutBlock(shelfProperties()));
        public static final RegistrySupplier<SupermarketShoppingCartBlock> SHOPPING_CART = register("supermarket_shopping_cart",
                () -> new SupermarketShoppingCartBlock(shelfProperties()));
        public static final RegistrySupplier<SupermarketShoppingCartBlock> FILLED_SHOPPING_CART = register("supermarket_filled_shopping_cart",
                () -> new SupermarketShoppingCartBlock(shelfProperties()));
        public static final RegistrySupplier<SupermarketShoppingPropBlock> BASKET_STACK = register("supermarket_basket_stack",
                () -> new SupermarketShoppingPropBlock(SupermarketShoppingPropBlock.Prop.BASKETS,
                        BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.WOOD).noOcclusion()));
        public static final RegistrySupplier<SupermarketSelfCheckoutBlock> SELF_CHECKOUT = register("supermarket_self_checkout",
                () -> new SupermarketSelfCheckoutBlock(shelfProperties()));
        public static final RegistrySupplier<SupermarketStorePropBlock> STOCK_PALLET = register("supermarket_stock_pallet",
                () -> new SupermarketStorePropBlock(SupermarketStorePropBlock.Prop.STOCK_PALLET, produceProperties()));
        public static final RegistrySupplier<SupermarketStorePropBlock> SECURITY_GATE = register("supermarket_security_gate",
                () -> new SupermarketStorePropBlock(SupermarketStorePropBlock.Prop.SECURITY_GATE, shelfProperties()));
        public static final RegistrySupplier<SupermarketStorePropBlock> CHECKOUT_CANDY_RACK = register("supermarket_checkout_candy_rack",
                () -> new SupermarketStorePropBlock(SupermarketStorePropBlock.Prop.CANDY_RACK, shelfProperties()));

        private static BlockBehaviour.Properties shelfProperties() {
            return BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL).noOcclusion();
        }

        private static BlockBehaviour.Properties produceProperties() {
            return BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOD).noOcclusion();
        }

        private Supermarket() { }

        private static void initialize() { }
    }

    public static final class Lighting {
        public static final RegistrySupplier<CeilingLightBlock> LED_CEILING_PANEL = register("lighting_led_ceiling_panel",
                () -> new SurfaceLightBlock(CeilingLightBlock.Fixture.PANEL, lightProperties(15)));
        public static final RegistrySupplier<FluorescentLightBlock> FLUORESCENT_TUBE = register("lighting_fluorescent_tube",
                () -> new FluorescentLightBlock(lightProperties(15)));
        public static final RegistrySupplier<CeilingLightBlock> INDUSTRIAL_PENDANT = register("lighting_industrial_pendant",
                () -> new CeilingLightBlock(CeilingLightBlock.Fixture.PENDANT, lightProperties(13)));
        public static final RegistrySupplier<WallLightBlock> MODERN_WALL_LIGHT = register("lighting_modern_wall_light",
                () -> new WallLightBlock(WallLightBlock.Fixture.SCONCE, lightProperties(12)));
        public static final RegistrySupplier<WallLightBlock> INDUSTRIAL_FLOODLIGHT = register("lighting_industrial_floodlight",
                () -> new WallLightBlock(WallLightBlock.Fixture.FLOODLIGHT, lightProperties(15)));
        public static final RegistrySupplier<ExitSignBlock> EXIT_SIGN = register("lighting_exit_sign",
                () -> new ExitSignBlock(ExitSignBlock.Arrow.RIGHT, lightProperties(9)));
        public static final RegistrySupplier<ExitSignBlock> EXIT_SIGN_LEFT = register("lighting_exit_sign_left",
                () -> new ExitSignBlock(ExitSignBlock.Arrow.LEFT, lightProperties(9)));
        public static final RegistrySupplier<CeilingLightBlock> RECESSED_SPOTLIGHT = register("lighting_recessed_spotlight",
                () -> new CeilingLightBlock(CeilingLightBlock.Fixture.SPOTLIGHT, lightProperties(14)));
        public static final RegistrySupplier<TrackLightBlock> TRACK_LIGHT = register("lighting_track_light",
                () -> new TrackLightBlock(lightProperties(15)));
        public static final RegistrySupplier<FloorLightBlock> DESK_LAMP = register("lighting_desk_lamp",
                () -> new FloorLightBlock(FloorLightBlock.Fixture.DESK, lightProperties(12)));
        public static final RegistrySupplier<OpenSignBlock> NEON_OPEN_SIGN = register("lighting_neon_open_sign",
                () -> new OpenSignBlock(lightProperties(12)));
        public static final RegistrySupplier<FloorLightBlock> OUTDOOR_BOLLARD = register("lighting_outdoor_bollard",
                () -> new FloorLightBlock(FloorLightBlock.Fixture.BOLLARD, lightProperties(14)));
        public static final RegistrySupplier<WallLightBlock> SECURITY_WALL_LIGHT = register("lighting_security_wall_light",
                () -> new WallLightBlock(WallLightBlock.Fixture.SECURITY, lightProperties(15)));
        public static final RegistrySupplier<CeilingLightBlock> UNDER_CABINET_STRIP = register("lighting_under_cabinet_strip",
                () -> new UnderCabinetLightBlock(lightProperties(13)));
        public static final RegistrySupplier<TallLightBlock> MODERN_FLOOR_LAMP = register("lighting_modern_floor_lamp",
                () -> new TallLightBlock(TallLightBlock.Fixture.FLOOR, lightProperties(13)));
        public static final RegistrySupplier<TallLightBlock> MODERN_CHANDELIER = register("lighting_modern_chandelier",
                () -> new TallLightBlock(TallLightBlock.Fixture.CHANDELIER, lightProperties(15)));
        public static final RegistrySupplier<ExpansionLightBlock> EMERGENCY_BEACON = register("lighting_emergency_beacon",
                () -> new MultiMountLightBlock(ExpansionLightBlock.Fixture.BEACON, lightProperties(11)));
        public static final RegistrySupplier<ExpansionLightBlock> CEILING_FAN_WITH_LIGHT = register("lighting_ceiling_fan_with_light",
                () -> new ExpansionLightBlock(ExpansionLightBlock.Fixture.FAN, lightProperties(12)));
        public static final RegistrySupplier<TallLightBlock> VINTAGE_STREET_LAMP = register("lighting_vintage_street_lamp",
                () -> new TallLightBlock(TallLightBlock.Fixture.STREET, lightProperties(14)));
        public static final RegistrySupplier<ExpansionLightBlock> DECORATIVE_WALL_SCONCE = register("lighting_decorative_wall_sconce",
                () -> new ExpansionLightBlock(ExpansionLightBlock.Fixture.SCONCE, lightProperties(11)));
        public static final RegistrySupplier<ConnectedLightBlock> NEON_LIGHT_STRIP = register("lighting_neon_light_strip",
                () -> new ConnectedLightBlock(ConnectedLightBlock.Kind.NEON, lightProperties(11)));
        public static final RegistrySupplier<ConnectedLightBlock> HANGING_STRING_LIGHTS = register("lighting_hanging_string_lights",
                () -> new HangingStringLightsBlock(lightProperties(8)));
        public static final RegistrySupplier<ExpansionLightBlock> GARDEN_PATH_LIGHT = register("lighting_garden_path_light",
                () -> new ExpansionLightBlock(ExpansionLightBlock.Fixture.GARDEN, lightProperties(9)));
        public static final RegistrySupplier<ExpansionLightBlock> INDUSTRIAL_WARNING_LIGHT = register("lighting_industrial_warning_light",
                () -> new MultiMountLightBlock(ExpansionLightBlock.Fixture.WARNING, lightProperties(11)));

        private static BlockBehaviour.Properties lightProperties(int level) {
            return BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.METAL)
                    .noOcclusion().lightLevel(state -> level);
        }

        private Lighting() { }

        private static void initialize() { }
    }

    public static final class FireStation {
        public static final RegistrySupplier<FireStationBlock> GEAR_LOCKER = register("firefighter_gear_locker",
                () -> new FireStationBlock(FireStationBlock.Variant.GEAR_LOCKER, equipmentProperties()));
        public static final RegistrySupplier<FireStationBlock> BREATHING_APPARATUS_RACK = register("breathing_apparatus_rack",
                () -> new FireStationBlock(FireStationBlock.Variant.BREATHING_RACK, equipmentProperties()));
        public static final RegistrySupplier<FireExtinguisherBlock> EXTINGUISHER = register("fire_extinguisher",
                () -> new FireExtinguisherBlock(equipmentProperties()));
        public static final RegistrySupplier<FireRescuePropBlock> HOSE_REEL = register("fire_hose_reel",
                () -> new FireRescuePropBlock(FireRescuePropBlock.Prop.REEL, equipmentProperties()));
        public static final RegistrySupplier<FireRescueRackBlock> HOSE_STORAGE_RACK = register("fire_hose_storage_rack",
                () -> new FireRescueRackBlock(FireRescueRackBlock.Rack.HOSE, equipmentProperties()));
        public static final RegistrySupplier<FireRescueRackBlock> RESCUE_TOOLS_RACK = register("fire_rescue_tools_rack",
                () -> new FireRescueRackBlock(FireRescueRackBlock.Rack.TOOLS, equipmentProperties()));
        public static final RegistrySupplier<FireRescuePropBlock> HYDRAULIC_RESCUE_TOOL = register("hydraulic_rescue_tool",
                () -> new FireRescuePropBlock(FireRescuePropBlock.Prop.HYDRAULIC, equipmentProperties()));
        public static final RegistrySupplier<RescueStretcherBlock> RESCUE_STRETCHER = register("rescue_stretcher",
                () -> new RescueStretcherBlock(equipmentProperties()));
        public static final RegistrySupplier<FireDepartmentSignBlock> DEPARTMENT_SIGN = register("fire_department_sign",
                () -> new FireDepartmentSignBlock(equipmentProperties()));
        public static final RegistrySupplier<FireAlarmControlPanelBlock> ALARM_CONTROL_PANEL = register("fire_alarm_control_panel",
                () -> new FireAlarmControlPanelBlock(equipmentProperties()));
        public static final RegistrySupplier<FireStationPoleBlock> STATION_POLE = register("fire_station_pole",
                () -> new FireStationPoleBlock(equipmentProperties()));
        public static final RegistrySupplier<FireDispatchDeskBlock> DISPATCH_DESK = register("fire_dispatch_desk",
                () -> new FireDispatchDeskBlock(equipmentProperties()));
        public static final RegistrySupplier<FireWallFixtureBlock> WALL_CLOCK = register("wall_clock",
                () -> new FireWallFixtureBlock(FireWallFixtureBlock.Fixture.CLOCK,equipmentProperties()));
        public static final RegistrySupplier<FireIncidentBoardBlock> INCIDENT_BOARD = register("fire_station_incident_board",
                () -> new FireIncidentBoardBlock(equipmentProperties()));
        public static final RegistrySupplier<FireLockerBenchBlock> LOCKER_BENCH = register("locker_bench",
                () -> new FireLockerBenchBlock(equipmentProperties()));
        public static final RegistrySupplier<FireWallFixtureBlock> FIRST_AID_CABINET = register("first_aid_cabinet",
                () -> new FireWallFixtureBlock(FireWallFixtureBlock.Fixture.FIRST_AID,equipmentProperties()));

        private static BlockBehaviour.Properties equipmentProperties() {
            return BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL).noOcclusion();
        }

        private FireStation() {
        }

        private static void initialize() {
        }
    }
}
