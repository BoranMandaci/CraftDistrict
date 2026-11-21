package com.craftdistrict.client;

import com.craftdistrict.access.AccessControlData;
import com.craftdistrict.item.AccessCardItem;
import com.craftdistrict.menu.CardReaderMenu;
import com.craftdistrict.network.AccessControlNetworking;
import com.craftdistrict.network.AccessControlNetworking.Action;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CardReaderScreen extends AbstractContainerScreen<CardReaderMenu> {
    private static final int ROW_Y = 118, ROW_HEIGHT = 20;
    private final Button[] roles = new Button[CardReaderMenu.PAGE_SIZE], removals = new Button[CardReaderMenu.PAGE_SIZE];
    private final List<List<FormattedCharSequence>> helpSteps = new ArrayList<>();
    private Button enabled, enrollment, unlink, previous, next, help;
    private DurationSlider duration;
    private boolean pending, showingHelp;
    private int shownVersion;

    public CardReaderScreen(CardReaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 310;
        imageHeight = 226;
        shownVersion = menu.view().version();
    }

    private static Component text(String key, Object... args) { return Component.translatable("screen.craftdistrict.card_reader." + key, args); }

    @Override protected void init() {
        super.init();
        if (shownVersion != menu.view().version()) pending = false;
        shownVersion = menu.view().version();
        enabled = button(12, 26, 119, 18, Component.empty(), b -> submit(Action.ENABLED, menu.view().enabled() ? 0 : 1));
        enrollment = button(138, 26, 160, 18, Component.empty(), b -> submit(Action.ENROLLMENT, menu.enrollmentTicks() > 0 ? 0 : 1));
        unlink = button(235, 55, 63, 18, text("unlink"), b -> submit(Action.UNLINK, 0));
        duration = addRenderableWidget(new DurationSlider(leftPos + 12, topPos + 81, menu.view().seconds()));
        for (int i = 0; i < roles.length; i++) {
            final int row = i;
            int y = ROW_Y + i * ROW_HEIGHT;
            roles[i] = button(154, y + 1, 90, 17, Component.empty(), b -> {
                var cards = menu.view().rows();
                if (row < cards.size()) submit(cards.get(row).administrator() ? Action.DOOR_ACCESS : Action.ADMINISTRATOR, row);
            });
            removals[i] = button(250, y + 1, 48, 17, text("remove"), b -> submit(Action.REMOVE, row));
        }
        previous = button(12, 204, 26, 18, Component.literal("<"), b -> submit(Action.PAGE, menu.view().page() - 1));
        next = button(42, 204, 26, 18, Component.literal(">"), b -> submit(Action.PAGE, menu.view().page() + 1));
        previous.setTooltip(Tooltip.create(text("previous")));
        next.setTooltip(Tooltip.create(text("next")));
        help = button(210, 204, 18, 18, Component.empty(), b -> showHelp(!showingHelp));
        button(234, 204, 64, 18, CommonComponents.GUI_DONE, b -> onClose());
        helpSteps.clear();
        for (int step = 1; step <= 6; step++) {
            helpSteps.add(font.split(text("help_step_" + step), imageWidth - 54));
        }
        showHelp(showingHelp);
    }

    private Button button(int x, int y, int w, int h, Component label, Button.OnPress click) {
        return addRenderableWidget(new AccessControlButton(leftPos + x, topPos + y, w, h, label, click));
    }

    private void refreshView() {
        var view = menu.view();
        enabled.visible = enrollment.visible = unlink.visible = duration.visible = previous.visible = next.visible = !showingHelp;
        enabled.setMessage(text(view.enabled() ? "enabled" : "disabled"));
        int seconds = (menu.enrollmentTicks() + 19) / 20;
        enrollment.setMessage(seconds > 0 ? text("enrollment_on", seconds) : text("enrollment_off"));
        unlink.active = !pending && view.door() != null;
        previous.active = !pending && view.page() > 0;
        next.active = !pending && view.page() + 1 < view.pages();
        enabled.active = enrollment.active = duration.active = !pending;
        if (!pending && !duration.dragging) duration.show(view.seconds());
        for (int i = 0; i < roles.length; i++) {
            roles[i].visible = removals[i].visible = !showingHelp && i < view.rows().size();
            if (!roles[i].visible) continue;
            var row = view.rows().get(i);
            roles[i].setMessage(text(row.administrator() ? "administrator" : "door_access"));
            roles[i].active = removals[i].active = !pending && !row.protectedCard();
            roles[i].setTooltip(Tooltip.create(text(row.protectedCard() ? "last_admin" : row.administrator() ? "demote" : "promote")));
            removals[i].setTooltip(row.protectedCard() ? Tooltip.create(text("last_admin")) : null);
        }
    }

    private void showHelp(boolean open) {
        showingHelp = open;
        if (open && duration.dragging) {
            duration.dragging = false;
            setDragging(false);
        }
        help.setMessage(Component.literal(open ? "×" : "?"));
        help.setTooltip(Tooltip.create(open ? CommonComponents.GUI_BACK : text("help_title")));
        refreshView();
    }

    private void submit(Action action, int value) {
        if (pending || (action != Action.DURATION && shownVersion != menu.view().version())) return;
        pending = true;
        AccessControlNetworking.editReader(menu, action, value);
        refreshView();
    }

    @Override protected void containerTick() {
        super.containerTick();
        if (shownVersion != menu.view().version()) {
            shownVersion = menu.view().version();
            pending = false;
            refreshView();
        } else {
            int seconds = (menu.enrollmentTicks() + 19) / 20;
            enrollment.setMessage(seconds > 0 ? text("enrollment_on", seconds) : text("enrollment_off"));
        }
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        // Container screens handle slot drags themselves, so route this drag to the slider.
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && duration.dragging) return duration.mouseDragged(x, y, button, dx, dy);
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && duration.dragging) {
            setDragging(false);
            return duration.mouseReleased(x, y, button);
        }
        return super.mouseReleased(x, y, button);
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (showingHelp && key == GLFW.GLFW_KEY_ESCAPE) {
            showHelp(false);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff425871);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xff121f31);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 4, 0xff69c3c4);
        if (showingHelp) {
            graphics.fill(leftPos + 12, topPos + 27, leftPos + 298, topPos + 197, 0xff1a2b40);
            return;
        }
        graphics.fill(leftPos + 12, topPos + 49, leftPos + 298, topPos + 77, 0xff1a2b40);
        for (int i = 0; i < roles.length; i++) {
            int y = topPos + ROW_Y + i * ROW_HEIGHT;
            graphics.fill(leftPos + 12, y, leftPos + 298, y + ROW_HEIGHT - 1, i % 2 == 0 ? 0xff1a2b40 : 0xff18263a);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (showingHelp) {
            graphics.drawString(font, text("help_title"), 12, 12, 0xffedf6ff, false);
            int y = 34;
            for (int i = 0; i < helpSteps.size(); i++) {
                graphics.drawString(font, (i + 1) + ".", 20, y, 0xff91d6d5, false);
                for (var line : helpSteps.get(i)) {
                    graphics.drawString(font, line, 35, y, 0xffedf6ff, false);
                    y += font.lineHeight;
                }
                y += 5;
            }
            return;
        }
        var view = menu.view();
        graphics.drawString(font, title, 12, 12, 0xffedf6ff, false);
        Component status = text(view.configured() ? "configured" : "unconfigured");
        graphics.drawString(font, status, 298 - font.width(status), 12, 0xff91d6d5, false);
        graphics.drawString(font, text("link_" + view.linkStatus().name().toLowerCase(Locale.ROOT)), 18, 53, 0xffa8bdd2, false);
        if (view.door() != null) {
            var block = BuiltInRegistries.BLOCK.get(view.door());
            Component name = block == Blocks.AIR ? Component.literal(view.door().getPath()) : block.getName();
            graphics.drawString(font, fit(name, 203), 18, 65, 0xffedf6ff, false);
        }
        graphics.drawString(font, text("cards", view.count()), 12, 106, 0xffa8bdd2, false);
        for (int i = 0; i < view.rows().size(); i++) {
            graphics.drawString(font, fit(AccessCardItem.displayName(view.rows().get(i).name()), 127), 18,
                    ROW_Y + i * ROW_HEIGHT + 5, 0xffedf6ff, false);
        }
        graphics.drawString(font, text("page", view.page() + 1, view.pages()), 77, 210, 0xffa8bdd2, false);
    }

    private String fit(Component value, int width) {
        String text = value.getString();
        return font.width(text) <= width ? text : font.plainSubstrByWidth(text, width - font.width("…")) + "…";
    }

    @Override public void render(GuiGraphics graphics, int x, int y, float delta) {
        //? if <1.21 {
        /*renderBackground(graphics);
        *///?}
        super.render(graphics, x, y, delta);
        if (showingHelp) return;
        int rowY = y - topPos - ROW_Y;
        int row = rowY / ROW_HEIGHT;
        if (x >= leftPos + 18 && x < leftPos + 145 && rowY >= 0 && row < menu.view().rows().size()) {
            Component name = AccessCardItem.displayName(menu.view().rows().get(row).name());
            if (font.width(name) > 127) graphics.renderTooltip(font, name, x, y);
        }
    }

    private final class DurationSlider extends AbstractSliderButton {
        private boolean dragging;

        private DurationSlider(int x, int y, int seconds) {
            super(x, y, 286, 19, text("duration", seconds), position(seconds));
        }

        private static double position(int seconds) { return (seconds - 1.0) / (AccessControlData.MAX_SECONDS - 1); }
        private int seconds() { return 1 + (int) Math.round(value * (AccessControlData.MAX_SECONDS - 1)); }
        private void show(int seconds) { value = position(seconds); updateMessage(); }

        @Override protected void updateMessage() { setMessage(text("duration", seconds())); }
        @Override protected void applyValue() { /* Mouse drag previews locally. */ }

        @Override public void onClick(double x, double y) {
            dragging = true;
            super.onClick(x, y);
        }

        @Override public void onRelease(double x, double y) {
            if (!dragging) return;
            super.onClick(x, y);
            super.onRelease(x, y);
            dragging = false;
            commit(seconds());
        }

        private void commit(int seconds) {
            show(seconds);
            if (seconds != menu.view().seconds()) submit(Action.DURATION, seconds);
        }

        @Override public boolean keyPressed(int key, int scan, int modifiers) {
            if (active && visible && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) {
                int next = menu.view().seconds() + (key == GLFW.GLFW_KEY_LEFT ? -1 : 1);
                if (!dragging && AccessControlData.validDuration(next)) commit(next);
                return true;
            }
            return super.keyPressed(key, scan, modifiers);
        }
    }
}
