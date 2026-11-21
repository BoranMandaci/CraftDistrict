package com.craftdistrict.door;

import com.craftdistrict.access.AccessDoorSecurity;
import com.craftdistrict.block.SlidingEntranceBlock;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Map;
import java.util.WeakHashMap;

/** Loaded-chunk, four-block spatial cells. Players visit nearby automatic entrances at 5 Hz. */
public final class SlidingDoorSensors {
    private static final Map<ServerLevel,Index> LEVELS=new WeakHashMap<>();
    private static final int INTERVAL=4,CLOSE_DELAY=20;
    private static final class Index {
        final Long2ObjectOpenHashMap<LongOpenHashSet> cells=new Long2ObjectOpenHashMap<>(),chunks=new Long2ObjectOpenHashMap<>();
        final LongOpenHashSet loaded=new LongOpenHashSet(),seen=new LongOpenHashSet(),dirty=new LongOpenHashSet();
        final LongOpenHashSet closing=new LongOpenHashSet();
        final Long2LongOpenHashMap lastSeen=new Long2LongOpenHashMap(),pending=new Long2LongOpenHashMap();
        boolean processing;
    }
    private SlidingDoorSensors(){ }
    private static long cell(BlockPos p){return BlockPos.asLong(p.getX()>>2,p.getY()>>2,p.getZ()>>2);}
    private static long chunk(BlockPos p){return ChunkPos.asLong(p.getX()>>4,p.getZ()>>4);}
    private static boolean automatic(BlockState s){return s.getBlock() instanceof SlidingEntranceBlock&&s.getValue(SlidingEntranceBlock.AUTOMATIC)&&s.getValue(SlidingEntranceBlock.HALF)==DoubleBlockHalf.LOWER;}
    private static void discard(Long2ObjectOpenHashMap<LongOpenHashSet> map,long key,long value) {
        LongOpenHashSet values=map.get(key);if(values!=null){values.remove(value);if(values.isEmpty())map.remove(key);}
    }
    private static void remove(Index data,BlockPos pos) {
        discard(data.cells,cell(pos),pos.asLong());discard(data.chunks,chunk(pos),pos.asLong());data.lastSeen.remove(pos.asLong());
    }
    public static void changed(ServerLevel level,BlockPos pos) {
        Index data=LEVELS.get(level);
        if(data==null&&!automatic(level.getBlockState(pos)))return;
        if(data==null){data=new Index();LEVELS.put(level,data);}
        if(data.processing){data.dirty.add(pos.asLong());return;}
        BlockState state = level.getBlockState(pos);
        if(automatic(state) && !AccessDoorSecurity.secured(level, pos, state)) {
            data.cells.computeIfAbsent(cell(pos),ignored->new LongOpenHashSet()).add(pos.asLong());
            data.chunks.computeIfAbsent(chunk(pos),ignored->new LongOpenHashSet()).add(pos.asLong());
            if(!state.getValue(SlidingEntranceBlock.OPEN))data.lastSeen.remove(pos.asLong());
        } else remove(data,pos);
    }
    public static void removed(ServerLevel level,BlockPos pos) {
        Index data=LEVELS.get(level);if(data==null)return;
        data.closing.remove(pos.asLong());
        if(data.processing)data.dirty.add(pos.asLong());else remove(data,pos);
    }
    public static void unload(ServerLevel level,long key) {
        Index data=LEVELS.get(level);if(data==null)return;
        LongOpenHashSet positions=data.chunks.remove(key);
        if(positions!=null)for(long p:positions){BlockPos pos=BlockPos.of(p);discard(data.cells,cell(pos),p);data.lastSeen.remove(p);}
        data.loaded.remove(key);data.pending.remove(key);
        var closing=data.closing.iterator();while(closing.hasNext())if(chunk(BlockPos.of(closing.nextLong()))==key)closing.remove();
    }
    public static void watchClosing(ServerLevel level,BlockPos pos) {
        BlockState state=level.getBlockState(pos);
        if (!(state.getBlock() instanceof SlidingEntranceBlock door)) return;
        BlockPos root = door.completeController(level, pos, state);
        if (root != null) LEVELS.computeIfAbsent(level, ignored -> new Index()).closing.add(root.asLong());
    }
    private static boolean discover(ServerLevel level,Index data,int x,int z) {
        long key=ChunkPos.asLong(x,z);if(data.loaded.contains(key))return true;
        var loaded=level.getChunkSource().getChunkNow(x,z);if(loaded==null)return false;
        data.loaded.add(key);
        loaded.findBlocks(SlidingDoorSensors::automatic,(pos,state)-> {
            changed(level,pos);
            if(state.getValue(SlidingEntranceBlock.OPEN)) {
                SlidingEntranceBlock door=(SlidingEntranceBlock)state.getBlock();
                BlockPos root = door.completeController(level, pos, state);
                if (root != null) arm(level, root);
            }
        });
        return true;
    }
    public static void chunkSent(ServerPlayer player,int x,int z) {
        ServerLevel level=player.serverLevel();
        if(!level.getServer().isSameThread()){level.getServer().execute(()->chunkSent(player,x,z));return;}
        Index data=LEVELS.computeIfAbsent(level,ignored->new Index());
        if(!discover(level,data,x,z))data.pending.put(ChunkPos.asLong(x,z),level.getGameTime()+100);
    }
    public static void arm(ServerLevel level,BlockPos root) {
        BlockState s=level.getBlockState(root);
        if(!automatic(s)||!s.getValue(SlidingEntranceBlock.OPEN)||AccessDoorSecurity.secured(level,root,s))return;
        Index data=LEVELS.computeIfAbsent(level,ignored->new Index());
        if(s.getValue(SlidingEntranceBlock.POWERED)){data.lastSeen.remove(root.asLong());return;}
        data.lastSeen.putIfAbsent(root.asLong(),level.getGameTime());
        if(!level.getBlockTicks().hasScheduledTick(root,s.getBlock()))level.scheduleTick(root.immutable(),s.getBlock(),CLOSE_DELAY);
    }
    public static void closeTick(ServerLevel level,SlidingEntranceBlock door,BlockPos pos) {
        BlockState s=level.getBlockState(pos);
        if(!automatic(s)||!s.getValue(SlidingEntranceBlock.OPEN)||s.getValue(SlidingEntranceBlock.POWERED)
                ||AccessDoorSecurity.secured(level,pos,s))return;
        BlockPos root = door.completeController(level, pos, s);
        if (root == null) return; // Resume on discovery or neighbor repair.
        if(!root.equals(pos)){arm(level,root);return;}
        Index data=LEVELS.computeIfAbsent(level,ignored->new Index());long now=level.getGameTime();
        if(door.occupied(level,root))data.lastSeen.put(root.asLong(),now);
        long remaining=CLOSE_DELAY-(now-data.lastSeen.getOrDefault(root.asLong(),now-CLOSE_DELAY));
        if(remaining>0){level.scheduleTick(root.immutable(),door,(int)remaining);return;}
        door.setOpen(null,level,s,root,false);data.lastSeen.remove(root.asLong());
    }
    public static void register() {
        TickEvent.SERVER_LEVEL_POST.register(SlidingDoorSensors::tick);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(level->LEVELS.remove(level));
    }
    private static void tick(ServerLevel level) {
        Index data=LEVELS.get(level);if(data==null)return;
        if(level.getGameTime()%2==0&&!data.closing.isEmpty()) {
            var closing=data.closing.iterator();
            while(closing.hasNext()) {
                BlockPos pos=BlockPos.of(closing.nextLong());
                if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null
                        ||!(level.getBlockState(pos).getBlock() instanceof SlidingEntranceBlock door)||!door.guardClosing(level,pos))closing.remove();
            }
        }
        if(level.getGameTime()%INTERVAL!=0)return;
        var pending=data.pending.long2LongEntrySet().fastIterator();
        while(pending.hasNext()) {
            var entry=pending.next();long key=entry.getLongKey();
            if(discover(level,data,ChunkPos.getX(key),ChunkPos.getZ(key))||level.getGameTime()>entry.getLongValue())pending.remove();
        }
        if(data.cells.isEmpty()||level.players().isEmpty())return;
        data.seen.clear();data.processing=true;
        try {
            for(ServerPlayer player:level.players()) {
                if(player.isSpectator()||!player.isAlive())continue;
                int minX = Mth.floor(player.getX() - 4) >> 2, maxX = Mth.floor(player.getX() + 4) >> 2;
                int minY = Mth.floor(player.getY() - 3) >> 2, maxY = Mth.floor(player.getY() + 2) >> 2;
                int minZ = Mth.floor(player.getZ() - 4) >> 2, maxZ = Mth.floor(player.getZ() + 4) >> 2;
                for (int x = minX; x <= maxX; x++)
                    for (int y = minY; y <= maxY; y++)
                        for (int z = minZ; z <= maxZ; z++) {
                            LongOpenHashSet nodes=data.cells.get(BlockPos.asLong(x,y,z));if(nodes==null)continue;
                            for(long p:nodes) {
                                BlockPos pos=BlockPos.of(p);
                                if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null)continue;
                                BlockState state=level.getBlockState(pos);if(!automatic(state)||AccessDoorSecurity.secured(level,pos,state))continue;
                                SlidingEntranceBlock door=(SlidingEntranceBlock)state.getBlock();
                                BlockPos root = door.completeController(level, pos, state);
                                if (root == null || data.seen.contains(root.asLong())
                                        || !door.near(level, root, player, state.getValue(SlidingEntranceBlock.OPEN) ? 3 : 2.5)) continue;
                                data.seen.add(root.asLong());data.lastSeen.put(root.asLong(),level.getGameTime());
                                if(!state.getValue(SlidingEntranceBlock.OPEN))door.setOpen(null,level,state,root,true);
                                arm(level,root);
                            }
                        }
            }
        } finally {
            data.processing=false;
            if(!data.dirty.isEmpty()){long[] dirty=data.dirty.toLongArray();data.dirty.clear();for(long p:dirty)changed(level,BlockPos.of(p));}
        }
    }
}
