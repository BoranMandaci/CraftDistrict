package com.craftdistrict.lighting;

import dev.architectury.event.events.common.ChunkEvent;
import dev.architectury.event.events.common.TickEvent;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
//?}

/** Sparse shared scheduling: proximity once a second, effects only at deterministic stage boundaries. */
public final class LightProximityData extends SavedData {
    private static final String NAME = "craftdistrict_light_proximity";
    private static final int INTERVAL = 20;
    public static final double RANGE = 10;
    public static final double RELEASE_RANGE = 10.5;
    //? if >=1.21 {
    private static final Factory<LightProximityData> FACTORY = new Factory<>(LightProximityData::new,
            (tag, registries) -> load(tag), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    //?}
    private final LongOpenHashSet origins = new LongOpenHashSet();
    private final Long2ObjectOpenHashMap<LongOpenHashSet>[] phases;
    private final Long2ByteOpenHashMap animated = new Long2ByteOpenHashMap();
    private final Long2ByteOpenHashMap offsets = new Long2ByteOpenHashMap();
    private final Long2ObjectOpenHashMap<LongOpenHashSet>[][] effects;

    @SuppressWarnings("unchecked")
    public LightProximityData() {
        phases = new Long2ObjectOpenHashMap[INTERVAL];
        for (int i = 0; i < INTERVAL; i++) phases[i] = new Long2ObjectOpenHashMap<>();
        effects = new Long2ObjectOpenHashMap[6][INTERVAL];
        animated.defaultReturnValue((byte)-1);
    }

    public static LightProximityData get(ServerLevel level) {
        //? if >=1.21 {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(LightProximityData::load, LightProximityData::new, NAME);
        *///?}
    }

    public static void register() {
        TickEvent.SERVER_LEVEL_POST.register(level -> get(level).tick(level));
        // Palette-filtered discovery on load also preserves effects saved by the earlier texture-only implementation.
        // The event may run on a chunk IO worker; SavedData and world changes belong exclusively to the server thread.
        ChunkEvent.LOAD_DATA.register((chunk, level, tag) -> {
            Long2ByteOpenHashMap found = new Long2ByteOpenHashMap();
            chunk.findBlocks(state -> LightControls.settingsEligible(state.getBlock()) && !LedLightGroups.panel(state) && state.getValue(LightControls.LIT)
                            && LightControls.effect(state) != LightEffect.STEADY,
                    (pos, state) -> found.put(LightControls.origin(state, pos).asLong(), (byte)LightControls.effect(state).ordinal()));
            if (found.isEmpty()) return;
            level.getServer().execute(() -> {
                LightProximityData data = get(level);
                for (var entry : found.long2ByteEntrySet())
                    data.setAnimation(BlockPos.of(entry.getLongKey()), LightEffect.byId(entry.getByteValue()));
            });
        });
    }

    static int phase(long pos) { return (int)Long.remainderUnsigned(HashCommon.mix(pos), INTERVAL); }
    private static long chunk(BlockPos origin) { return ChunkPos.asLong(origin.getX() >> 4, origin.getZ() >> 4); }
    public boolean contains(BlockPos origin) { return origins.contains(origin.asLong()); }
    public boolean animated(BlockPos origin, LightEffect effect) {
        return effect != LightEffect.STEADY && animated.get(origin.asLong()) == effect.ordinal();
    }

    private static void remove(Long2ObjectOpenHashMap<LongOpenHashSet> chunks, long chunk, long pos) {
        var values = chunks.get(chunk);
        if (values != null) { values.remove(pos); if (values.isEmpty()) chunks.remove(chunk); }
    }

    public boolean set(BlockPos origin, boolean enabled) {
        long pos = origin.asLong();
        if (enabled ? !origins.add(pos) : !origins.remove(pos)) return false;
        var bucket = phases[phase(pos)];
        if (enabled) bucket.computeIfAbsent(chunk(origin), ignored -> new LongOpenHashSet()).add(pos);
        else remove(bucket, chunk(origin), pos);
        setDirty();
        return true;
    }

    public void setAnimation(BlockPos origin, LightEffect effect) { setAnimation(origin, effect, phase(origin.asLong())); }
    public void setAnimation(BlockPos origin, LightEffect effect, int offset) {
        long pos = origin.asLong();
        int previous = animated.get(pos), next = effect == LightEffect.STEADY ? -1 : effect.ordinal();
        int oldOffset = offsets.containsKey(pos) ? offsets.get(pos) : phase(pos);
        if (previous == next && (next < 0 || oldOffset == offset)) return;
        int phase = offset; long chunk = chunk(origin);
        if (previous >= 0) remove(effects[previous][oldOffset], chunk, pos);
        if (next < 0) { animated.remove(pos); offsets.remove(pos); }
        else {
            animated.put(pos, (byte)next); offsets.put(pos, (byte)offset);
            var groups = effects[next][phase];
            if (groups == null) effects[next][phase] = groups = new Long2ObjectOpenHashMap<>();
            groups.computeIfAbsent(chunk, ignored -> new LongOpenHashSet()).add(pos);
        }
        setDirty();
    }

    private void tick(ServerLevel level) {
        LedLightGroups groups = LedLightGroups.get(level);
        groups.tickLoads(level);
        long time = level.getGameTime();
        if (!origins.isEmpty()) proximity(level, phases[(int)Math.floorMod(time, INTERVAL)]);
        if (animated.isEmpty()) return;
        for (int id = 1; id < 6; id++) {
            LightEffect effect = LightEffect.byId(id);
            for (int offset = 0; offset < INTERVAL; offset++) {
                var phaseGroup = effects[id][offset];
                // Skip entire groups between boundaries; no per-light ticking, reads, allocations or packets.
                if (phaseGroup == null || phaseGroup.isEmpty() || effect.levelAt(time + offset) == effect.levelAt(time + offset - 1)) continue;
                var chunks = phaseGroup.long2ObjectEntrySet().fastIterator();
                while (chunks.hasNext()) {
                    var entry = chunks.next(); long chunk = entry.getLongKey();
                    boolean chunkLoaded = level.getChunkSource().getChunkNow(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)) != null;
                    if (!chunkLoaded && !groups.knownChunk(chunk)) continue;
                    var positions = entry.getValue().iterator();
                    while (positions.hasNext()) {
                        long packed = positions.nextLong();
                        var group = groups.root(packed);
                        if (group != null) {
                            if (group.active() && group.effect() == effect) groups.animate(level, group);
                            else { positions.remove(); animated.remove(packed); offsets.remove(packed); setDirty(); }
                            continue;
                        }
                        if (!chunkLoaded) continue;
                        BlockPos pos = BlockPos.of(packed);
                        var state = level.getBlockState(pos);
                        if (LedLightGroups.panel(state)) {
                            positions.remove(); animated.remove(packed); offsets.remove(packed); groups.defer(level, pos); setDirty(); continue;
                        }
                        if (!LightControls.settingsEligible(state.getBlock()) || LightControls.effect(state) != effect || !LightControls.origin(state, pos).equals(pos)) {
                            positions.remove(); animated.remove(packed); offsets.remove(packed); setDirty(); continue;
                        }
                        LightControls.updateEffect(level, pos, state);
                    }
                    if (entry.getValue().isEmpty()) chunks.remove();
                }
            }
        }
    }

    private void proximity(ServerLevel level, Long2ObjectOpenHashMap<LongOpenHashSet> group) {
        var groups = LedLightGroups.get(level);
        var chunks = group.long2ObjectEntrySet().fastIterator();
        while (chunks.hasNext()) {
            var entry = chunks.next(); long chunk = entry.getLongKey();
            // Never request a chunk load or inspect players/individual lights in unloaded chunks.
            boolean chunkLoaded = level.getChunkSource().getChunkNow(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)) != null;
            if (!chunkLoaded && !groups.knownChunk(chunk)) continue;
            var positions = entry.getValue().iterator();
            while (positions.hasNext()) {
                long packed = positions.nextLong();
                var shared = groups.root(packed);
                if (shared != null) { groups.control(level, shared); continue; }
                if (!chunkLoaded) continue;
                BlockPos origin = BlockPos.of(packed);
                var state = level.getBlockState(origin);
                if (LedLightGroups.panel(state)) {
                    positions.remove(); origins.remove(packed); groups.defer(level, origin); setDirty(); continue;
                }
                if (!LightControls.settingsEligible(state.getBlock()) || state.getValue(LightControls.REDSTONE)
                        || !LightControls.origin(state, origin).equals(origin)) {
                    positions.remove(); origins.remove(packed); setDirty(); continue;
                }
                LightControls.updateProximity(level, origin, state);
            }
            if (entry.getValue().isEmpty()) chunks.remove();
        }
    }

    public static LightProximityData load(CompoundTag tag) {
        LightProximityData data = new LightProximityData();
        for (long pos : tag.getLongArray("origins")) data.set(BlockPos.of(pos), true);
        ListTag animations = tag.getList("animated", Tag.TAG_COMPOUND);
        for (int i = 0; i < animations.size(); i++) {
            CompoundTag entry = animations.getCompound(i);
            if (entry.contains("pos", Tag.TAG_LONG) && LightEffect.valid(entry.getByte("effect")))
                data.setAnimation(BlockPos.of(entry.getLong("pos")), LightEffect.byId(entry.getByte("effect")),
                        entry.contains("offset", Tag.TAG_BYTE) ? Math.floorMod(entry.getByte("offset"), INTERVAL) : phase(entry.getLong("pos")));
        }
        data.setDirty(false); return data;
    }

    @Override public CompoundTag save(CompoundTag tag
            //? if >=1.21 {
            , HolderLookup.Provider registries
            //?}
    ) {
        tag.putLongArray("origins", origins.toLongArray());
        ListTag animations = new ListTag();
        for (var entry : animated.long2ByteEntrySet()) {
            CompoundTag value = new CompoundTag(); value.putLong("pos", entry.getLongKey()); value.putByte("effect", entry.getByteValue()); value.putByte("offset", offsets.get(entry.getLongKey()));
            animations.add(value);
        }
        tag.put("animated", animations); return tag;
    }
}
