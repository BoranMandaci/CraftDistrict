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

/** One unticked record per two-block door. Paired leaves share the same transition endpoints and clock. */
public final class SlidingDoorBlockEntity extends BlockEntity {
    public static final int DURATION=12;
    private float from,to;
    private long started;
    private AABB renderBounds;

    public SlidingDoorBlockEntity(BlockPos pos,BlockState state) {
        super(CraftDistrictDoors.SLIDING.get(),pos,state);
        from=to=state.getValue(SlidingEntranceBlock.OPEN)?1:0;
    }
    public static float interpolate(float from,float to,double elapsed) {
        float t=(float)Mth.clamp(elapsed/DURATION,0,1);
        return Mth.lerp(t*t*(3-2*t),from,to);
    }
    public float progress(double time){return from==to||time-started>=DURATION?to:interpolate(from,to,time-started);}
    public boolean moving(long now){return from!=to&&now<started+DURATION;}
    public boolean recent(long now){return from!=to&&now-started>=0&&now-started<=DURATION+10;}
    public boolean transition(boolean open,long now) {
        float target=open?1:0;
        return to!=target&&set(progress(now),target,now);
    }
    public boolean copy(SlidingDoorBlockEntity other){return set(other.from,other.to,other.started);}
    private boolean set(float a,float b,long time) {
        if(from==a&&to==b&&started==time)return false;
        from=a;to=b;started=time;setChanged();return true;
    }
    private void write(CompoundTag tag){tag.putFloat("From",from);tag.putFloat("To",to);tag.putLong("Started",started);}
    private void saveMotion(CompoundTag tag){if(level!=null&&moving(level.getGameTime()))write(tag);}
    private void read(CompoundTag tag) {
        if(!tag.contains("Started"))return;
        float a=tag.getFloat("From"),b=tag.getFloat("To");
        if(Float.isFinite(a)&&Float.isFinite(b)){from=Mth.clamp(a,0,1);to=Mth.clamp(b,0,1);started=tag.getLong("Started");}
    }
    //? if >=1.21 {
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);saveMotion(tag);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);read(tag);}
    //?} else {
    /*@Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);saveMotion(tag);}
    @Override public void load(CompoundTag tag){super.load(tag);read(tag);}
    *///?}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(
            //? if >=1.21 {
            HolderLookup.Provider lookup
            //?}
    ){CompoundTag tag=new CompoundTag();write(tag);return tag;}
    // Forge/NeoForge's culling extension; the same two-high model also renders on Fabric.
    public AABB getRenderBoundingBox() {
        if(renderBounds==null)renderBounds=new AABB(worldPosition.getX()-1,worldPosition.getY(),worldPosition.getZ()-1,
                worldPosition.getX()+2,worldPosition.getY()+2,worldPosition.getZ()+2);
        return renderBounds;
    }
}
