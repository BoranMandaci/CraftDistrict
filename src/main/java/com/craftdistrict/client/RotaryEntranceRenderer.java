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

    private List<BakedQuad> wings;
    private final Quaternionf rotation = new Quaternionf();
    private final DoorRenderSupport quadEmitter = new DoorRenderSupport();
    private float cachedAngle = Float.NaN;
    private int cachedFacing = -1;

    public RotaryEntranceRenderer(BlockEntityRendererProvider.Context context) { }

    private List<BakedQuad> quads(BlockState state) {
        if (quadEmitter.modelsChanged() || wings == null) {
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
            RandomSource random = RandomSource.create(42);
            List<BakedQuad> quads = new ArrayList<>(model.getQuads(state, null, random));
            for (Direction face : Direction.values()) {
                random.setSeed(42);
                quads.addAll(model.getQuads(state, face, random));
            }
            wings = quads;
        }
        return wings;
    }

    @Override public boolean shouldRenderOffScreen(RotaryEntranceBlockEntity entrance) {
        // Keep the controller eligible when a neighboring section can see its wings.
        var pos = entrance.getBlockPos();
        return atSectionEdge(pos.getX()) || atSectionEdge(pos.getY()) || atSectionEdge(pos.getZ());
    }

    @Override public boolean shouldRender(RotaryEntranceBlockEntity entrance, Vec3 cameraPos) {
        var bounds = entrance.getRenderBoundingBox();
        int distance = getViewDistance();
        return bounds.distanceToSqr(cameraPos) < (double) distance * distance && DoorRenderSupport.inFrustum(bounds);
    }

    private static boolean atSectionEdge(int coordinate) {
        int local = coordinate & 15;
        return local == 0 || local == 15;
    }

    @Override
    public void render(RotaryEntranceBlockEntity entrance, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var level = entrance.getLevel();
        if (level == null) return;
        BlockState state = entrance.getBlockState();
        RotaryEntranceBlock block = (RotaryEntranceBlock) state.getBlock();
        List<BakedQuad> wings = quads(block.rotorModelState());
        float angle = entrance.renderAngle(level.getGameTime(), partialTick);
        int facing = state.getValue(RotaryEntranceBlock.FACING).get2DDataValue();
        if (angle != cachedAngle || facing != cachedFacing) {
            rotation.rotationY((float) Math.toRadians(angle)).premul(FACINGS[facing]);
            cachedAngle = angle;
            cachedFacing = facing;
        }
        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        quadEmitter.setLight(light);
        pose.pushPose();
        pose.rotateAround(rotation, .5F, 0, .5F);
        quadEmitter.emit(wings, pose.last(), buffer, overlay);
        for (int blade = 1; blade < 3; blade++) {
            pose.rotateAround(BLADE_SPACING, .5F, 0, .5F);
            quadEmitter.emit(wings, pose.last(), buffer, overlay);
        }
        pose.popPose();
    }

}
