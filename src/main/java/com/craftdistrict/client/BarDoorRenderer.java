package com.craftdistrict.client;

import com.craftdistrict.block.BarDoorBlockEntity;
import com.craftdistrict.registry.CraftDistrictBlocks;
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
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/** Shared item-mesh quads, constant resting poses, and no chunk or blockstate updates per frame. */
public final class BarDoorRenderer implements BlockEntityRenderer<BarDoorBlockEntity> {
    private static final Quaternionf[] FACINGS=new Quaternionf[4];
    private static final Quaternionf POSITIVE=Axis.YP.rotationDegrees(90),NEGATIVE=Axis.YP.rotationDegrees(-90);
    static {for(Direction f:Direction.Plane.HORIZONTAL)FACINGS[f.get2DDataValue()]=Axis.YP.rotationDegrees(180-f.toYRot());}
    private final Quaternionf rotation=new Quaternionf();
    private final List<BakedQuad> left=new ArrayList<>(),right=new ArrayList<>();
    private BakedModel source;
    public BarDoorRenderer(BlockEntityRendererProvider.Context context){ }
    private void refresh() {
        var item=CraftDistrictBlocks.Doors.BAR.get().asItem();
        BakedModel model=Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(item);
        if(source==model)return;
        source=model;left.clear();right.clear();RandomSource random=RandomSource.create(42);
        collect(model.getQuads(null,null,random));
        for(Direction face:Direction.values()){random.setSeed(42);collect(model.getQuads(null,face,random));}
    }
    private void collect(List<BakedQuad> quads) {
        for(BakedQuad quad:quads) {
            int[] vertices=quad.getVertices();int stride=vertices.length/4;
            boolean l=true,r=true;
            for(int i=0;i<4;i++) {
                float x=Float.intBitsToFloat(vertices[i*stride])*16,y=Float.intBitsToFloat(vertices[i*stride+1])*16,
                        z=Float.intBitsToFloat(vertices[i*stride+2])*16;
                boolean leaf=y>=1.5F-1e-4&&y<=15+1e-4&&z>=7.25F-1e-4&&z<=8.75F+1e-4;
                l&=leaf&&x>=1-1e-4&&x<=7.9F+1e-4;r&=leaf&&x>=8.1F-1e-4&&x<=15+1e-4;
            }
            if(l)left.add(quad);else if(r)right.add(quad);
        }
    }
    @Override public void render(BarDoorBlockEntity door,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var level = door.getLevel();
        if (level == null) return;
        refresh();
        float angle = door.renderAngle(level.getGameTime(), partial);
        pose.pushPose();pose.translate(.5,0,.5);
        pose.mulPose(FACINGS[door.getBlockState().getValue(com.craftdistrict.block.BarDoorBlock.FACING).get2DDataValue()]);pose.translate(-.5,0,-.5);
        VertexConsumer consumer=buffers.getBuffer(Sheets.cutoutBlockSheet());
        if(angle==0) {
            pose.translate(0,.5,6.25/16);
            emit(left,pose,consumer,light,overlay);emit(right,pose,consumer,light,overlay);
        } else {
            draw(left,1/16D,-angle,pose,consumer,light,overlay);
            draw(right,15/16D,angle,pose,consumer,light,overlay);
        }
        pose.popPose();
    }
    private void draw(List<BakedQuad> quads,double hinge,float angle,PoseStack pose,VertexConsumer consumer,int light,int overlay) {
        pose.pushPose();pose.translate(hinge,0,14.25/16);
        pose.mulPose(angle==90?POSITIVE:angle==-90?NEGATIVE:rotation.rotationY((float)Math.toRadians(angle)));
        pose.translate(-hinge,.5,(-14.25+6.25)/16);
        emit(quads,pose,consumer,light,overlay);
        pose.popPose();
    }
    private void emit(List<BakedQuad> quads,PoseStack pose,VertexConsumer consumer,int light,int overlay) {
        for(BakedQuad quad:quads)consumer.putBulkData(pose.last(),quad,1,1,1,
                //? if >=1.21 {
                1,
                //?}
                light,overlay);
    }
}
