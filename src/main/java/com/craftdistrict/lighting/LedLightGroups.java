package com.craftdistrict.lighting;

import com.craftdistrict.block.ModularLedPanelBlock;
import com.craftdistrict.block.SurfaceLightBlock;
import com.craftdistrict.network.LightDyeNetworking;
import dev.architectury.event.events.common.ChunkEvent;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
//?}
import net.minecraft.world.level.saveddata.SavedData;

/** Persisted panel topology/settings, with one scheduler entry per connected component and no block entities. */
public final class LedLightGroups extends SavedData {
    private static final String NAME = "craftdistrict_led_groups";
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final Direction[][] EDGES = {
            {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST},
            {Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST},
            {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
            {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
            {Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH}
    };
    //? if >=1.21 {
    private static final Factory<LedLightGroups> FACTORY = new Factory<>(LedLightGroups::new,
            (tag, registries) -> load(tag), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    //?}
    private final Long2ObjectOpenHashMap<Group> roots = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Group> members = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> chunks = new Long2ObjectOpenHashMap<>();
    private final Long2LongOpenHashMap pending = new Long2LongOpenHashMap();
    private long revision;
    private boolean rebuilding;

    public static final class Group {
        public final long root, revision;
        public final int mount, phase;
        private final long[] members;
        private final Long2ObjectOpenHashMap<long[]> chunks = new Long2ObjectOpenHashMap<>();
        private LightMode mode;
        private LightEffect effect;
        private boolean manual, active;
        private int color;
        private int lastLevel = -1;
        private BlockPos poweredMember;
        private final int minX, minY, minZ, maxX, maxY, maxZ;
        private Group(long[] positions, int mount, long revision, Settings settings) {
            Arrays.sort(positions); this.members = positions; root = positions[0]; this.mount = mount; this.revision = revision;
            mode = settings.mode; effect = settings.effect; manual = settings.manual; active = settings.active;
            color = settings.color; phase = settings.phase;
            Long2ObjectOpenHashMap<LongArrayList> byChunk = new Long2ObjectOpenHashMap<>();
            int lowX = Integer.MAX_VALUE, lowY = Integer.MAX_VALUE, lowZ = Integer.MAX_VALUE;
            int highX = Integer.MIN_VALUE, highY = Integer.MIN_VALUE, highZ = Integer.MIN_VALUE;
            for (long pos : positions) {
                int x = BlockPos.getX(pos), y = BlockPos.getY(pos), z = BlockPos.getZ(pos);
                lowX = Math.min(lowX, x); lowY = Math.min(lowY, y); lowZ = Math.min(lowZ, z);
                highX = Math.max(highX, x); highY = Math.max(highY, y); highZ = Math.max(highZ, z);
                byChunk.computeIfAbsent(ChunkPos.asLong(x >> 4, z >> 4), ignored -> new LongArrayList()).add(pos);
            }
            for (var entry : byChunk.long2ObjectEntrySet()) chunks.put(entry.getLongKey(), entry.getValue().toLongArray());
            minX = lowX; minY = lowY; minZ = lowZ;
            maxX = highX; maxY = highY; maxZ = highZ;
        }
        public int size() { return members.length; }
        public long[] members() { return members.clone(); }
        public LightMode mode() { return mode; }
        public LightEffect effect() { return effect; }
        public boolean manual() { return manual; }
        public boolean active() { return active; }
        public int color() { return color; }
        private Settings settings() { return new Settings(mode, effect, manual, active, color, phase); }
    }
    private record Settings(LightMode mode, LightEffect effect, boolean manual, boolean active, int color, int phase) { }
    public static LedLightGroups get(ServerLevel level) {
        //? if >=1.21 {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(LedLightGroups::load, LedLightGroups::new, NAME);
        *///?}
    }
    public static boolean panel(BlockState state) { return state.getBlock() instanceof ModularLedPanelBlock; }
    private static long chunk(BlockPos pos) { return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4); }
    public static int mounting(BlockState state) {
        return state.getValue(SurfaceLightBlock.WALL_MOUNTED) ? 1 + state.getValue(ModularLedPanelBlock.FACING).get2DDataValue() : 0;
    }
    private static Direction[] edges(int mount) {
        return EDGES[mount];
    }
    private static boolean loaded(ServerLevel level, long chunk) {
        return level.getChunkSource().getChunkNow(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)) != null;
    }
    public Group find(BlockPos pos) { return members.get(pos.asLong()); }
    public Group root(long pos) { return roots.get(pos); }
    public boolean knownChunk(long key) { return chunks.containsKey(key); }
    public void defer(ServerLevel level, BlockPos pos) { pending.put(chunk(pos), level.getGameTime() + 200); }

    public static void register() {
        ChunkEvent.LOAD_DATA.register((chunk, level, tag) -> {
            boolean hasPanels = false;
            for (var section : chunk.getSections()) if (section != null && section.maybeHas(LedLightGroups::panel)) { hasPanels = true; break; }
            if (!hasPanels) return;
            long key = chunk.getPos().toLong();
            level.getServer().execute(() -> get(level).pending.put(key, level.getGameTime() + 200));
        });
    }
    /** Chunk IO can finish before the chunk is installed. Reconcile once it is available, never load it ourselves. */
    public void tickLoads(ServerLevel level) {
        if (pending.isEmpty()) return;
        long previousRevision = revision, time = level.getGameTime();
        Set<Group> touched = Collections.newSetFromMap(new IdentityHashMap<>());
        var iterator = pending.long2LongEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next(); long key = entry.getLongKey();
            var chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk == null) { if (time > entry.getLongValue()) iterator.remove(); continue; }
            iterator.remove();
            LongOpenHashSet actual = new LongOpenHashSet();
            chunk.findBlocks(LedLightGroups::panel, (pos, state) -> actual.add(pos.asLong()));
            var known = chunks.get(key);
            if (known != null) for (long pos : known.toLongArray()) if (!actual.contains(pos)) refresh(level, BlockPos.of(pos), false);
            for (long packed : actual) {
                Group group = ensure(level, BlockPos.of(packed)); if (group != null) touched.add(group);
            }
        }
        // A component spanning several loaded chunks needs one reconciliation, not one per chunk.
        for (Group group : touched) if (roots.get(group.root) == group) {
            // refresh() already reconciled any rebuilt group against the currently loaded chunks.
            if (group.revision > previousRevision && group.lastLevel >= 0) continue;
            sync(level, group); schedule(level, group);
        }
    }
    public Group ensure(ServerLevel level, BlockPos pos) {
        if (!loaded(level, chunk(pos))) return find(pos);
        BlockState state = level.getBlockState(pos);
        if (!panel(state)) return null;
        Group group = find(pos);
        if (rebuilding) return group;
        if (group == null || group.mount != mounting(state) || topologyChanged(level, pos, state, group)) refresh(level, pos, false);
        return find(pos);
    }
    private boolean topologyChanged(ServerLevel level, BlockPos pos, BlockState state, Group group) {
        for (Direction side : edges(mounting(state))) {
            BlockPos neighbor = pos.relative(side);
            if (!loaded(level, chunk(neighbor))) continue;
            BlockState other = level.getBlockState(neighbor);
            boolean connected = panel(other) && mounting(other) == mounting(state);
            if (connected ? find(neighbor) != group : find(neighbor) == group) return true;
        }
        return false;
    }
    public void placed(ServerLevel level, BlockPos pos) { refresh(level, pos, true); }
    public void removed(ServerLevel level, BlockPos pos) { if (find(pos) != null) refresh(level, pos, false); }

    private Settings legacy(ServerLevel level, BlockPos pos, BlockState state) {
        LightProximityData data = LightProximityData.get(level);
        LightEffect effect = LightControls.effect(state);
        boolean active = state.getValue(LightControls.LIT) || data.animated(pos, effect);
        LightMode mode = state.getValue(LightControls.REDSTONE) ? LightMode.REDSTONE
                : data.contains(pos) ? LightMode.PROXIMITY : LightMode.MANUAL;
        return new Settings(mode, effect, active, active, LightDyeData.get(level).color(pos), LightProximityData.phase(pos.asLong()));
    }
    private void index(Group group) {
        roots.put(group.root, group);
        for (long packed : group.members) {
            members.put(packed, group);
            chunks.computeIfAbsent(chunk(BlockPos.of(packed)), ignored -> new LongOpenHashSet()).add(packed);
        }
    }
    private void unindex(Group group) {
        roots.remove(group.root);
        for (long packed : group.members) {
            members.remove(packed); long key = chunk(BlockPos.of(packed)); var set = chunks.get(key);
            if (set != null) { set.remove(packed); if (set.isEmpty()) chunks.remove(key); }
        }
    }
    /** Graph work occurs on topology edits/load reconciliation, using saved nodes at unloaded chunk borders. */
    private void refresh(ServerLevel level, BlockPos seed, boolean newlyPlaced) {
        if (rebuilding) return;
        rebuilding = true;
        try {
            LongArrayFIFOQueue queue = new LongArrayFIFOQueue(); queue.enqueue(seed.asLong());
            LongOpenHashSet visited = new LongOpenHashSet();
            Long2ByteOpenHashMap nodes = new Long2ByteOpenHashMap();
            Long2ObjectOpenHashMap<Settings> settings = new Long2ObjectOpenHashMap<>();
            Long2LongOpenHashMap priorities = new Long2LongOpenHashMap();
            Map<Group, Settings> previous = new IdentityHashMap<>();
            while (!queue.isEmpty()) {
                long packed = queue.dequeueLong(); if (!visited.add(packed)) continue;
                BlockPos pos = BlockPos.of(packed); Group old = members.get(packed);
                if (old != null && !previous.containsKey(old)) {
                    previous.put(old, old.settings());
                    for (long p : old.members) queue.enqueue(p);
                }
                int mounting;
                if (loaded(level, chunk(pos))) {
                    BlockState state = level.getBlockState(pos); if (!panel(state)) continue;
                    mounting = mounting(state);
                    if (old == null) settings.put(packed, legacy(level, pos, state));
                } else { if (old == null) continue; mounting = old.mount; }
                nodes.put(packed, (byte)mounting);
                if (old != null) settings.put(packed, previous.get(old));
                priorities.put(packed, old == null ? packed : old.root);
                for (Direction side : edges(mounting)) {
                    BlockPos next = pos.relative(side); Group known = members.get(next.asLong());
                    if (loaded(level, chunk(next))) {
                        BlockState other = level.getBlockState(next);
                        if (panel(other) && mounting(other) == mounting) queue.enqueue(next.asLong());
                    } else if (known != null && known.mount == mounting) queue.enqueue(next.asLong());
                }
            }
            LightProximityData scheduler = LightProximityData.get(level);
            for (Group group : previous.keySet()) unindex(group);
            for (long packed : visited) { BlockPos pos = BlockPos.of(packed); scheduler.set(pos, false); scheduler.setAnimation(pos, LightEffect.STEADY); }
            LongOpenHashSet assigned = new LongOpenHashSet();
            for (long start : nodes.keySet()) {
                if (!assigned.add(start)) continue;
                LongArrayList component = new LongArrayList(); queue.enqueue(start);
                Settings winner = null; long priority = Long.MAX_VALUE; int mounting = nodes.get(start);
                while (!queue.isEmpty()) {
                    long packed = queue.dequeueLong(); component.add(packed);
                    if (!(newlyPlaced && packed == seed.asLong()) && (winner == null || priorities.get(packed) < priority)) {
                        winner = settings.get(packed); priority = priorities.get(packed);
                    }
                    BlockPos pos = BlockPos.of(packed);
                    for (Direction side : edges(mounting)) {
                        long next = pos.relative(side).asLong();
                        if (nodes.containsKey(next) && nodes.get(next) == mounting && assigned.add(next)) queue.enqueue(next);
                    }
                }
                if (winner == null) winner = new Settings(LightMode.MANUAL, LightEffect.STEADY, true, true, -1, LightProximityData.phase(start));
                Group group = new Group(component.toLongArray(), mounting, ++revision, winner);
                index(group); sync(level, group); schedule(level, group);
            }
            setDirty();
        } finally { rebuilding = false; }
    }
    public List<BlockPos> loadedParts(ServerLevel level, Group group) {
        return loadedParts(level, group, null);
    }
    private List<BlockPos> loadedParts(ServerLevel level, Group group, List<BlockState> snapshot) {
        List<BlockPos> result = new ArrayList<>();
        var chunks = group.chunks.long2ObjectEntrySet().fastIterator();
        while (chunks.hasNext()) {
            var entry = chunks.next(); long key = entry.getLongKey();
            var chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk == null) continue;
            for (long packed : entry.getValue()) {
                BlockPos pos = BlockPos.of(packed); BlockState state = chunk.getBlockState(pos);
                if (panel(state) && mounting(state) == group.mount) {
                    result.add(pos);
                    if (snapshot != null) snapshot.add(state);
                }
            }
        }
        return result;
    }
    public List<BlockPos> allParts(Group group) {
        List<BlockPos> result = new ArrayList<>(group.size()); for (long pos : group.members) result.add(BlockPos.of(pos)); return result;
    }
    private boolean nearby(ServerLevel level, Group group) {
        double radius = group.active ? LightProximityData.RELEASE_RANGE : LightProximityData.RANGE;
        double limit = radius * radius;
        for (Player player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            double x = player.getX(), y = player.getY(), z = player.getZ();
            double dy = distanceToSpan(y, group.minY + .5, group.maxY + .5), vertical = dy * dy;
            double dx = distanceToSpan(x, group.minX + .5, group.maxX + .5);
            double dz = distanceToSpan(z, group.minZ + .5, group.maxZ + .5);
            if (dx * dx + vertical + dz * dz > limit) continue;
            var chunks = group.chunks.long2ObjectEntrySet().fastIterator();
            while (chunks.hasNext()) {
                var entry = chunks.next(); long key = entry.getLongKey();
                double lowX = (ChunkPos.getX(key) << 4) + .5, lowZ = (ChunkPos.getZ(key) << 4) + .5;
                dx = distanceToSpan(x, lowX, lowX + 15);
                dz = distanceToSpan(z, lowZ, lowZ + 15);
                if (dx * dx + vertical + dz * dz > limit || !loaded(level, key)) continue;
                for (long packed : entry.getValue()) {
                    if (player.distanceToSqr(BlockPos.getX(packed) + .5, BlockPos.getY(packed) + .5, BlockPos.getZ(packed) + .5) <= limit)
                        return true;
                }
            }
        }
        return false;
    }
    private static double distanceToSpan(double value, double min, double max) {
        return Math.max(Math.max(min - value, value - max), 0);
    }
    private boolean powered(ServerLevel level, Group group, BlockPos changedMember) {
        LightControls.PowerCheck power = new LightControls.PowerCheck(level);
        BlockPos previous = group.poweredMember;
        long previousPos = previous == null ? 0 : previous.asLong();
        if (previous != null && loaded(level, chunk(previous)) && power.hasPower(previousPos)) return true;

        long changedPos = changedMember == null ? 0 : changedMember.asLong();
        if (changedMember != null && !changedMember.equals(previous) && loaded(level, chunk(changedMember))
                && power.hasPower(changedPos)) {
            group.poweredMember = changedMember.immutable();
            return true;
        }
        group.poweredMember = null;
        // A negative result still needs the full loaded set, including indirect signals and changes on chunk boundaries.
        var chunks = group.chunks.long2ObjectEntrySet().fastIterator();
        while (chunks.hasNext()) {
            var entry = chunks.next();
            if (!loaded(level, entry.getLongKey())) continue;
            for (long packed : entry.getValue()) {
                if ((previous != null && packed == previousPos) || (changedMember != null && packed == changedPos)) continue;
                if (!power.hasPower(packed)) continue;
                group.poweredMember = BlockPos.of(packed);
                return true;
            }
        }
        return false;
    }
    private boolean activation(ServerLevel level, Group group, LightMode mode, boolean manual, BlockPos changedMember) {
        return switch (mode) { case MANUAL -> manual; case REDSTONE -> powered(level, group, changedMember); case PROXIMITY -> nearby(level, group); };
    }
    private boolean render(ServerLevel level, Group group, LightMode mode, LightEffect effect, boolean active) {
        List<BlockState> before = new ArrayList<>();
        List<BlockPos> parts = loadedParts(level, group, before);
        BlockState[] after = new BlockState[parts.size()];
        int brightness = active ? effect.levelAt(level.getGameTime() + group.phase) : 0;
        boolean redstone = mode == LightMode.REDSTONE, lit = brightness > 0, changed = false;
        LightEffect.Stage stage = LightEffect.Stage.of(effect, brightness);
        BlockState previous = null, target = null;
        for (int i = 0; i < parts.size(); i++) {
            BlockState current = before.get(i);
            if (current != previous) {
                previous = current;
                target = previous.setValue(LightControls.REDSTONE, redstone)
                        .setValue(LightControls.LIT, lit).setValue(LightControls.EFFECT, stage);
            }
            after[i] = target;
            changed |= current != target;
        }
        if (!changed) {
            group.lastLevel = brightness;
            return true;
        }
        int written = 0;
        for (int i = 0; i < parts.size(); i++) {
            BlockState original = before.get(i);
            if (level.getBlockState(parts.get(i)) != original || after[i] != original && !level.setBlock(parts.get(i), after[i], FLAGS)) {
                for (int j = written - 1; j >= 0; j--)
                    if (level.getBlockState(parts.get(j)) == after[j] && after[j] != before.get(j))
                        level.setBlock(parts.get(j), before.get(j), FLAGS);
                return false;
            }
            written++;
        }
        for (int i = 0; i < parts.size(); i++) if (level.getBlockState(parts.get(i)) != after[i]) {
            for (int j = written - 1; j >= 0; j--) if (level.getBlockState(parts.get(j)) == after[j] && before.get(j) != after[j])
                level.setBlock(parts.get(j), before.get(j), FLAGS);
            return false;
        }
        group.lastLevel = brightness; return true;
    }
    private void schedule(ServerLevel level, Group group) {
        var data = LightProximityData.get(level); BlockPos root = BlockPos.of(group.root);
        data.set(root, group.mode != LightMode.MANUAL);
        data.setAnimation(root, group.active ? group.effect : LightEffect.STEADY, group.phase);
    }
    private void sync(ServerLevel level, Group group) {
        boolean active = activation(level, group, group.mode, group.manual, null);
        if (active != group.active) setDirty();
        group.active = active;
        render(level, group, group.mode, group.effect, group.active);
        LightDyeData dyes = LightDyeData.get(level); List<BlockPos> changed = new ArrayList<>();
        for (long packed : group.members) {
            BlockPos pos = BlockPos.of(packed);
            if (dyes.inherit(pos, group.color) && loaded(level, chunk(pos))) changed.add(pos);
        }
        if (!changed.isEmpty()) LightDyeNetworking.changed(level, changed);
    }
    public boolean apply(ServerLevel level, Group group, LightMode mode, LightEffect effect, boolean manual) {
        if (roots.get(group.root) != group) return false;
        boolean active = activation(level, group, mode, manual, null);
        if (!render(level, group, mode, effect, active)) return false;
        group.mode = mode; group.effect = effect; group.manual = manual; group.active = active;
        if (mode != LightMode.REDSTONE) group.poweredMember = null;
        schedule(level, group); setDirty(); return true;
    }
    public void color(Group group, int color) { group.color = color; setDirty(); }
    public void control(ServerLevel level, Group group) {
        control(level, group, null);
    }
    public void control(ServerLevel level, Group group, BlockPos changedMember) {
        boolean active = activation(level, group, group.mode, group.manual, changedMember);
        if (active == group.active) return;
        if (render(level, group, group.mode, group.effect, active)) { group.active = active; schedule(level, group); setDirty(); }
    }
    void animate(ServerLevel level, Group group, int brightness) {
        if (brightness == group.lastLevel) return;
        LightEffect.Stage stage = LightEffect.Stage.of(group.effect, brightness);
        boolean lit = brightness > 0;
        boolean complete = true;
        // Animation only changes the frame; settings/dye transactions retain the rollback path in render().
        // Stream loaded chunks once, without per-frame part lists or before/after arrays.
        var chunks = group.chunks.long2ObjectEntrySet().fastIterator();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState previous = null, target = null;
        while (chunks.hasNext()) {
            var entry = chunks.next(); long key = entry.getLongKey();
            var chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk == null) continue;
            for (long packed : entry.getValue()) {
                pos.set(packed);
                BlockState before = chunk.getBlockState(pos);
                if (before != previous) {
                    previous = before;
                    target = panel(before) && mounting(before) == group.mount
                            ? before.setValue(LightControls.LIT, lit).setValue(LightControls.EFFECT, stage) : before;
                }
                if (target != before && !level.setBlock(pos.immutable(), target, FLAGS)) complete = false;
            }
        }
        group.lastLevel = complete ? brightness : -1;
    }

    public static LedLightGroups load(CompoundTag tag) {
        LedLightGroups data = new LedLightGroups(); data.revision = tag.getLong("revision");
        ListTag list = tag.getList("groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag value = list.getCompound(i); long[] positions = value.getLongArray("members");
            int mount = value.getByte("mount"), color = value.getByte("color"), phase = value.getByte("phase");
            if (positions.length == 0 || mount < 0 || mount > 4 || color < -1 || color > 15 || phase < 0 || phase >= 20) continue;
            Settings settings = new Settings(LightMode.byId(value.getByte("mode")), LightEffect.byId(value.getByte("effect")),
                    value.getBoolean("manual"), value.getBoolean("active"), color, phase);
            data.index(new Group(positions, mount, ++data.revision, settings));
        }
        data.setDirty(false); return data;
    }
    @Override public CompoundTag save(CompoundTag tag
            //? if >=1.21 {
            , HolderLookup.Provider registries
            //?}
    ) {
        ListTag list = new ListTag();
        for (Group group : roots.values()) {
            CompoundTag value = new CompoundTag(); value.putLongArray("members", group.members); value.putByte("mount", (byte)group.mount);
            value.putByte("mode", (byte)group.mode.ordinal()); value.putByte("effect", (byte)group.effect.ordinal());
            value.putByte("color", (byte)group.color); value.putByte("phase", (byte)group.phase);
            value.putBoolean("manual", group.manual); value.putBoolean("active", group.active); list.add(value);
        }
        tag.putLong("revision", revision); tag.put("groups", list); return tag;
    }
}
