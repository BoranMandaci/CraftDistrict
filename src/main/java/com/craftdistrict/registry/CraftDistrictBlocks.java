package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.block.MultiBlockDecorationBlock;
import com.craftdistrict.block.RotatableDecorationBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public final class CraftDistrictBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(CraftDistrict.MOD_ID, Registries.ITEM);

    private CraftDistrictBlocks() {
    }

    public static void register() {
        Workshop.initialize();
        BLOCKS.register();
        ITEMS.register();
    }

    private static <T extends Block> RegistrySupplier<T> register(String id, Supplier<T> factory) {
        RegistrySupplier<T> block = BLOCKS.register(id, factory);
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    /** Test entries grouped by theme; future collections can use the same shared registries. */
    public static final class Workshop {
        public static final RegistrySupplier<Block> TEST_STATIC = register("test_static",
                () -> new Block(BlockBehaviour.Properties.of().strength(1.5F)));
        public static final RegistrySupplier<RotatableDecorationBlock> TEST_ROTATABLE = register("test_rotatable",
                () -> new RotatableDecorationBlock(BlockBehaviour.Properties.of().strength(1.5F)));
        public static final RegistrySupplier<MultiBlockDecorationBlock> TEST_COUNTER = register("test_counter",
                () -> new MultiBlockDecorationBlock(BlockBehaviour.Properties.of().strength(1.5F)));

        private Workshop() {
        }

        private static void initialize() {
            // Accessing this method initializes the themed collection's registry entries.
        }
    }
}
