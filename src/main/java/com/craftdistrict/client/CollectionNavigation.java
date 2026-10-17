package com.craftdistrict.client;

import com.craftdistrict.registry.DecorationCollection;
import dev.architectury.platform.Platform;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Local screen widgets; the selected collection lasts only for this client session. */
public final class CollectionNavigation {
    private static final int BUTTON_WIDTH = 24;
    private static final int BUTTON_HEIGHT = 16;
    private static final int ICON_SIZE = 12;
    private static final int GAP = 2;
    private static final List<DecorationCollection> COLLECTIONS = List.of(DecorationCollection.values());
    private static DecorationCollection selected = DecorationCollection.BAKERY;

    private final List<CollectionButton> buttons = new ArrayList<>();
    private final Button previous;
    private final Button next;
    private int left;
    private int top;
    private int headerRight;
    private int windowHeight;
    private int gridTop;
    private int topCount;
    private int capacity;
    private int page;
    private boolean visible;
    private boolean overflow;

    public CollectionNavigation(Consumer<AbstractWidget> addWidget, Runnable onSelectionChanged) {
        for (DecorationCollection collection : COLLECTIONS) {
            CollectionButton button = new CollectionButton(collection, () -> {
                if (selected != collection) {
                    selected = collection;
                    onSelectionChanged.run();
                }
            });
            buttons.add(button);
            addWidget.accept(button);
        }
        previous = Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        next = Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        previous.setTooltip(Tooltip.create(Component.translatable("spectatorMenu.previous_page")));
        next.setTooltip(Tooltip.create(Component.translatable("spectatorMenu.next_page")));
        addWidget.accept(previous);
        addWidget.accept(next);
        hide();
    }

    public static DecorationCollection selected() {
        return selected;
    }

    public void show(int left, int top, int width, int height, int gridTop, Font font, Component title) {
        this.left = left;
        this.top = top;
        this.windowHeight = height;
        this.gridTop = gridTop;
        headerRight = left + width - 8;
        if (Platform.isFabric()) {
            // Fabric puts page arrows in the header; Forge/NeoForge put them above it.
            int paginationLeft = 170;
            //? if >=1.21 {
            paginationLeft = 171;
            //?}
            headerRight = Math.min(headerRight, left + paginationLeft - GAP);
        }
        // Fit categories between the title and this version's pagination controls.
        int available = headerRight - (left + 8 + font.width(title) + 6);
        topCount = Math.min(2, Math.max(0, (available + GAP) / (BUTTON_WIDTH + GAP)));
        int leftCount = Math.max(1, (height - gridTop + GAP) / (BUTTON_HEIGHT + GAP));
        overflow = COLLECTIONS.size() > topCount + leftCount;
        if (overflow) {
            leftCount = Math.max(1, leftCount - 1);
        }
        capacity = topCount + leftCount;
        page = selected.ordinal() / capacity;
        visible = true;
        updateButtons();
    }

    public void hide() {
        visible = false;
        for (CollectionButton button : buttons) {
            button.visible = false;
            button.active = false;
        }
        previous.visible = false;
        previous.active = false;
        next.visible = false;
        next.active = false;
    }

    private void changePage(int direction) {
        int lastPage = (COLLECTIONS.size() - 1) / capacity;
        page = Math.max(0, Math.min(lastPage, page + direction));
        updateButtons();
    }

    private void updateButtons() {
        int first = page * capacity;
        int topStart = headerRight - topCount * (BUTTON_WIDTH + GAP) + GAP;
        for (int index = 0; index < buttons.size(); index++) {
            CollectionButton button = buttons.get(index);
            int slot = index - first;
            button.visible = visible && slot >= 0 && slot < capacity;
            button.active = button.visible;
            if (!button.visible) {
                continue;
            }
            button.topEdge = slot < topCount;
            if (slot < topCount) {
                button.setX(topStart + slot * (BUTTON_WIDTH + GAP));
                button.setY(top + gridTop - BUTTON_HEIGHT - 1);
            } else {
                // Meet the frame's outer border rather than floating beside it.
                button.setX(left - BUTTON_WIDTH + 1);
                button.setY(top + gridTop + (slot - topCount) * (BUTTON_HEIGHT + GAP));
            }
        }
        previous.setX(left - 2 * (BUTTON_WIDTH + GAP));
        next.setX(left - BUTTON_WIDTH - GAP);
        previous.setY(top + windowHeight - BUTTON_HEIGHT - GAP);
        next.setY(previous.getY());
        previous.visible = visible && overflow;
        next.visible = visible && overflow;
        previous.active = previous.visible && page > 0;
        next.active = next.visible && (page + 1) * capacity < COLLECTIONS.size();
    }

    private static final class CollectionButton extends Button {
        private final DecorationCollection collection;
        private final ItemStack icon;
        private boolean topEdge;

        private CollectionButton(DecorationCollection collection, Runnable onPress) {
            super(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, collection.title(),
                    button -> onPress.run(), DEFAULT_NARRATION);
            this.collection = collection;
            this.icon = collection.icon();
            setTooltip(Tooltip.create(collection.title()));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            boolean selectedTab = selected == collection;
            boolean highlighted = isHoveredOrFocused();
            int face = selectedTab ? 0xFFC6C6C6 : highlighted ? 0xFFB5B5B5 : 0xFF949494;
            int highlight = selectedTab || highlighted ? 0xFFFFFFFF : 0xFFCECECE;
            int shadow = selectedTab ? 0xFF8B8B8B : 0xFF5D5D5D;

            // Stepped corners and a light/dark bevel match the vanilla inventory frame.
            graphics.fill(x + 1, y, x + width - 1, y + height, 0xFF373737);
            graphics.fill(x, y + 1, x + width, y + height - 1, 0xFF373737);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, face);
            graphics.fill(x + 2, y + 1, x + width - 2, y + 2, highlight);
            graphics.fill(x + 1, y + 2, x + 2, y + height - 2, highlight);
            graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 1, shadow);
            graphics.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, shadow);
            if (selectedTab) {
                // The open joining edge also identifies selection without relying on color.
                if (topEdge) {
                    graphics.fill(x + 1, y + height - 2, x + width - 1, y + height, face);
                } else {
                    graphics.fill(x + width - 2, y + 1, x + width, y + height - 1, face);
                }
            }
            graphics.pose().pushPose();
            graphics.pose().translate(x + (width - ICON_SIZE) / 2, y + (height - ICON_SIZE) / 2, 0);
            graphics.pose().scale(ICON_SIZE / 16.0F, ICON_SIZE / 16.0F, 1.0F);
            graphics.renderItem(icon, 0, 0);
            graphics.pose().popPose();
        }
    }
}
