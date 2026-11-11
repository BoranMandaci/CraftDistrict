package com.craftdistrict.client;

import com.craftdistrict.block.RoomNumberPlaqueBlockEntity;
import com.craftdistrict.menu.RoomNumberMenu;
import com.craftdistrict.network.RoomNumberNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class RoomNumberScreen extends AbstractContainerScreen<RoomNumberMenu> {
    private EditBox number;
    private Button done;
    private Button forward, reverse;
    private String draft;
    private boolean pending;
    private boolean selectedReversed;
    public RoomNumberScreen(RoomNumberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 232; imageHeight = menu.kind() == 0 ? 132 : 166;
        selectedReversed = menu.originalReversed();
    }
    @Override protected void init() {
        super.init();
        number = addRenderableWidget(new EditBox(font, leftPos + 22, topPos + 44, imageWidth - 44, 20,
                menu.text("number")));
        number.setMaxLength(menu.kind() == 0 ? 4 : menu.kind() == 1 ? 5 : 9);
        number.setFilter(value -> RoomNumberPlaqueBlockEntity.draft(menu.kind(), value));
        number.setValue(draft == null ? menu.original() : draft);
        number.setResponder(value -> { draft = value; updateDone(); });
        if (menu.kind() != 0) {
            forward = addRenderableWidget(Button.builder(menu.text("forward"), button -> { selectedReversed = false; updateDone(); }).bounds(leftPos + 22, topPos + 74, 90, 20).build());
            reverse = addRenderableWidget(Button.builder(menu.text("reverse"), button -> { selectedReversed = true; updateDone(); }).bounds(leftPos + 120, topPos + 74, 90, 20).build());
        }
        done = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> submit()).bounds(leftPos + 120, topPos + imageHeight - 34, 90, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(leftPos + 22, topPos + imageHeight - 34, 90, 20).build());
        setInitialFocus(number); updateDone();
    }
    private void updateDone() {
        if (done != null) done.active = !pending && !menu.stale() && RoomNumberPlaqueBlockEntity.valid(menu.kind(), number.getValue());
        if (number != null) number.setEditable(!pending && !menu.stale());
        if (forward != null) {
            forward.active = reverse.active = !pending && !menu.stale();
            forward.setMessage(Component.literal(selectedReversed ? "" : "[ ").append(menu.text("forward")).append(selectedReversed ? "" : " ]"));
            reverse.setMessage(Component.literal(selectedReversed ? "[ " : "").append(menu.text("reverse")).append(selectedReversed ? " ]" : ""));
        }
    }
    private void submit() {
        if (pending || menu.stale() || !RoomNumberPlaqueBlockEntity.valid(menu.kind(), number.getValue())) return;
        pending = true; updateDone(); number.setEditable(false);
        RoomNumberNetworking.request(menu.containerId, menu.session(), number.getValue(), selectedReversed);
    }
    @Override protected void containerTick() { super.containerTick(); if (menu.stale()) pending = false; updateDone(); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 257 || key == 335) { submit(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff40505c);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xff17212a);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 15, 0xffeff5f5);
        Component hint = menu.text(menu.stale() ? "stale" : "hint");
        var lines = font.split(hint, imageWidth - 32);
        for (int i = 0; i < Math.min(2, lines.size()); i++) graphics.drawString(font, lines.get(i), 16, (menu.kind() == 0 ? 73 : 105) + i * 9, menu.stale() ? 0xffffb0a0 : 0xffb3c6cb, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        //? if <1.21 {
        /*renderBackground(graphics);
        *///?}
        super.render(graphics, mouseX, mouseY, delta);
    }
}
