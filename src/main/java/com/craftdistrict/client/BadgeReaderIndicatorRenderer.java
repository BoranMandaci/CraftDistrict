package com.craftdistrict.client;

import com.craftdistrict.block.OfficeBadgeReaderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Only the active lens is overlaid, using the vanilla block-entity render pass. */
public final class BadgeReaderIndicatorRenderer implements BlockEntityRenderer<OfficeBadgeReaderBlockEntity> {
    private static final RenderType LENS = RenderType.entityCutout(
            ResourceLocation.tryParse("minecraft:textures/block/white_concrete.png"));
    private static final Quaternionf[] FACINGS = new Quaternionf[4];
    private static final float FRONT_Z = 13.85F / 16;
    private static final float BOTTOM = 8.875F / 16, TOP = 9.125F / 16;
    private static final int VIEW_DISTANCE = 24;

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL)
            FACINGS[facing.get2DDataValue()] = Axis.YP.rotationDegrees(180 - facing.toYRot());
    }

    public BadgeReaderIndicatorRenderer(BlockEntityRendererProvider.Context context) { }

    @Override public int getViewDistance() { return VIEW_DISTANCE; }

    @Override public boolean shouldRender(OfficeBadgeReaderBlockEntity reader, Vec3 camera) {
        var level = reader.getLevel();
        if (level == null || reader.visibleIndicator(level.getGameTime()) == 0) return false;
        var pos = reader.getBlockPos();
        var facing = reader.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        double x = pos.getX() + .5, z = pos.getZ() + .5;
        return (camera.x - x) * facing.getStepX() + (camera.z - z) * facing.getStepZ() > .5F - FRONT_Z
                && camera.distanceToSqr(x, pos.getY() + .5, z) < VIEW_DISTANCE * VIEW_DISTANCE;
    }

    @Override public void render(OfficeBadgeReaderBlockEntity reader, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        var level = reader.getLevel();
        if (level == null) return;
        int indicator = reader.visibleIndicator(level.getGameTime());
        if (indicator == 0) return;
        var facing = reader.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        float left = (indicator == 1 ? 6.5F : 8.75F) / 16;
        float right = left + .75F / 16;
        int color = indicator == 1 ? 0xff9affab : 0xffff817c;
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(FACINGS[facing.get2DDataValue()]);
        pose.translate(-.5, 0, -.5);
        var surface = pose.last();
        VertexConsumer buffer = buffers.getBuffer(LENS);
        vertex(buffer, surface, right, TOP, color, overlay);
        vertex(buffer, surface, right, BOTTOM, color, overlay);
        vertex(buffer, surface, left, BOTTOM, color, overlay);
        vertex(buffer, surface, left, TOP, color, overlay);
        pose.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int color, int overlay) {
        //? if >=1.21 {
        buffer.addVertex(pose, x, y, FRONT_Z).setColor(color).setUv(.5F, .5F)
                .setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, -1);
        //?} else {
        /*buffer.vertex(pose.pose(), x, y, FRONT_Z).color(color).uv(.5F, .5F)
                .overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0, 0, -1).endVertex();
        *///?}
    }
}
