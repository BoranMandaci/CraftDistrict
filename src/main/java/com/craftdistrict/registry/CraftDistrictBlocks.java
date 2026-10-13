package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.block.WideDecorationBlock;
import com.craftdistrict.block.TallDecorationBlock;
import com.craftdistrict.block.WideTallDecorationBlock;
import com.craftdistrict.block.CashRegisterBlock;
import com.craftdistrict.block.DoughPreparationTableBlock;
import com.craftdistrict.block.FlourSacksBlock;
import com.craftdistrict.block.PastryTrayBlock;
import com.craftdistrict.block.IndustrialDoughMixerBlock;
import com.craftdistrict.block.BakerySignageBlock;
import com.craftdistrict.block.BakeryTrayRackBlock;
import com.craftdistrict.block.RefrigeratedDisplayBlock;
import com.craftdistrict.block.BakeryWallToolBlock;
import com.craftdistrict.block.BreadSlicerBlock;
import com.craftdistrict.block.PoliceEntranceBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public final class CraftDistrictBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.ITEM);

    private CraftDistrictBlocks() {
    }

    public static void register() {
        Bakery.initialize();
        Police.initialize();
        BLOCKS.register();
        ITEMS.register();
    }

    private static <T extends Block> RegistrySupplier<T> register(String id, Supplier<T> factory) {
        RegistrySupplier<T> block = BLOCKS.register(id, factory);
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static final class Bakery {
        public static final RegistrySupplier<WideDecorationBlock> DISPLAY_COUNTER = register("bakery_display_counter",
                () -> new WideDecorationBlock(BlockBehaviour.Properties.of()
                        .strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
        public static final RegistrySupplier<TallDecorationBlock> BREAD_SHELF = register("bakery_bread_shelf",
                () -> new TallDecorationBlock(BlockBehaviour.Properties.of()
                        .strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
        public static final RegistrySupplier<WideTallDecorationBlock> INDUSTRIAL_OVEN = register("industrial_bakery_oven",
                () -> new WideTallDecorationBlock(BlockBehaviour.Properties.of()
                        .strength(3.0F).sound(SoundType.METAL).noOcclusion()));
        public static final RegistrySupplier<CashRegisterBlock> CASH_REGISTER = register("bakery_cash_register",
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
        public static final RegistrySupplier<PoliceEntranceBlock> WAITING_BENCH = register("police_waiting_area_bench",
                () -> new PoliceEntranceBlock(PoliceEntranceBlock.Variant.WAITING_BENCH, entranceProperties()));

        private static BlockBehaviour.Properties entranceProperties() {
            return BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL).noOcclusion();
        }

        private Police() {
        }

        private static void initialize() {
        }
    }
}
