package com.craftdistrict.client;

import com.craftdistrict.block.RotaryEntranceBlock;
import com.craftdistrict.block.RotaryEntranceBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/** One cached baked mesh is shared by the three moving wings. */
public final class RotaryEntranceRenderer implements BlockEntityRenderer<RotaryEntranceBlockEntity> {
    private static final Quaternionf[] FACINGS = new Quaternionf[4];
    private static final Quaternionf BLADE_SPACING = Axis.YP.rotationDegrees(120);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL)
            FACINGS[facing.get2DDataValue()] = Axis.YP.rotationDegrees(180 - facing.toYRot());
    }

    private record Mesh(BakedModel source, List<BakedQuad> quads) { }

    private Mesh mesh;
    private final Quaternionf rotation = new Quaternionf();
    private final DoorRenderSupport quadEmitter = new DoorRenderSupport();
    private long rotationTick;
    private float rotationPartial = Float.NaN;

    public RotaryEntranceRenderer(BlockEntityRendererProvider.Context context) { }

    private List<BakedQuad> quads(BlockState state) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        if (mesh == null || mesh.source() != model) {
            RandomSource random = RandomSource.create(42);
            List<BakedQuad> quads = new ArrayList<>(model.getQuads(state, null, random));
            for (Direction face : Direction.values()) {
                random.setSeed(42);
                quads.addAll(model.getQuads(state, face, random));
            }
            mesh = new Mesh(model, quads);
        }
        return mesh.quads();
    }

    @Override public boolean shouldRenderOffScreen(RotaryEntranceBlockEntity entrance) {
        // Keep the controller eligible when a neighboring section can see its wings.
        var bounds = entrance.getRenderBoundingBox();
        return differentSections(bounds.minX, bounds.maxX) || differentSections(bounds.minY, bounds.maxY)
                || differentSections(bounds.minZ, bounds.maxZ);
    }

    @Override public boolean shouldRender(RotaryEntranceBlockEntity entrance, Vec3 cameraPos) {
        var bounds = entrance.getRenderBoundingBox();
        int distance = getViewDistance();
        return bounds.distanceToSqr(cameraPos) < (double) distance * distance && DoorRenderSupport.inFrustum(bounds);
    }

    private static boolean differentSections(double min, double max) {
        return (int) Math.floor(min / 16) != (int) Math.floor(Math.nextDown(max) / 16);
    }

    @Override
    public void render(RotaryEntranceBlockEntity entrance, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var level = entrance.getLevel();
        if (level == null) return;
        BlockState state = entrance.getBlockState();
        RotaryEntranceBlock block = (RotaryEntranceBlock) state.getBlock();
        List<BakedQuad> wings = quads(block.rotorModelState());
        long time = level.getGameTime();
        if (time != rotationTick || partialTick != rotationPartial) {
            float angle = RotaryEntranceBlockEntity.revolvingAngle(time, partialTick);
            rotation.rotationY((float) Math.toRadians(angle));
            rotationTick = time;
            rotationPartial = partialTick;
        }
        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        quadEmitter.setLight(light);
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(FACINGS[state.getValue(RotaryEntranceBlock.FACING).get2DDataValue()]);
        pose.mulPose(rotation);
        pose.translate(-.5, 0, -.5);
        quadEmitter.emit(wings, pose.last(), buffer, overlay);
        for (int blade = 1; blade < 3; blade++) {
            pose.translate(.5, 0, .5);
            pose.mulPose(BLADE_SPACING);
            pose.translate(-.5, 0, -.5);
            quadEmitter.emit(wings, pose.last(), buffer, overlay);
        }
        pose.popPose();
    }

}
