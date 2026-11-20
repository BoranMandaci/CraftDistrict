package com.craftdistrict.client;

import com.craftdistrict.block.OfficeTurnstileBlock;
import com.craftdistrict.block.OfficeTurnstileBlockEntity;
import com.craftdistrict.registry.CraftDistrictBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/** The static pedestal stays in the chunk mesh. The inventory model supplies one reusable arm. */
public final class OfficeTurnstileRenderer implements BlockEntityRenderer<OfficeTurnstileBlockEntity> {
    // The inventory model has a uniform authoring offset relative to the placed hub.
    private static final double ITEM_HUB_X = 7.125 / 16, ITEM_HUB_Y = 9.75 / 16, ITEM_HUB_Z = 7.921573 / 16;
    private static final Quaternionf[] FACINGS = new Quaternionf[4];
    private static final Quaternionf ARM_SPACING = axleRotation(120);
    private static final Quaternionf FORWARD_HALF = axleRotation(60), BACKWARD_HALF = axleRotation(-60);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL)
            FACINGS[facing.get2DDataValue()] = Axis.YP.rotationDegrees(180 - facing.toYRot());
    }

    private BakedModel source;
    private final List<BakedQuad> arm = new ArrayList<>(5);
    private final Quaternionf rotation = new Quaternionf();

    public OfficeTurnstileRenderer(BlockEntityRendererProvider.Context context) { }

    private static Quaternionf axleRotation(float degrees) {
        return new Quaternionf().rotationAxis(degrees * Mth.DEG_TO_RAD, OfficeTurnstileBlock.AXLE_X, OfficeTurnstileBlock.AXLE_Y, 0);
    }

    private void refreshArm() {
        var item = CraftDistrictBlocks.Office.SECURITY_TURNSTILE.get().asItem();
        BakedModel model = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(item);
        if (source == model) return;
        source = model;
        arm.clear();
        RandomSource random = RandomSource.create(42);
        collectArm(model.getQuads(null, null, random));
        for (Direction direction : Direction.values()) {
            random.setSeed(42);
            collectArm(model.getQuads(null, direction, random));
        }
    }

    private void collectArm(List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            boolean inArm = true, reachesTip = false;
            for (int i = 0; i < 4; i++) {
                int offset = i * stride;
                float x = Float.intBitsToFloat(vertices[offset]) * 16;
                float y = Float.intBitsToFloat(vertices[offset + 1]) * 16;
                float z = Float.intBitsToFloat(vertices[offset + 2]) * 16;
                inArm &= x >= 7.5 - 1e-4 && x <= 14.75 + 1e-4 && y >= 9.375 - 1e-4 && y <= 10.125 + 1e-4
                        && z >= 7.546573 - 1e-4 && z <= 8.296573 + 1e-4;
                reachesTip |= x >= 14.75 - 1e-4;
            }
            if (inArm && reachesTip) arm.add(quad);
        }
    }

    @Override public boolean shouldRender(OfficeTurnstileBlockEntity gate, Vec3 camera) {
        AABB bounds = gate.getRenderBoundingBox();
        int distance = getViewDistance();
        return bounds.distanceToSqr(camera) < (double) distance * distance && DoorRenderSupport.inFrustum(bounds);
    }

    @Override public void render(OfficeTurnstileBlockEntity gate, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        var level = gate.getLevel();
        if (level == null) return;
        refreshArm();
        float angle = gate.angle(level.getGameTime(), partialTick);
        var consumer = buffers.getBuffer(Sheets.solidBlockSheet());
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(FACINGS[gate.getBlockState().getValue(OfficeTurnstileBlock.FACING).get2DDataValue()]);
        pose.translate(OfficeTurnstileBlock.HUB_X - .5, OfficeTurnstileBlock.HUB_Y, OfficeTurnstileBlock.HUB_Z - .5);
        if (angle != 0) {
            pose.mulPose(angle == 60 ? FORWARD_HALF : angle == -60 ? BACKWARD_HALF
                    : rotation.rotationAxis(angle * Mth.DEG_TO_RAD, OfficeTurnstileBlock.AXLE_X, OfficeTurnstileBlock.AXLE_Y, 0));
        }
        pose.translate(-ITEM_HUB_X, -ITEM_HUB_Y, -ITEM_HUB_Z);
        DoorRenderSupport.emit(arm, pose.last(), consumer, light, overlay);
        for (int wing = 1; wing < 3; wing++) {
            pose.translate(ITEM_HUB_X, ITEM_HUB_Y, ITEM_HUB_Z);
            pose.mulPose(ARM_SPACING);
            pose.translate(-ITEM_HUB_X, -ITEM_HUB_Y, -ITEM_HUB_Z);
            DoorRenderSupport.emit(arm, pose.last(), consumer, light, overlay);
        }
        pose.popPose();
    }
}
