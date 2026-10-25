package com.craftdistrict.client;

import com.craftdistrict.assembly.*;
import com.craftdistrict.menu.AssemblyStationMenu;
import com.craftdistrict.registry.CraftDistrictBlocks;
import com.craftdistrict.registry.DecorationCollection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Fits Minecraft's minimum normal 320x240 GUI; long catalogs and material lists scroll independently. */
public final class AssemblyStationScreen extends AbstractContainerScreen<AssemblyStationMenu> {
    private static final int LIST_X = 8, LIST_Y = 58, LIST_WIDTH = 144, ROW_HEIGHT = 20, VISIBLE_ROWS = 4;
    private static final int DETAIL_X = 160, COST_Y = 92, CELL_WIDTH = 73, CELL_HEIGHT = 16, VISIBLE_COST_ROWS = 2;
    private record Row(AssemblyCatalog.Entry entry, ItemStack icon, Component name, String searchName) { }
    private final Inventory inventory;
    private final List<Button> filters = new ArrayList<>();
    private final List<ItemStack> filterIcons = new ArrayList<>();
    private List<AssemblyCatalog.Entry> catalog = List.of();
    private List<Row> rows = List.of(), visible = List.of();
    private List<ItemStack> inventorySnapshot = List.of();
    private EditBox search;
    private Button craft, decrease, increase;
    private DecorationCollection filter;
    private ResourceLocation selected;
    private Row selection;
    private AssemblyCrafting.Plan preview;
    private int scroll, costScroll, catalogTicks;
    private int quantity = 1, buttonQuantity = -1;
    private boolean dragging;
    private boolean wasPending;
    private int errorTicks;
    private String serverError;
    private String buttonLabel = "";
    private final QuantityRepeat quantityRepeat = new QuantityRepeat();
    private Button heldQuantityButton;

    /** A clock-driven schedule; delayed frames combine due steps into one preview update. */
    static final class QuantityRepeat {
        private int direction;
        private long pressedAt, nextAt;

        void press(int direction, long now) {
            if (direction != -1 && direction != 1) throw new IllegalArgumentException("Invalid repeat direction");
            this.direction = direction; pressedAt = now; nextAt = now + 300;
        }
        int poll(long now) {
            if (direction == 0) return 0;
            if (now < pressedAt) { cancel(); return 0; }
            int due = 0;
            while (now >= nextAt && due < 63) {
                due++;
                nextAt += nextAt - pressedAt >= 1000 ? 40 : 100;
            }
            if (due == 63 && now >= nextAt) nextAt = now + 40;
            return direction * due;
        }
        void cancel() { direction = 0; }
    }

    public AssemblyStationScreen(AssemblyStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.inventory = inventory;
        imageWidth = AssemblyStationMenu.WIDTH; imageHeight = AssemblyStationMenu.HEIGHT;
    }

    private static Component text(String key) { return Component.translatable("screen.craftdistrict.assembly." + key); }

    @Override protected void init() {
        stopQuantityRepeat();
        String query = search == null ? "" : search.getValue();
        super.init();
        search = addRenderableWidget(new EditBox(font, leftPos + 145, topPos + 7, 163, 18, text("search")));
        search.setMaxLength(100); search.setHint(text("search")); search.setValue(query);
        search.setResponder(ignored -> applyFilter());
        filters.clear();
        filterIcons.clear();
        filterIcons.add(new ItemStack(CraftDistrictBlocks.Survival.ASSEMBLY_STATION.get()));
        int start = 8;
        filters.add(addRenderableWidget(Button.builder(Component.empty(), button -> chooseFilter(null))
                .bounds(leftPos + start, topPos + 32, 24, 20).tooltip(Tooltip.create(text("all"))).build()));
        for (DecorationCollection collection : AssemblyCatalog.collections()) {
            filterIcons.add(collection.icon());
            int x = start + filters.size() * 27;
            filters.add(addRenderableWidget(Button.builder(Component.empty(), button -> chooseFilter(collection))
                    .bounds(leftPos + x, topPos + 32, 24, 20).tooltip(Tooltip.create(collection.title())).build()));
        }
        decrease = addRenderableWidget(Button.builder(Component.literal("-"), button -> setQuantity(quantity - 1))
                .bounds(leftPos + 253, topPos + 32, 16, 20).tooltip(Tooltip.create(text("decrease"))).build());
        increase = addRenderableWidget(Button.builder(Component.literal("+"), button -> setQuantity(quantity + 1))
                .bounds(leftPos + 292, topPos + 32, 16, 20).tooltip(Tooltip.create(text("increase"))).build());
        craft = addRenderableWidget(Button.builder(Component.translatable("screen.craftdistrict.assembly.craft", quantity), button -> {
            if (selected != null && preview != null && preview.result() == AssemblyCrafting.Result.READY) {
                menu.requestCraft(selected, quantity); updateButton();
                wasPending = menu.pending();
            }
        }).bounds(leftPos + DETAIL_X, topPos + 126, 148, 18).build());
        buttonLabel = ""; buttonQuantity = -1;
        refreshCatalog();
        refreshInventory();
        updateButton();
    }

    private void chooseFilter(DecorationCollection collection) { filter = collection; applyFilter(); }

    private void setQuantity(int value) {
        int next = Math.max(AssemblyCrafting.MIN_OPERATIONS, Math.min(AssemblyCrafting.MAX_OPERATIONS, value));
        if (quantity == next) return;
        quantity = next; errorTicks = 0; refreshPreview();
    }

    private void stopQuantityRepeat() {
        heldQuantityButton = null;
        quantityRepeat.cancel();
    }

    private void updateQuantityRepeat(double mouseX, double mouseY) {
        if (heldQuantityButton == null) return;
        if (minecraft == null || minecraft.screen != this || !minecraft.isWindowActive()
                || !heldQuantityButton.active || !heldQuantityButton.isFocused()
                || !heldQuantityButton.isMouseOver(mouseX, mouseY)
                || GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            stopQuantityRepeat(); return;
        }
        int steps = quantityRepeat.poll(Util.getMillis());
        if (steps != 0) setQuantity(quantity + steps);
    }

    private void refreshCatalog() {
        catalog = AssemblyCatalog.entries(inventory.player.level().getRecipeManager());
        rows = catalog.stream().map(entry -> {
            ItemStack icon = entry.recipe().output(); Component name = icon.getHoverName();
            return new Row(entry, icon, name, AssemblyCatalog.normalizeSearch(name.getString()));
        }).toList();
        applyFilter();
    }

    private void applyFilter() {
        ResourceLocation previous = selected;
        String query = AssemblyCatalog.normalizeSearch(search.getValue());
        visible = rows.stream().filter(row -> AssemblyCatalog.matches(row.icon().getItem(), filter, row.searchName(), query)).toList();
        if (visible.stream().noneMatch(row -> row.entry().id().equals(selected))) selected = visible.isEmpty() ? null : visible.get(0).entry().id();
        if (!Objects.equals(previous, selected)) quantity = 1;
        selection = visible.stream().filter(row -> row.entry().id().equals(selected)).findFirst().orElse(null);
        errorTicks = 0;
        scroll = 0; costScroll = 0;
        if (selected != null) for (int i = 0; i < visible.size(); i++) if (visible.get(i).entry().id().equals(selected)) {
            scroll = Math.min(Math.max(0, i - VISIBLE_ROWS + 1), Math.max(0, visible.size() - VISIBLE_ROWS)); break;
        }
        for (int i = 0; i < filters.size(); i++) filters.get(i).active = i == 0 ? filter != null : filter != AssemblyCatalog.collections().get(i - 1);
        refreshPreview();
    }

    private Row selectedRow() {
        return selection;
    }

    private void refreshInventory() {
        inventorySnapshot = AssemblyCrafting.snapshot(inventory); refreshPreview();
    }

    private void refreshPreview() {
        Row row = selectedRow();
        preview = row == null || inventorySnapshot.size() != 36 ? null : AssemblyCrafting.plan(row.entry().recipe(), inventorySnapshot, quantity);
        updateButton();
    }

    private void updateButton() {
        if (craft == null) return;
        boolean pending = menu.pending();
        String label = pending ? "pending" : preview == null ? "craft" : switch (preview.result()) {
            case MISSING_MATERIALS -> "missing";
            case INVENTORY_FULL -> "full";
            default -> "craft";
        };
        if (!pending && preview != null && preview.result() == AssemblyCrafting.Result.READY && errorTicks > 0) label = serverError;
        if (!buttonLabel.equals(label) || buttonQuantity != quantity) {
            Component message = label.equals("craft") ? Component.translatable("screen.craftdistrict.assembly.craft", quantity) : text(label);
            craft.setMessage(message); craft.setTooltip(Tooltip.create(message)); buttonLabel = label; buttonQuantity = quantity;
        }
        craft.active = !pending && preview != null && preview.result() == AssemblyCrafting.Result.READY;
        decrease.active = !pending && quantity > AssemblyCrafting.MIN_OPERATIONS;
        increase.active = !pending && quantity < AssemblyCrafting.MAX_OPERATIONS;
        if (heldQuantityButton != null && !heldQuantityButton.active) stopQuantityRepeat();
    }

    @Override protected void containerTick() {
        super.containerTick();
        //? if <1.21 {
        /*search.tick();
        *///?}
        if (++catalogTicks >= 10) {
            catalogTicks = 0;
            if (!AssemblyCatalog.same(catalog, AssemblyCatalog.entries(inventory.player.level().getRecipeManager()))) refreshCatalog();
        }
        boolean changed = inventorySnapshot.size() != 36;
        for (int i = 0; !changed && i < 36; i++) changed = !ItemStack.matches(inventorySnapshot.get(i), inventory.getItem(i));
        if (changed) refreshInventory();
        boolean pending = menu.pending();
        if (wasPending && !pending && Objects.equals(selected, menu.requestedRecipe())) {
            serverError = switch (menu.feedback()) {
                case MISSING_MATERIALS -> "missing";
                case INVENTORY_FULL -> "full";
                case STALE_RECIPE -> "reload";
                case BUSY -> "busy";
                default -> null;
            };
            errorTicks = serverError == null ? 0 : 40;
        }
        wasPending = pending;
        if (errorTicks > 0) errorTicks--;
        updateButton();
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        graphics.fill(x, y, x + width, y + height, 0xff343b3e);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xffe0dfd4);
    }

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        panel(graphics, leftPos, topPos, imageWidth, imageHeight, 0xffb9b7a9);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 28, 0xff7c5b3e);
        graphics.fill(leftPos + 3, topPos + 29, leftPos + imageWidth - 3, topPos + 55, 0xff969d97);
        panel(graphics, leftPos + 270, topPos + 33, 21, 18, 0xff333c3e);
        panel(graphics, leftPos + LIST_X, topPos + LIST_Y, LIST_WIDTH, 84, 0xff333c3e);
        panel(graphics, leftPos + DETAIL_X - 2, topPos + LIST_Y, 152, 66, 0xffd2d0c2);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xfff0eee4);
            graphics.fill(x - 1, y - 1, x + 16, y + 16, 0xff505856);
            graphics.fill(x, y, x + 16, y + 16, 0xff8e948d);
        }
    }

    private String shorten(Component component, int width) {
        String value = component.getString();
        return font.width(value) <= width ? value : font.plainSubstrByWidth(value, Math.max(0, width - font.width("…"))) + "…";
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, shorten(title, 130), 8, 12, 0xfff2e4c8, false);
        graphics.drawString(font, shorten(text("quantity"), 49), 201, 38, 0xff303833, false);
        graphics.drawCenteredString(font, Integer.toString(quantity), 280, 38, 0xfff1efe3);
        graphics.drawString(font, Component.translatable("container.inventory"), AssemblyStationMenu.INVENTORY_X, 146, 0xff303833, false);
        if (visible.isEmpty()) {
            graphics.drawWordWrap(font, text("none"), LIST_X + 8, LIST_Y + 25, LIST_WIDTH - 18, 0xffdeded4);
        }
        for (int i = scroll; i < Math.min(visible.size(), scroll + VISIBLE_ROWS); i++) {
            Row row = visible.get(i); int y = LIST_Y + 2 + (i - scroll) * ROW_HEIGHT;
            if (row.entry().id().equals(selected)) {
                graphics.fill(LIST_X + 2, y, LIST_X + LIST_WIDTH - 6, y + ROW_HEIGHT, 0xff6c796e);
                graphics.fill(LIST_X + 2, y, LIST_X + 4, y + ROW_HEIGHT, 0xffdab76d);
            }
            graphics.renderItem(row.icon(), LIST_X + 5, y + 2);
            graphics.drawString(font, shorten(row.name(), LIST_WIDTH - 33), LIST_X + 25, y + 6, 0xfff1efe3, false);
        }
        if (visible.size() > VISIBLE_ROWS) {
            int thumb = Math.max(10, 80 * VISIBLE_ROWS / visible.size());
            int y = LIST_Y + 2 + (80 - thumb) * scroll / (visible.size() - VISIBLE_ROWS);
            graphics.fill(LIST_X + LIST_WIDTH - 5, LIST_Y + 2, LIST_X + LIST_WIDTH - 2, LIST_Y + 82, 0xff1d282a);
            graphics.fill(LIST_X + LIST_WIDTH - 5, y, LIST_X + LIST_WIDTH - 2, y + thumb, 0xffd4c292);
        }
        Row row = selectedRow();
        if (row == null) return;
        graphics.renderItem(row.icon(), DETAIL_X + 2, LIST_Y + 3);
        int totalOutput = Math.multiplyExact(row.icon().getCount(), quantity);
        graphics.renderItemDecorations(font, row.icon(), DETAIL_X + 2, LIST_Y + 3, totalOutput > 1 ? Integer.toString(totalOutput) : null);
        var lines = font.split(row.name(), 122);
        for (int i = 0; i < Math.min(2, lines.size()); i++) graphics.drawString(font, lines.get(i), DETAIL_X + 24, LIST_Y + 2 + i * 9, 0xff252c2c, false);
        graphics.drawString(font, shorten(text("counts"), 145), DETAIL_X, 81, 0xff39443c, false);
        List<AssemblyIngredient> requirements = row.entry().recipe().requirements();
        for (int i = costScroll * 2; i < Math.min(requirements.size(), (costScroll + VISIBLE_COST_ROWS) * 2); i++) {
            int x = DETAIL_X + (i % 2) * (CELL_WIDTH + 1), y = COST_Y + (i / 2 - costScroll) * CELL_HEIGHT;
            graphics.fill(x, y, x + CELL_WIDTH, y + CELL_HEIGHT, 0xffb8bcaf);
            ItemStack icon = ingredientIcon(requirements.get(i)); graphics.renderItem(icon, x, y);
            int available = preview == null ? 0 : preview.available()[i];
            int required = AssemblyCrafting.requiredCount(requirements.get(i), quantity);
            boolean missing = preview == null || preview.allocated()[i] < required;
            graphics.drawString(font, shorten(Component.literal(available + "/" + required), CELL_WIDTH - 19), x + 18, y + 4, missing ? 0xff953a32 : 0xff245438, false);
        }
        if (requirements.size() > VISIBLE_COST_ROWS * 2) graphics.fill(DETAIL_X + 147, COST_Y + costScroll * 5, DETAIL_X + 149, COST_Y + costScroll * 5 + 10, 0xff7c5b3e);
    }

    private static ItemStack ingredientIcon(AssemblyIngredient ingredient) {
        ItemStack[] items = ingredient.ingredient().getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0];
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        updateQuantityRepeat(mouseX, mouseY);
        //? if <1.21 {
        /*renderBackground(graphics);
        *///?}
        super.render(graphics, mouseX, mouseY, delta);
        for (int i = 0; i < filters.size(); i++) {
            graphics.renderItem(filterIcons.get(i), filters.get(i).getX() + 4, filters.get(i).getY() + 2);
        }
        renderTooltip(graphics, mouseX, mouseY);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= LIST_X && x < LIST_X + LIST_WIDTH - 6 && y >= LIST_Y + 2 && y < LIST_Y + 82) {
            int index = scroll + (y - LIST_Y - 2) / ROW_HEIGHT;
            if (index < visible.size()) graphics.renderTooltip(font, visible.get(index).icon(), mouseX, mouseY);
        }
        Row row = selectedRow();
        if (row != null && x >= DETAIL_X && x < DETAIL_X + 148 && y >= COST_Y && y < COST_Y + VISIBLE_COST_ROWS * CELL_HEIGHT) {
            int column = Math.min(1, (x - DETAIL_X) / (CELL_WIDTH + 1));
            int index = (costScroll + (y - COST_Y) / CELL_HEIGHT) * 2 + column;
            if (index < row.entry().recipe().requirements().size()) {
                AssemblyIngredient ingredient = row.entry().recipe().requirements().get(index);
                graphics.renderComponentTooltip(font, List.of(ingredientIcon(ingredient).getHoverName(), text("materials"),
                        Component.translatable("screen.craftdistrict.assembly.amount", preview == null ? 0 : preview.available()[index], AssemblyCrafting.requiredCount(ingredient, quantity))), mouseX, mouseY);
            }
        }
        if (row != null && x >= DETAIL_X && x < DETAIL_X + 148 && y >= LIST_Y && y < LIST_Y + 22) {
            graphics.renderComponentTooltip(font, List.of(row.name(), Component.translatable("screen.craftdistrict.assembly.output", Math.multiplyExact(row.icon().getCount(), quantity))), mouseX, mouseY);
        }
        if (x >= 201 && x < 292 && y >= 32 && y < 52) graphics.renderTooltip(font, Component.translatable("screen.craftdistrict.assembly.quantity_hint", quantity, AssemblyCrafting.MAX_OPERATIONS), mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        stopQuantityRepeat();
        int x = (int) mouseX - leftPos, y = (int) mouseY - topPos;
        if (button == 0 && x >= LIST_X && x < LIST_X + LIST_WIDTH && y >= LIST_Y + 2 && y < LIST_Y + 82) {
            search.setFocused(false); setFocused(null);
            if (x >= LIST_X + LIST_WIDTH - 6 && visible.size() > VISIBLE_ROWS) { dragging = true; dragScroll(y); return true; }
            int index = scroll + (y - LIST_Y - 2) / ROW_HEIGHT;
            if (index < visible.size()) {
                Row next = visible.get(index);
                if (!Objects.equals(selected, next.entry().id())) quantity = 1;
                selection = next; selected = selection.entry().id(); costScroll = 0; errorTicks = 0; refreshPreview();
            }
            return true;
        }
        Button target = button != 0 ? null : increase.active && increase.isMouseOver(mouseX, mouseY) ? increase
                : decrease.active && decrease.isMouseOver(mouseX, mouseY) ? decrease : null;
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        // The native button performs the immediate increment exactly once.
        if (handled && target != null && target.active) {
            heldQuantityButton = target;
            quantityRepeat.press(target == increase ? 1 : -1, Util.getMillis());
        }
        return handled;
    }

    private void dragScroll(double y) {
        scroll = Math.max(0, Math.min(visible.size() - VISIBLE_ROWS, (int) Math.round((y - LIST_Y - 2) / 80 * (visible.size() - VISIBLE_ROWS))));
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (heldQuantityButton != null && !heldQuantityButton.isMouseOver(x, y)) stopQuantityRepeat();
        if (dragging) { dragScroll(y - topPos); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public void mouseMoved(double x, double y) {
        if (heldQuantityButton != null && !heldQuantityButton.isMouseOver(x, y)) stopQuantityRepeat();
        super.mouseMoved(x, y);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        stopQuantityRepeat(); dragging = false;
        return super.mouseReleased(x, y, button);
    }
    @Override public void setFocused(GuiEventListener child) {
        if (heldQuantityButton != null && child != heldQuantityButton) stopQuantityRepeat();
        super.setFocused(child);
    }
    @Override public void onClose() { stopQuantityRepeat(); super.onClose(); }
    @Override public void removed() { stopQuantityRepeat(); super.removed(); }
    private boolean scrollAt(double mouseX, double mouseY, double amount) {
        int x = (int) mouseX - leftPos, y = (int) mouseY - topPos;
        int step = amount > 0 ? -1 : amount < 0 ? 1 : 0;
        if (x >= LIST_X && x < LIST_X + LIST_WIDTH && y >= LIST_Y && y < LIST_Y + 84) {
            scroll = Math.max(0, Math.min(Math.max(0, visible.size() - VISIBLE_ROWS), scroll + step)); return true;
        }
        Row row = selectedRow();
        if (row != null && x >= DETAIL_X && x < DETAIL_X + 150 && y >= COST_Y && y < COST_Y + 32) {
            int maximum = Math.max(0, (row.entry().recipe().requirements().size() + 1) / 2 - VISIBLE_COST_ROWS);
            costScroll = Math.max(0, Math.min(maximum, costScroll + step)); return true;
        }
        return false;
    }
    //? if >=1.21 {
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        return scrollAt(x, y, vertical) || super.mouseScrolled(x, y, horizontal, vertical);
    }
    //?} else {
    /*@Override public boolean mouseScrolled(double x, double y, double amount) {
        return scrollAt(x, y, amount) || super.mouseScrolled(x, y, amount);
    }
    *///?}
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (search.isFocused() && key != 256) { search.keyPressed(key, scan, modifiers); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
}
