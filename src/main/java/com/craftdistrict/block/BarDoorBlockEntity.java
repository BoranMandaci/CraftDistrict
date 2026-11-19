package com.craftdistrict.block;

import com.craftdistrict.registry.CraftDistrictDoors;
import net.minecraft.core.BlockPos;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Six-tick transition endpoints only. No block-entity ticker and no per-angle packets. */
public final class BarDoorBlockEntity extends BlockEntity {
    public static final int DURATION=6;
    private float from,to;
    private long started;
    private float clientFrom,clientTo;
    private double clientStarted;
    private boolean clientInitialized;
    private AABB renderBounds;
    public BarDoorBlockEntity(BlockPos pos,BlockState state) {
        super(CraftDistrictDoors.BAR.get(),pos,state);
        from=to=(state.getValue(BarDoorBlock.SWING)-4)*22.5F;
    }
    public static float interpolate(float from,float to,double elapsed) {
        float t=(float)Mth.clamp(elapsed/DURATION,0,1);
        return Mth.lerp(t*t*(3-2*t),from,to);
    }
    public float angle(double time) {
        if(level!=null&&level.isClientSide) {
            if(!clientInitialized){clientFrom=from;clientTo=to;clientStarted=started;clientInitialized=true;}
            if(clientFrom==clientTo)return clientTo;
            if(time-clientStarted>=DURATION)return clientTo;
            return interpolate(clientFrom,clientTo,time-clientStarted);
        }
        if(from==to||time-started>=DURATION)return to;
        return interpolate(from,to,time-started);
    }
    public int remaining(long now){return from==to?0:(int)Math.max(0,Math.min(DURATION,started+DURATION-now));}
    public void begin(float current,float target,long now){from=current;to=target;started=now;setChanged();}
    private void writeAnimation(CompoundTag tag) {
        if(level!=null&&remaining(level.getGameTime())>0) {
            writeEndpoints(tag);
        }
    }
    private void writeEndpoints(CompoundTag tag){tag.putFloat("From",from);tag.putFloat("To",to);tag.putLong("Started",started);}
    //? if >=1.21 {
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);writeAnimation(tag);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);readAnimation(tag);}
    //?} else {
    /*@Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);writeAnimation(tag);}
    @Override public void load(CompoundTag tag){super.load(tag);readAnimation(tag);}
    *///?}
    private void readAnimation(CompoundTag tag) {
        if(tag.contains("Started")) {
            float a=tag.getFloat("From"),b=tag.getFloat("To");
            if(Float.isFinite(a)&&Float.isFinite(b)) {
                long stamp=tag.getLong("Started");
                if(level!=null&&level.isClientSide&&(stamp!=started||b!=to)) {
                    double now=level.getGameTime();
                    // Initial chunk data can arrive mid-swing; retain its authoritative progress.
                    clientFrom=clientInitialized?interpolate(clientFrom,clientTo,now-clientStarted):Mth.clamp(a,-90,90);
                    clientTo=Mth.clamp(b,-90,90);clientStarted=clientInitialized?now:stamp;clientInitialized=true;
                }
                from=Mth.clamp(a,-90,90);to=Mth.clamp(b,-90,90);started=stamp;
            }
        }
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    // Forge/NeoForge's extension uses this for the raised wings; Fabric retains ordinary section culling.
    public AABB getRenderBoundingBox(){
        if(renderBounds==null)renderBounds=new AABB(worldPosition.getX()-.4,worldPosition.getY()+.5,worldPosition.getZ()-.4,
                worldPosition.getX()+1.4,worldPosition.getY()+1.5,worldPosition.getZ()+1.4);
        return renderBounds;
    }
    @Override public CompoundTag getUpdateTag(
            //? if >=1.21 {
            HolderLookup.Provider lookup
            //?}
    ){CompoundTag tag=new CompoundTag();writeEndpoints(tag);return tag;}
}
