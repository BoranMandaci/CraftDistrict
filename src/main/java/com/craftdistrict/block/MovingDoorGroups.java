package com.craftdistrict.block;

import com.craftdistrict.access.AccessControlData;
import com.craftdistrict.access.AccessDoorSecurity;
import com.craftdistrict.access.AccessDoorTarget;
import com.craftdistrict.item.WrenchItem;
import dev.architectury.event.events.common.ChunkEvent;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Event-only ownership for panels parked outside their doorway. No entities or per-tick scans. */
public final class MovingDoorGroups extends SavedData {
    private static final String NAME="craftdistrict_moving_doors";
    private static final int LIMIT=256, SPAN=16;
    private static final ThreadLocal<Boolean> MOVING=ThreadLocal.withInitial(()->false);
    //? if >=1.21 {
    private static final Factory<MovingDoorGroups> FACTORY=new Factory<>(MovingDoorGroups::new,
            (tag,registries)->load(tag),DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    //?}
    private record Cell(BlockPos anchor,BlockPos parked,BlockState closed) { }
    private static final class Group {
        final List<Cell> cells;
        final Map<BlockPos,BlockState> open=new LinkedHashMap<>();
        final Set<BlockPos> removed=new HashSet<>();
        boolean broken;
        Group(List<Cell> cells) {
            this.cells=List.copyOf(cells);
            for(Cell c:cells)open.put(c.anchor,c.closed.setValue(ModularMovingDoorBlock.OPEN,true)
                    .setValue(ModularMovingDoorBlock.ROLE,ModularMovingDoorBlock.Role.ANCHOR));
            for(Cell c:cells)open.put(c.parked,c.closed.setValue(ModularMovingDoorBlock.OPEN,true)
                    .setValue(ModularMovingDoorBlock.ROLE,open.containsKey(c.parked)
                            ?ModularMovingDoorBlock.Role.SHARED:ModularMovingDoorBlock.Role.PANEL));
        }
    }
    private final Set<Group> groups=new LinkedHashSet<>();
    private final Map<BlockPos,Group> members=new HashMap<>();
    private final Long2ObjectOpenHashMap<Set<Group>> chunks=new Long2ObjectOpenHashMap<>();

    private static MovingDoorGroups get(ServerLevel level) {
        //? if >=1.21 {
        return level.getDataStorage().computeIfAbsent(FACTORY,NAME);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(MovingDoorGroups::load,MovingDoorGroups::new,NAME);
        *///?}
    }

    /** Read the fixed doorway cells even when the clicked panel is parked away from the frame. */
    public static List<BlockPos> entranceAnchors(ServerLevel level, BlockPos pos) {
        Group group = get(level).members.get(pos);
        return group == null || group.broken ? List.of() : group.cells.stream().map(Cell::anchor).toList();
    }
    static BlockPos unloadedPanel(ServerLevel level, BlockPos root) {
        Group group = get(level).members.get(root);
        if (group != null) for (BlockPos pos : group.open.keySet()) if (!loaded(level, pos)) return pos;
        return null;
    }
    private static boolean loaded(ServerLevel level,BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    private static long chunk(BlockPos pos) {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }
    private void add(Group group) {
        groups.add(group);
        for(BlockPos pos:group.open.keySet()) {
            members.put(pos,group);
            chunks.computeIfAbsent(chunk(pos),ignored->new LinkedHashSet<>()).add(group);
        }
    }
    private void remove(Group group) {
        groups.remove(group);
        for(BlockPos pos:group.open.keySet()) {
            members.remove(pos,group);
            long key=chunk(pos);Set<Group> entries=chunks.get(key);
            if(entries!=null){entries.remove(group);if(entries.isEmpty())chunks.remove(key);}
        }
        setDirty();
    }
    public static void register() {
        ChunkEvent.LOAD_DATA.register((chunk,level,tag)-> {
            // IO workers never inspect SavedData or mutate the level.
            long key=chunk.getPos().toLong();
            level.getServer().execute(()->get(level).reconcile(level,key));
        });
    }
    /** Also reconcile after chunk installation, before a returning player interacts with it. */
    public static void chunkSent(ServerPlayer player,int x,int z) {
        ServerLevel level=player.serverLevel();long key=ChunkPos.asLong(x,z);
        if(level.getServer().isSameThread())get(level).reconcile(level,key);
        else level.getServer().execute(()->get(level).reconcile(level,key));
    }
    private void reconcile(ServerLevel level,long chunk) {
        Set<Group> entries=chunks.get(chunk);
        if(entries!=null)for(Group group:List.copyOf(entries)) {
            detectMissing(level,group);
            if(group.broken)cleanup(level,group);
        }
    }
    private static boolean commit(ServerLevel level,Map<BlockPos,BlockState> after) {
        if (after.isEmpty()) return true;
        BlockPos[] positions=after.keySet().toArray(BlockPos[]::new);
        BlockState[] before=new BlockState[positions.length],next=new BlockState[positions.length];
        for(int i=0;i<positions.length;i++){before[i]=level.getBlockState(positions[i]);next[i]=after.get(positions[i]);}
        boolean previous=MOVING.get();MOVING.set(true);
        // Modular doors have no client-side block prediction to correct on rollback.
        try{return DoorStateUpdates.commit(level,positions,before,next,false);}
        finally{MOVING.set(previous);}
    }
    private static InteractionResult fail(Player player,String key) {
        if (player != null) player.displayClientMessage(Component.translatable("message.craftdistrict."+key),true);
        return InteractionResult.FAIL;
    }
    private static boolean unobstructed(ServerLevel level,Map<BlockPos,BlockState> after) {
        for(var entry:after.entrySet())
            if(!level.isUnobstructed(entry.getValue(),entry.getKey(),net.minecraft.world.phys.shapes.CollisionContext.empty()))return false;
        return true;
    }
    public static InteractionResult toggle(Level world,BlockPos pos,Player player) {
        return move(world, pos, player, null);
    }

    static boolean accessMove(ServerLevel level, AccessDoorTarget target, Player player, boolean open) {
        BlockState state = AccessDoorTarget.loadedState(level, target.root());
        if (state == null || !(state.getBlock() instanceof ModularMovingDoorBlock door) || door.garage()) return false;
        return state.getValue(ModularMovingDoorBlock.OPEN) == open || move(level, target.root(), player, target).consumesAction();
    }

    private static InteractionResult move(Level world, BlockPos pos, Player player, AccessDoorTarget access) {
        if(!(world instanceof ServerLevel level))return InteractionResult.SUCCESS;
        MovingDoorGroups data=get(level);Group group=data.members.get(pos);
        boolean opening=group==null;
        if(group!=null) {
            Cell first = group.cells.get(0);
            if (access == null && first.closed.getBlock() instanceof ModularMovingDoorBlock door && !door.garage()
                    && AccessDoorSecurity.secured(level, first.anchor, AccessDoorTarget.loadedState(level, first.anchor)))
                return AccessDoorSecurity.denied(level, player);
            data.detectMissing(level,group);
            if(group.broken){data.cleanup(level,group);return InteractionResult.SUCCESS;}
            Map<BlockPos,BlockState> after=new LinkedHashMap<>();
            for(var entry:group.open.entrySet()) {
                if(!loaded(level,entry.getKey()))return fail(player,"door_unloaded");
                if(player != null&&!level.mayInteract(player,entry.getKey()))return InteractionResult.FAIL;
                after.put(entry.getKey(),Blocks.AIR.defaultBlockState());
            }
            for(Cell cell:group.cells)after.put(cell.anchor,cell.closed);
            if(!unobstructed(level,after))return fail(player,"door_space");
            if(!commit(level,after))return InteractionResult.FAIL;
            data.remove(group);refresh(level,after.keySet());
        } else {
            BlockState start=level.getBlockState(pos);
            if(!(start.getBlock() instanceof ModularMovingDoorBlock door)||start.getValue(ModularMovingDoorBlock.OPEN))
                return InteractionResult.PASS;
            Map<BlockPos,BlockState> found=new LinkedHashMap<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();
            LongOpenHashSet queued = new LongOpenHashSet();
            Direction facing=start.getValue(ModularMovingDoorBlock.FACING),right=facing.getClockWise();
            Direction[] edges={right,right.getOpposite(),Direction.UP,Direction.DOWN};
            if (access == null) { queue.add(pos); queued.add(pos.asLong()); }
            else queue.addAll(access.parts());
            int minY=pos.getY(),maxY=minY,minSide=pos.getX()*right.getStepX()+pos.getZ()*right.getStepZ(),maxSide=minSide;
            while(!queue.isEmpty()) {
                BlockPos p=queue.removeFirst();
                if(!loaded(level,p))return fail(player,"door_unloaded");
                BlockState s=level.getBlockState(p);
                if(!door.compatible(start,s)||s.getValue(ModularMovingDoorBlock.OPEN)) {
                    if (access != null) return InteractionResult.FAIL;
                    continue;
                }
                if (access == null && !door.garage() && AccessDoorSecurity.secured(level, p, s)) return AccessDoorSecurity.denied(level, player);
                int side=p.getX()*right.getStepX()+p.getZ()*right.getStepZ();
                minY=Math.min(minY,p.getY());maxY=Math.max(maxY,p.getY());minSide=Math.min(minSide,side);maxSide=Math.max(maxSide,side);
                if(found.size()>=LIMIT||maxY-minY>=SPAN||maxSide-minSide>=SPAN)return fail(player,"door_size");
                if(player != null&&!level.mayInteract(player,p))return InteractionResult.FAIL;
                found.put(p,s);
                if (access == null) for (Direction edge : edges) {
                    BlockPos next = p.relative(edge);
                    if (queued.add(next.asLong())) queue.addLast(next);
                }
            }
            int width=maxSide-minSide+1;
            List<Cell> cells=new ArrayList<>();Set<BlockPos> travel=new HashSet<>();
            for(var entry:found.entrySet()) {
                BlockPos p=entry.getKey(),parked;
                if(door.garage()) {
                    int depth=maxY-p.getY()+1;
                    // Start behind the doorway plane, including the uppermost section.
                    // Stored anchor/parking pairs still close older, already-open layouts unchanged.
                    parked=new BlockPos(p.getX(),maxY,p.getZ()).relative(facing.getOpposite(),depth);
                    for(int y=p.getY();y<=maxY;y++)travel.add(new BlockPos(p.getX(),y,p.getZ()));
                    for(int d=0;d<=depth;d++)travel.add(new BlockPos(p.getX(),maxY,p.getZ()).relative(facing.getOpposite(),d));
                } else {
                    parked=p.relative(right.getOpposite(),width);
                    for(int d=1;d<=width;d++)travel.add(p.relative(right.getOpposite(),d));
                }
                cells.add(new Cell(p,parked,entry.getValue()));
            }
            for(BlockPos p:travel) {
                if(!loaded(level,p))return fail(player,"door_unloaded");
                if(level.isOutsideBuildHeight(p)||!level.getWorldBorder().isWithinBounds(p)||player != null&&!level.mayInteract(player,p))return InteractionResult.FAIL;
                if(!found.containsKey(p)&&(!level.getBlockState(p).isAir()||data.members.containsKey(p)))return fail(player,"door_space");
            }
            group=new Group(cells);
            if(!unobstructed(level,group.open))return fail(player,"door_space");
            if(!commit(level,group.open))return InteractionResult.FAIL;
            data.add(group);data.setDirty();
        }
        level.playSound(null,pos,opening?SoundEvents.IRON_TRAPDOOR_OPEN:SoundEvents.IRON_TRAPDOOR_CLOSE,SoundSource.BLOCKS,1,.85F);
        level.gameEvent(player,opening?GameEvent.BLOCK_OPEN:GameEvent.BLOCK_CLOSE,pos);
        if (access == null) WrenchItem.usedIfHeld(player);
        return InteractionResult.CONSUME;
    }
    private void detectMissing(ServerLevel level,Group group) {
        if(group.broken)return; // Already-restored anchors are valid during deferred, cross-chunk cleanup.
        LevelChunk loadedChunk = null;
        long lastChunk = Long.MIN_VALUE;
        for (var entry : group.open.entrySet()) {
            BlockPos pos = entry.getKey();
            long key = chunk(pos);
            if (key != lastChunk) {
                lastChunk = key;
                loadedChunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            }
            if (loadedChunk == null || loadedChunk.getBlockState(pos) == entry.getValue()) continue;
            if (!group.broken) invalidateAccess(level, group);
            group.broken=true;
            for(Cell cell:group.cells)if(cell.anchor.equals(pos)||cell.parked.equals(pos))group.removed.add(cell.anchor);
            setDirty();
        }
    }
    public static void removed(Level world,BlockPos pos) {
        if(MOVING.get()||!(world instanceof ServerLevel level))return;
        MovingDoorGroups data=get(level);Group group=data.members.get(pos);if(group==null)return;
        invalidateAccess(level, group);
        group.broken=true;
        for(Cell cell:group.cells)if(cell.anchor.equals(pos)||cell.parked.equals(pos))group.removed.add(cell.anchor);
        data.setDirty();data.cleanup(level,group);
    }
    private static void invalidateAccess(ServerLevel level, Group group) {
        Cell first = group.cells.get(0);
        if (first.closed.getBlock() instanceof ModularMovingDoorBlock door && !door.garage())
            AccessControlData.get(level).doorRemoved(first.anchor);
    }
    /** Restore surviving segments and remove their parking proxies, without spawning extra loot. */
    private void cleanup(ServerLevel level,Group group) {
        Map<BlockPos,BlockState> after=new LinkedHashMap<>();boolean complete=true;
        Map<BlockPos,Cell> anchors=new HashMap<>();for(Cell cell:group.cells)anchors.put(cell.anchor,cell);
        for(var entry:group.open.entrySet()) {
            BlockPos p=entry.getKey();
            if(!loaded(level,p)){complete=false;continue;}
            if(level.getBlockState(p)!=entry.getValue())continue; // Never overwrite another block.
            Cell cell=anchors.get(p);
            after.put(p,cell!=null&&!group.removed.contains(p)?cell.closed:Blocks.AIR.defaultBlockState());
        }
        if(!commit(level,after))return;
        refresh(level,after.keySet());
        if(complete)remove(group);
    }
    private static void refresh(ServerLevel level,Collection<BlockPos> positions) {
        if (positions.isEmpty()) return;
        Set<BlockPos> adjacent=new HashSet<>();
        for(BlockPos pos:positions){adjacent.add(pos);for(Direction side:Direction.values())adjacent.add(pos.relative(side));}
        for(BlockPos p:adjacent)if(loaded(level,p)) {
            BlockState state=level.getBlockState(p);
            if(state.getBlock() instanceof ModularMovingDoorBlock door&&!state.getValue(ModularMovingDoorBlock.OPEN)) {
                BlockState updated=door.layout(state,level,p);
                if(state!=updated)level.setBlock(p,updated,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
    private static MovingDoorGroups load(CompoundTag tag) {
        MovingDoorGroups data=new MovingDoorGroups();ListTag list=tag.getList("groups",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++) {
            CompoundTag saved=list.getCompound(i);ListTag entries=saved.getList("cells",Tag.TAG_COMPOUND);
            if(entries.isEmpty()||entries.size()>LIMIT)continue;
            List<Cell> cells=new ArrayList<>();Set<BlockPos> anchors=new HashSet<>(),parking=new HashSet<>();
            boolean valid=true;BlockState first=null;BlockPos origin=null;
            for(int j=0;j<entries.size();j++) {
                CompoundTag c=entries.getCompound(j);BlockPos a=BlockPos.of(c.getLong("anchor")),p=BlockPos.of(c.getLong("parked"));
                BlockState state=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),c.getCompound("state"));
                if(!(state.getBlock() instanceof ModularMovingDoorBlock door)||state.getValue(ModularMovingDoorBlock.OPEN)
                        ||state.getValue(ModularMovingDoorBlock.ROLE)!=ModularMovingDoorBlock.Role.ANCHOR
                        ||!anchors.add(a)||!parking.add(p)||data.members.containsKey(a)||data.members.containsKey(p)){valid=false;break;}
                if(first==null){first=state;origin=a;}
                if(!door.compatible(first,state)||a.distManhattan(origin)>2*SPAN||a.distManhattan(p)>2*SPAN){valid=false;break;}
                cells.add(new Cell(a,p,state));
            }
            if(!valid)continue;
            Group group=new Group(cells);group.broken=saved.getBoolean("broken");
            for(long removed:saved.getLongArray("removed"))if(anchors.contains(BlockPos.of(removed)))group.removed.add(BlockPos.of(removed));
            data.add(group);
        }
        return data;
    }
    //? if >=1.21 {
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
    //?} else {
    /*@Override public CompoundTag save(CompoundTag tag) {
    *///?}
        ListTag list=new ListTag();
        for(Group group:groups) {
            CompoundTag saved=new CompoundTag();ListTag cells=new ListTag();
            for(Cell cell:group.cells) {
                CompoundTag c=new CompoundTag();c.putLong("anchor",cell.anchor.asLong());c.putLong("parked",cell.parked.asLong());
                c.put("state",NbtUtils.writeBlockState(cell.closed));cells.add(c);
            }
            saved.put("cells",cells);saved.putBoolean("broken",group.broken);
            saved.putLongArray("removed",group.removed.stream().mapToLong(BlockPos::asLong).toArray());list.add(saved);
        }
        tag.put("groups",list);return tag;
    }
}
