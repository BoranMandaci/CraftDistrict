package com.craftdistrict.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

class AccessControlButton extends Button {
    AccessControlButton(int x, int y, int width, int height, Component label, OnPress press) {
        super(x, y, width, height, label, press, DEFAULT_NARRATION);
    }

    @Override protected void renderWidget(GuiGraphics graphics, int x, int y, float tick) {
        int edge = active && isHoveredOrFocused() ? 0xff79d6d2 : 0xff435971;
        int fill = active && isHoveredOrFocused() ? 0xff29495b : 0xff1e3046;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, edge);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, fill);
        renderScrollingString(graphics, Minecraft.getInstance().font, 3, active ? 0xffeaf4fe : 0xff91a1b1);
    }
}
