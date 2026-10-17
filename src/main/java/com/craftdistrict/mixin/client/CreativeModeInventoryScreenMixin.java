package com.craftdistrict.mixin.client;

import com.craftdistrict.client.CollectionNavigation;
import com.craftdistrict.registry.CraftDistrictCreativeTabs;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Modify only the live CraftDistrict picker, leaving tab contents and search intact. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private float scrollOffs;
    @Shadow private boolean scrolling;
    @Unique private CollectionNavigation craftdistrict$navigation;

    protected CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu,
                                               Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void craftdistrict$resetNavigation(CallbackInfo ci) {
        if (craftdistrict$navigation != null) {
            craftdistrict$navigation.hide();
        }
        craftdistrict$navigation = null;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void craftdistrict$addNavigation(CallbackInfo ci) {
        craftdistrict$navigation = new CollectionNavigation(this::craftdistrict$addWidget,
                this::craftdistrict$filterItems);
        craftdistrict$updateNavigation();
    }

    @Unique
    private void craftdistrict$addWidget(AbstractWidget widget) {
        // Avoid javac's static generic-method bridge retaining the mixin receiver type.
        addRenderableWidget(widget);
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void craftdistrict$tabSelected(CallbackInfo ci) {
        craftdistrict$updateNavigation();
    }

    @Inject(method = "refreshCurrentTabContents", at = @At("TAIL"))
    private void craftdistrict$tabContentsRefreshed(CallbackInfo ci) {
        craftdistrict$filterItems();
    }

    @Unique
    private boolean craftdistrict$isSelected() {
        return selectedTab == CraftDistrictCreativeTabs.DECORATIONS.get();
    }

    @Unique
    private void craftdistrict$updateNavigation() {
        if (craftdistrict$navigation != null) {
            if (craftdistrict$isSelected()) {
                craftdistrict$navigation.show(leftPos, topPos, imageWidth, imageHeight,
                        menu.slots.get(0).y, font, selectedTab.getDisplayName());
            } else {
                craftdistrict$navigation.hide();
            }
        }
        craftdistrict$filterItems();
    }

    @Unique
    private void craftdistrict$filterItems() {
        // selectTab/refresh can run inside the vanilla constructor or init.
        if (craftdistrict$navigation == null || !craftdistrict$isSelected()) {
            return;
        }
        menu.items.clear();
        for (Item item : CollectionNavigation.selected().items()) {
            menu.items.add(new ItemStack(item));
        }
        scrollOffs = 0.0F;
        scrolling = false;
        menu.scrollTo(0.0F);
    }
}
