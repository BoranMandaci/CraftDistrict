package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.block.WideDecorationBlock;
import com.craftdistrict.block.TallDecorationBlock;
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

        private Bakery() {
        }

        private static void initialize() {
            // Initialize this collection before submitting the shared registries.
        }
    }

}
