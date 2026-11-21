package com.craftdistrict.client;

import com.craftdistrict.block.SlidingDoorBlockEntity;
import com.craftdistrict.block.SlidingEntranceBlock;
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
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/** Cache the existing baked models: fixed frames stay still and only the leaves translate. No ticker or frame packets. */
public final class SlidingDoorRenderer implements BlockEntityRenderer<SlidingDoorBlockEntity> {
    private static final Quaternionf[] FACINGS=new Quaternionf[4];
    static {for(Direction f:Direction.Plane.HORIZONTAL)FACINGS[f.get2DDataValue()]=Axis.YP.rotationDegrees(180-f.toYRot());}
    private final Mesh[] meshes=new Mesh[8];
    private final DoorRenderSupport quadEmitter = new DoorRenderSupport();
    private static final class Mesh {
        final BakedModel lower,upper;
        final List<BakedQuad> lowerFrame=new ArrayList<>(),upperFrame=new ArrayList<>(),lowerLeaf=new ArrayList<>(),upperLeaf=new ArrayList<>();
        Mesh(BakedModel lower,BakedModel upper){this.lower=lower;this.upper=upper;collect(lower,false,lowerFrame,lowerLeaf);collect(upper,true,upperFrame,upperLeaf);}
    }
    public SlidingDoorRenderer(BlockEntityRendererProvider.Context context){ }
    @Override public boolean shouldRenderOffScreen(SlidingDoorBlockEntity door) {
        // The upper half or translated leaf can be visible across a section boundary on Fabric too.
        var pos=door.getBlockPos();int x=pos.getX()&15,z=pos.getZ()&15;
        return (pos.getY()&15)==15||x==0||x==15||z==0||z==15;
    }
    @Override public boolean shouldRender(SlidingDoorBlockEntity door, Vec3 cameraPos) {
        var pos = door.getBlockPos();
        int distance = getViewDistance();
        return cameraPos.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) < (double) distance * distance
                && DoorRenderSupport.inFrustum(door.getRenderBoundingBox());
    }
    private Mesh mesh(BlockState state) {
        var shaper=Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        BlockState closed=state.setValue(SlidingEntranceBlock.FACING,Direction.NORTH).setValue(SlidingEntranceBlock.OPEN,false);
        BakedModel lower=shaper.getBlockModel(closed.setValue(SlidingEntranceBlock.HALF,DoubleBlockHalf.LOWER));
        BakedModel upper=shaper.getBlockModel(closed.setValue(SlidingEntranceBlock.HALF,DoubleBlockHalf.UPPER));
        int index=((SlidingEntranceBlock)state.getBlock()).design().ordinal()*4+(state.getValue(SlidingEntranceBlock.PAIRED)?2:0)
                +(state.getValue(SlidingEntranceBlock.HINGE)==DoorHingeSide.RIGHT?1:0);
        Mesh mesh=meshes[index];
        if(mesh==null||mesh.lower!=lower||mesh.upper!=upper)meshes[index]=mesh=new Mesh(lower,upper);
        return mesh;
    }
    private static void collect(BakedModel model,boolean upper,List<BakedQuad> frame,List<BakedQuad> leaf) {
        RandomSource random=RandomSource.create(42);
        split(model.getQuads(null,null,random),upper,frame,leaf);
        for(Direction face:Direction.values()){random.setSeed(42);split(model.getQuads(null,face,random),upper,frame,leaf);}
    }
    private static void split(List<BakedQuad> quads,boolean upper,List<BakedQuad> frame,List<BakedQuad> leaf) {
        for(BakedQuad quad:quads) {
            int[] vertices=quad.getVertices();int stride=vertices.length/4;
            float minY=Float.POSITIVE_INFINITY,minZ=minY,maxZ=Float.NEGATIVE_INFINITY;
            for(int i=0;i<4;i++) {
                minY=Math.min(minY,Float.intBitsToFloat(vertices[i*stride+1])*16);
                float z=Float.intBitsToFloat(vertices[i*stride+2])*16;minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
            }
            // Closed source models: jambs occupy z=0..1.75; the track/sensors are above the leaf.
            boolean fixed=upper&&minY>=14.25F-1e-4||Math.abs(minZ)<1e-4||Math.abs(maxZ-1.75F)<1e-4;
            (fixed?frame:leaf).add(quad);
        }
    }
    @Override public void render(SlidingDoorBlockEntity door,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var level = door.getLevel();
        if (level == null) return;
        BlockState state=door.getBlockState();Mesh mesh=mesh(state);
        float progress = door.progress(level.getGameTime(), partial);
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(FACINGS[state.getValue(SlidingEntranceBlock.FACING).get2DDataValue()]);pose.translate(-.5,0,-.5);
        VertexConsumer consumer=buffers.getBuffer(Sheets.cutoutBlockSheet());
        quadEmitter.setLight(light);
        draw(mesh.lowerFrame,mesh.upperFrame,pose,consumer,overlay);
        if (progress != 0)
            pose.translate((state.getValue(SlidingEntranceBlock.HINGE)==DoorHingeSide.RIGHT?1:-1)*progress,0,-1.75/16*progress);
        draw(mesh.lowerLeaf,mesh.upperLeaf,pose,consumer,overlay);
        pose.popPose();
    }
    private void draw(List<BakedQuad> lower,List<BakedQuad> upper,PoseStack pose,VertexConsumer consumer,int overlay) {
        quadEmitter.emit(lower, pose.last(), consumer, overlay);
        pose.translate(0, 1, 0);
        quadEmitter.emit(upper, pose.last(), consumer, overlay);
        pose.translate(0, -1, 0);
    }
}
