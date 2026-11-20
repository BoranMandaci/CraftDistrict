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
    private long clientStarted;
    private float clientStartPartial;
    private long lastRenderTick = Long.MIN_VALUE;
    private float lastRenderPartial;
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
    public float angle(long tick) {
        return angle(tick, 0, false);
    }
    public float renderAngle(long tick, float partialTick) {
        return angle(tick, partialTick, true);
    }
    private float angle(long tick, float partialTick, boolean rendering) {
        if (level != null && level.isClientSide) {
            if (!clientInitialized) {
                clientFrom = from;
                clientTo = to;
                clientStarted = started;
                clientInitialized = true;
            }
            if (clientFrom == clientTo) return clientTo;
            double elapsed = clientElapsed(tick, partialTick);
            if (rendering && (elapsed < DURATION || lastRenderTick == tick)) {
                lastRenderTick = tick;
                lastRenderPartial = partialTick;
            }
            return elapsed >= DURATION ? clientTo : interpolate(clientFrom, clientTo, elapsed);
        }
        long elapsed = tick - started;
        return from == to || elapsed >= DURATION ? to : interpolate(from, to, elapsed + (double) partialTick);
    }
    private double clientElapsed(long tick, float partialTick) {
        return (tick - clientStarted) + (double) partialTick - clientStartPartial;
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
            if (Float.isFinite(a) && Float.isFinite(b)) {
                a = Mth.clamp(a, -90, 90);
                b = Mth.clamp(b, -90, 90);
                long stamp = tag.getLong("Started");
                if (level != null && level.isClientSide && (stamp != started || b != to)) {
                    if (clientInitialized) {
                        long tick = level.getGameTime();
                        // A packet can reverse a swing after this tick's interpolated pose has already been drawn.
                        float partial = lastRenderTick == tick ? lastRenderPartial : 0;
                        clientFrom = interpolate(clientFrom, clientTo, clientElapsed(tick, partial));
                        clientStarted = tick;
                        clientStartPartial = partial;
                    } else {
                        // Initial chunk data retains the server's progress even when it arrives mid-swing.
                        clientFrom = a;
                        clientStarted = stamp;
                        clientStartPartial = 0;
                    }
                    clientTo = b;
                    clientInitialized = true;
                }
                from = a;
                to = b;
                started = stamp;
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
