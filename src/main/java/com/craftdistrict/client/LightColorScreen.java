package com.craftdistrict.client;

import com.craftdistrict.menu.LightColorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** A compact swatch palette using the standard menu lifecycle. */
public final class LightColorScreen extends AbstractContainerScreen<LightColorMenu> {
    private final List<ColorButton> colors = new ArrayList<>();
    private Button done;

    public LightColorScreen(LightColorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 212; imageHeight = 220;
    }
    private static Component text(String key, Object... args) {
        return Component.translatable("screen.craftdistrict.light_color." + key, args);
    }
    @Override protected void init() {
        super.init(); colors.clear();
        for (DyeColor color : DyeColor.values()) {
            int id = color.getId();
            colors.add(addRenderableWidget(new ColorButton(leftPos + 38 + id % 4 * 35,
                    topPos + 47 + id / 4 * 29, color)));
        }
        done = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            if (menu.pending()) return;
            if (menu.selectedColor() == menu.color()) onClose();
            else menu.confirmSelection();
        })
                .bounds(leftPos + 48, topPos + 191, 116, 20).build());
    }
    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff15191b);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xfff2f0e6);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xffc6c6c6);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + 38, 0xff30383b);
        graphics.fill(leftPos + 8, topPos + 40, leftPos + imageWidth - 8, topPos + 164, 0xff484e50);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 11, 0xffffffff);
        Component current = menu.color() < 0 ? text("original")
                : new ItemStack(DyeItem.byColor(DyeColor.byId(menu.color()))).getHoverName();
        graphics.drawCenteredString(font, text("current", current), imageWidth / 2, 27, 0xffe6f0ed);
        Component message = switch (menu.feedback()) {
            case MISSING_DYE -> text("missing_dye", new ItemStack(DyeItem.byColor(DyeColor.byId(menu.requestedColor()))).getHoverName());
            case STALE -> text("stale");
            case READY -> text(minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild
                    ? "hint_creative" : "hint");
        };
        var lines = font.split(message, imageWidth - 18);
        for (int i = 0; i < Math.min(2, lines.size()); i++)
            graphics.drawString(font, lines.get(i), (imageWidth - font.width(lines.get(i))) / 2, 168 + i * 9,
                    menu.feedback() == LightColorMenu.Feedback.READY ? 0xff252a2c : 0xff9b1c1c, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean pending = menu.pending();
        for (ColorButton button : colors) button.active = !pending;
        done.active = !pending;
        //? if <1.21 {
        /*renderBackground(graphics);
        *///?}
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
    private final class ColorButton extends Button {
        private final DyeColor color;
        private final Component colorName;
        ColorButton(int x, int y, DyeColor color) {
            super(x, y, 30, 25, new ItemStack(DyeItem.byColor(color)).getHoverName(),
                    button -> menu.chooseColor(color.getId()), DEFAULT_NARRATION);
            this.color = color; colorName = new ItemStack(DyeItem.byColor(color)).getHoverName();
            setTooltip(Tooltip.create(colorName));
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean selected = menu.selectedColor() == color.getId();
            graphics.fill(getX(), getY(), getX() + width, getY() + height,
                    selected ? 0xfff8e67e : isHoveredOrFocused() ? 0xffffffff : 0xff202629);
            graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0xff202629);
            graphics.fill(getX() + 3, getY() + 3, getX() + width - 3, getY() + height - 3,
                    LightingColors.color(0x1000000, color.getId()));
        }
        @Override public Component getMessage() {
            return menu.selectedColor() == color.getId() ? text("selected", colorName) : colorName;
        }
    }
}
