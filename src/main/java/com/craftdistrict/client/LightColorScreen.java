package com.craftdistrict.client;

import com.craftdistrict.lighting.LightMode;
import com.craftdistrict.lighting.LightEffect;
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
import java.util.function.BooleanSupplier;

/** A compact CraftDistrict panel; all controls edit pending menu choices until Done. */
public final class LightColorScreen extends AbstractContainerScreen<LightColorMenu> {
    private enum Tab { MODE, EFFECT, COLOR }
    private Tab tab = Tab.MODE;
    private final List<Button> choices = new ArrayList<>();
    private final Component[] colorNames = new Component[16];
    private Button done, powerOn, powerOff;
    private int powerRow;

    public LightColorScreen(LightColorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        for (DyeColor color : DyeColor.values())
            colorNames[color.getId()] = new ItemStack(DyeItem.byColor(color)).getHoverName();
    }
    private static Component text(String key, Object... args) {
        return Component.translatable("screen.craftdistrict.light_settings." + key, args);
    }
    private static Component paletteText(String key, Object... args) {
        return Component.translatable("screen.craftdistrict.light_color." + key, args);
    }
    private static Component modeName(LightMode mode) { return text("mode." + mode.key()); }
    private static Component effectName(LightEffect effect) { return text("effect." + effect.getSerializedName()); }

    @Override protected void init() {
        if (menu.colorOnly()) tab = Tab.COLOR;
        int longest = 0;
        if (!menu.colorOnly()) for (LightEffect effect : LightEffect.values()) longest = Math.max(longest, font.width(effectName(effect)));
        imageWidth = Math.min(width - 12, Math.max(264, longest * 2 + 56));
        imageHeight = Math.min(246, height - 12);
        super.init();
        choices.clear(); powerOn = powerOff = null;
        int count = menu.colorOnly() ? 1 : menu.allowsColor() ? 3 : 2;
        int tabWidth = (imageWidth - 24) / count;
        for (int i = 0; i < count; i++) {
            Tab target = menu.colorOnly() ? Tab.COLOR : Tab.values()[i];
            choices.add(addRenderableWidget(new PanelButton(leftPos + 12 + i * tabWidth, topPos + 45,
                    tabWidth - 4, 22, text("tab." + target.name().toLowerCase(java.util.Locale.ROOT)),
                    () -> tab == target, () -> { tab = target; rebuildWidgets(); }, false)));
        }
        int bodyTop = topPos + 75, bodyHeight = imageHeight - 136;
        int rows = tab == Tab.MODE ? 4 : 3;
        int rowHeight = Math.min(30, bodyHeight / rows);
        int optionTop = bodyTop + (bodyHeight - rowHeight * rows) / 2;
        if (tab == Tab.MODE) {
            for (LightMode mode : LightMode.values())
                choices.add(addRenderableWidget(new PanelButton(leftPos + 18, optionTop + mode.ordinal() * rowHeight,
                        imageWidth - 36, rowHeight - 4, modeName(mode),
                        () -> menu.selectedMode() == mode, () -> menu.chooseMode(mode), false)));
            powerRow = optionTop - topPos + rowHeight * 3;
            int half = (imageWidth - 36) / 2;
            powerOn = addRenderableWidget(new PanelButton(leftPos + 18, topPos + powerRow, half - 2, rowHeight - 4,
                    text("power_on"), menu::selectedPower, () -> menu.choosePower(true), false));
            powerOff = addRenderableWidget(new PanelButton(leftPos + 20 + half, topPos + powerRow, half - 2, rowHeight - 4,
                    text("power_off"), () -> !menu.selectedPower(), () -> menu.choosePower(false), false));
            choices.add(powerOn); choices.add(powerOff);
        } else if (tab == Tab.EFFECT) {
            int cell = (imageWidth - 32) / 2;
            for (LightEffect effect : LightEffect.values())
                choices.add(addRenderableWidget(new PanelButton(leftPos + 16 + effect.ordinal() % 2 * cell,
                        optionTop + effect.ordinal() / 2 * rowHeight, cell - 4, rowHeight - 4, effectName(effect),
                        () -> menu.selectedEffect() == effect, () -> menu.chooseEffect(effect), false)));
        } else {
            int gap = 4, size = Math.min(24, (bodyHeight - gap * 3) / 4);
            int grid = size * 4 + gap * 3;
            int startX = leftPos + (imageWidth - grid) / 2, startY = bodyTop + (bodyHeight - grid) / 2;
            for (DyeColor color : DyeColor.values()) {
                int id = color.getId();
                choices.add(addRenderableWidget(new ColorButton(startX + id % 4 * (size + gap),
                        startY + id / 4 * (size + gap), size, id)));
            }
        }
        int footer = topPos + imageHeight - 29, buttonWidth = (imageWidth - 30) / 2;
        addRenderableWidget(new PanelButton(leftPos + 12, footer, buttonWidth, 21, CommonComponents.GUI_CANCEL,
                () -> false, this::onClose, false));
        done = addRenderableWidget(new PanelButton(leftPos + imageWidth - buttonWidth - 12, footer, buttonWidth, 21,
                CommonComponents.GUI_DONE, () -> false, menu::confirmSelection, true));
    }

    /** Small pixel-rounded corners, with no texture dependency or custom renderer. */
    private static void rounded(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x + 2, y, x + width - 2, y + height, color);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
        graphics.fill(x, y + 2, x + width, y + height - 2, color);
    }
    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        rounded(graphics, leftPos - 2, topPos + 3, imageWidth + 4, imageHeight, 0x70000000);
        rounded(graphics, leftPos, topPos, imageWidth, imageHeight, 0xff40505c);
        rounded(graphics, leftPos + 1, topPos + 1, imageWidth - 2, imageHeight - 2, 0xff17212a);
        graphics.fill(leftPos + 14, topPos + 39, leftPos + imageWidth - 14, topPos + 40, 0xff31414d);
        rounded(graphics, leftPos + 11, topPos + 71, imageWidth - 22, imageHeight - 129, 0xff101920);
        graphics.fill(leftPos + 14, topPos + imageHeight - 35,
                leftPos + imageWidth - 14, topPos + imageHeight - 34, 0xff31414d);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 12, 0xffeff5f5, false);
        Component current = switch (tab) {
            case MODE -> text("mode_power", modeName(menu.mode()), text(menu.active() ? "on" : "off"));
            case EFFECT -> effectName(menu.effect());
            case COLOR -> menu.color() < 0 ? paletteText("original") : colorNames[menu.color()];
        };
        Component status = paletteText("current", current);
        graphics.drawString(font, status, (imageWidth - font.width(status)) / 2, 26, 0xff99b5bd, false);
        Component message = switch (menu.feedback()) {
            case MISSING_DYE -> menu.requiredDyes() > 1
                    ? text("missing_dyes", menu.requiredDyes(), colorNames[Math.max(0, menu.requestedColor())])
                    : paletteText("missing_dye", colorNames[Math.max(0, menu.requestedColor())]);
            case STALE -> menu.colorOnly() ? Component.translatable("screen.craftdistrict.bedding_color.stale") : paletteText("stale");
            case READY -> switch (tab) {
                case MODE -> text("hint.mode");
                case EFFECT -> text("hint.effect");
                case COLOR -> menu.requiredDyes() > 1 && minecraft != null && minecraft.player != null && !minecraft.player.getAbilities().instabuild
                        ? text("hint.group", menu.requiredDyes()) : minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild
                        ? menu.colorOnly() ? Component.translatable("screen.craftdistrict.bedding_color.hint_creative") : paletteText("hint_creative") : paletteText("hint");
            };
        };
        if (tab == Tab.MODE && menu.selectedMode() != LightMode.MANUAL) {
            Component automatic = text("power_auto", text(menu.active() ? "on" : "off"));
            graphics.drawString(font, automatic, (imageWidth - font.width(automatic)) / 2, powerRow + 5, 0xff99b5bd, false);
        }
        var lines = font.split(message, imageWidth - 24);
        for (int i = 0; i < Math.min(2, lines.size()); i++)
            graphics.drawString(font, lines.get(i), (imageWidth - font.width(lines.get(i))) / 2,
                    imageHeight - 55 + i * 9,
                    menu.feedback() == LightColorMenu.Feedback.READY ? 0xffb3c6cb : 0xffffb0a0, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean pending = menu.pending();
        for (Button button : choices) button.active = !pending;
        done.active = !pending;
        if (powerOn != null) {
            boolean manual = menu.selectedMode() == LightMode.MANUAL;
            powerOn.visible = powerOff.visible = manual;
            powerOn.active = powerOff.active = manual && !pending;
        }
        //? if <1.21 {
        /*renderBackground(graphics);
        *///?}
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private final class PanelButton extends Button {
        private final BooleanSupplier selected;
        private final boolean primary;
        PanelButton(int x, int y, int width, int height, Component label, BooleanSupplier selected, Runnable action, boolean primary) {
            super(x, y, width, height, label, button -> action.run(), DEFAULT_NARRATION);
            this.selected = selected; this.primary = primary;
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean chosen = selected.getAsBoolean(), hover = active && isHoveredOrFocused();
            int border = !active ? 0xff30404a : chosen ? 0xff7dd8ca : hover ? 0xff8baeb8 : 0xff405461;
            int fill = !active ? 0xff1c2a33 : primary ? hover ? 0xff268678 : 0xff206e64
                    : chosen ? 0xff234a4d : hover ? 0xff304652 : 0xff253540;
            rounded(graphics, getX(), getY(), width, height, border);
            rounded(graphics, getX() + 1, getY() + 1, width - 2, height - 2, fill);
            if (chosen) graphics.fill(getX() + 4, getY() + 5, getX() + 6, getY() + height - 5, 0xff7dd8ca);
            renderScrollingString(graphics, font, 9, active ? 0xffedf4f4 : 0xff738c97);
        }
    }

    private final class ColorButton extends Button {
        private final int color;
        ColorButton(int x, int y, int size, int color) {
            super(x, y, size, size, colorNames[color], button -> menu.chooseColor(color), DEFAULT_NARRATION);
            this.color = color;
            setTooltip(Tooltip.create(colorNames[color]));
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean selected = menu.selectedColor() == color;
            rounded(graphics, getX(), getY(), width, height,
                    selected ? 0xff8fe5d6 : active && isHoveredOrFocused() ? 0xffe0eeee : 0xff435762);
            rounded(graphics, getX() + 2, getY() + 2, width - 4, height - 4, 0xff142129);
            graphics.fill(getX() + 4, getY() + 4, getX() + width - 4, getY() + height - 4,
                    LightingColors.color(0x1000000, color));
            if (selected) graphics.fill(getX() + width - 5, getY() + 2, getX() + width - 2, getY() + 5, 0xffeffffb);
        }
        @Override public Component getMessage() {
            return menu.selectedColor() == color ? paletteText("selected", colorNames[color]) : colorNames[color];
        }
    }
}
