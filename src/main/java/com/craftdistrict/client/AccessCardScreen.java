package com.craftdistrict.client;

import com.craftdistrict.item.AccessCardItem;
import com.craftdistrict.menu.AccessCardMenu;
import com.craftdistrict.network.AccessControlNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import org.lwjgl.glfw.GLFW;

public final class AccessCardScreen extends AbstractContainerScreen<AccessCardMenu> {
    private final Swatch[] swatches = new Swatch[AccessCardItem.COLOR_COUNT];
    private EditBox name;
    private Button save;
    private String draft;
    private int color;
    private boolean pending;

    public AccessCardScreen(AccessCardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 236;
        imageHeight = 150;
        color = menu.color();
        draft = menu.name();
    }

    private static Component text(String key) { return Component.translatable("screen.craftdistrict.access_card." + key); }

    static int cardColor(int color) {
        return mix(0x15273a, palette(color), 60);
    }

    private static int palette(int color) { return DyeColor.byId(color).getFireworkColor(); }

    private static int mix(int base, int color, int weight) {
        int result = 0xff000000;
        for (int shift = 0; shift < 24; shift += 8) {
            int channel = ((base >> shift & 255) * (100 - weight) + (color >> shift & 255) * weight) / 100;
            result |= channel << shift;
        }
        return result;
    }

    @Override protected void init() {
        super.init();
        name = addRenderableWidget(new EditBox(font, leftPos + 14, topPos + 54, 208, 19, text("name")));
        name.setMaxLength(AccessCardItem.MAX_NAME);
        name.setFilter(AccessCardItem::validName);
        name.setHint(AccessCardItem.displayName(""));
        name.setValue(draft);
        name.setResponder(value -> draft = value);
        for (int id = 0; id < swatches.length; id++) {
            swatches[id] = addRenderableWidget(new Swatch(leftPos + 14 + id * 13, topPos + 93, id));
        }
        addRenderableWidget(new AccessControlButton(leftPos + 14, topPos + 120, 100, 19, CommonComponents.GUI_CANCEL, button -> onClose()));
        save = addRenderableWidget(new AccessControlButton(leftPos + 122, topPos + 120, 100, 19, text("save"), button -> submit()));
        setInitialFocus(name);
        refresh();
    }

    private void refresh() {
        name.setEditable(!pending);
        save.active = !pending;
        for (Swatch swatch : swatches) swatch.active = !pending;
    }

    private void submit() {
        if (pending || !AccessCardItem.validName(draft) || !AccessCardItem.validColor(color)) return;
        pending = true;
        refresh();
        AccessControlNetworking.saveCard(menu, draft, color);
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (name == null || !name.isFocused() || key == GLFW.GLFW_KEY_ESCAPE)
            return super.keyPressed(key, scan, modifiers);

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
        } else if (key == GLFW.GLFW_KEY_TAB) {
            var navigation = new FocusNavigationEvent.TabNavigation(!hasShiftDown());
            var path = nextFocusPath(navigation);
            if (path == null) {
                setFocused(null);
                path = nextFocusPath(navigation);
            }
            if (path != null) changeFocus(path);
        } else {
            // Printable characters arrive separately through charTyped.
            name.keyPressed(key, scan, modifiers);
        }
        return true;
    }

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        int edge = mix(0xb9d3e4, palette(color), 33);
        rounded(graphics, leftPos, topPos, imageWidth, imageHeight, edge);
        rounded(graphics, leftPos + 1, topPos + 1, imageWidth - 2, imageHeight - 2, mix(0x111e32, palette(color), 22));
        graphics.fill(leftPos + 14, topPos + 38, leftPos + 222, topPos + 39, edge);
        int x = leftPos + 199, y = topPos + 15;
        graphics.fill(x, y + 6, x + 2, y + 8, edge);
        for (int band = 0; band < 3; band++) {
            int run = 3 + band * 3;
            graphics.fill(x + run, y + 4 - band * 2, x + run + 2, y + 10 + band * 2, edge);
            graphics.fill(x + run - 1, y + 3 - band * 2, x + run, y + 5 - band * 2, edge);
            graphics.fill(x + run - 1, y + 9 + band * 2, x + run, y + 11 + band * 2, edge);
        }
    }

    private static void rounded(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x + 5, y, x + width - 5, y + height, color);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, color);
        graphics.fill(x, y + 5, x + width, y + height - 5, color);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, text("brand"), 14, 11, 0xffb7cadb, false);
        graphics.drawString(font, title, 14, 25, 0xfff2f8ff, false);
        graphics.drawString(font, text("name"), 14, 44, 0xffdce9f4, false);
        graphics.drawString(font, text("color"), 14, 81, 0xffdce9f4, false);
        Component selected = AccessCardItem.colorName(color);
        graphics.drawString(font, selected, 222 - font.width(selected), 81, 0xffdce9f4, false);
    }

    @Override public void render(GuiGraphics graphics, int x, int y, float delta) {
        //? if <1.21 {
        /*renderBackground(graphics);
        *///?}
        super.render(graphics, x, y, delta);
    }

    private final class Swatch extends Button {
        private final int dye;

        private Swatch(int x, int y, int dye) {
            super(x, y, 11, 15, AccessCardItem.colorName(dye), button -> color = dye, DEFAULT_NARRATION);
            this.dye = dye;
            setTooltip(Tooltip.create(AccessCardItem.colorName(dye)));
        }

        @Override protected void renderWidget(GuiGraphics graphics, int x, int y, float delta) {
            int edge = dye == color ? 0xfff3f9ff : isHoveredOrFocused() ? 0xff9ac6f2 : 0xff34475d;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, edge);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xff000000 | palette(dye));
            if (dye == color) graphics.fill(getX() + 3, getY() + 11, getX() + 8, getY() + 13, edge);
        }
    }
}
