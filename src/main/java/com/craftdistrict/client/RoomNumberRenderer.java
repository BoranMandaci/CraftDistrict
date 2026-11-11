package com.craftdistrict.client;

import com.craftdistrict.block.RoomNumberPlaqueBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Baked sign bodies share a short, naturally lit text layer; no per-frame data or world updates. */
public final class RoomNumberRenderer implements BlockEntityRenderer<RoomNumberPlaqueBlockEntity> {
    private final Font font;
    public RoomNumberRenderer(BlockEntityRendererProvider.Context context) { font = context.getFont(); }
    @Override public void render(RoomNumberPlaqueBlockEntity plaque, float delta, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int kind = plaque.kind();
        pose.pushPose(); pose.translate(.5, .5, .5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - plaque.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
        pose.translate(0, 0, 6.734375 / 16);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        if (kind == 0) draw(pose, buffers, light, plaque.number(), 8, 8, .0175F);
        else if (kind == 1) {
            draw(pose, buffers, light, plaque.number(), 9, 8, .0145F);
            draw(pose, buffers, light, plaque.reversed() ? "\u2193" : "\u2191", 3.75, 8, .016F);
        } else {
            double center = plaque.reversed() ? 8.5 : 7.5;
            draw(pose, buffers, light, "ROOMS", center, 10, .0115F);
            draw(pose, buffers, light, plaque.number(), center, 6.5, .0105F);
            draw(pose, buffers, light, plaque.reversed() ? "\u2190" : "\u2192", plaque.reversed() ? 2.25 : 13.75, 8, .016F);
        }
        pose.popPose();
    }
    private void draw(PoseStack pose, MultiBufferSource buffers, int light, String text, double x, double y, float scale) {
        pose.pushPose(); pose.translate((x - 8) / 16, (y - 8) / 16, 0); pose.scale(scale, -scale, scale);
        font.drawInBatch(text, -font.width(text) / 2F, -font.lineHeight / 2F, 0xffead9b8, false,
                pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
        pose.popPose();
    }
    @Override public int getViewDistance() { return 24; }
}
