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
        // The controller's own section can be out of view while a blade in the next section is visible.
        var bounds = entrance.getRenderBoundingBox();
        return differentSections(bounds.minX, bounds.maxX) || differentSections(bounds.minY, bounds.maxY)
                || differentSections(bounds.minZ, bounds.maxZ);
    }

    private static boolean differentSections(double min, double max) {
        return (int) Math.floor(min / 16) != (int) Math.floor(Math.nextDown(max) / 16);
    }

    @Override
    public void render(RotaryEntranceBlockEntity entrance, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (entrance.getLevel() == null) return;
        BlockState state = entrance.getBlockState();
        RotaryEntranceBlock block = (RotaryEntranceBlock) state.getBlock();
        List<BakedQuad> wings = quads(block.rotorModelState());
        float angle = RotaryEntranceBlockEntity.revolvingAngle(entrance.getLevel().getGameTime(), partialTick);
        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(FACINGS[state.getValue(RotaryEntranceBlock.FACING).get2DDataValue()]);
        pose.mulPose(rotation.rotationY((float) Math.toRadians(angle)));
        pose.translate(-.5, 0, -.5);
        emit(wings, pose, buffer, light, overlay);
        for (int blade = 1; blade < 3; blade++) {
            pose.translate(.5, 0, .5);
            pose.mulPose(BLADE_SPACING);
            pose.translate(-.5, 0, -.5);
            emit(wings, pose, buffer, light, overlay);
        }
        pose.popPose();
    }

    private static void emit(List<BakedQuad> quads, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        for (int i = 0; i < quads.size(); i++) {
            buffer.putBulkData(pose.last(), quads.get(i), 1, 1, 1,
                    //? if >=1.21 {
                    1,
                    //?}
                    light, overlay);
        }
    }
}
