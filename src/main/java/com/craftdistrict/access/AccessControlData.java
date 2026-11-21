package com.craftdistrict.access;

import com.craftdistrict.item.AccessCardItem;
import com.craftdistrict.block.AccessDoorMotion;
import com.craftdistrict.block.OfficeBadgeReaderBlock;
import com.craftdistrict.block.SlidingEntranceBlock;
import com.craftdistrict.door.SlidingDoorSensors;
import com.craftdistrict.network.AccessControlNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Per-dimension reader settings. Unconfigured world blocks do not need a block entity. */
public final class AccessControlData extends SavedData {
    private static final String NAME = "craftdistrict_access_control";
    public static final int DEFAULT_SECONDS = 5, MAX_SECONDS = 30, ENROLLMENT_TICKS = 60 * 20;
    //? if >=1.21 {
    private static final Factory<AccessControlData> FACTORY = new Factory<>(AccessControlData::new,
            (tag, registries) -> load(tag), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    //?}
    private final Map<BlockPos, Reader> readers = new HashMap<>();
    private final Map<Long, Set<BlockPos>> readersByChunk = new HashMap<>();
    private final Map<AccessDoorTarget, Door> doors = new HashMap<>();
    private final Map<BlockPos, Set<Door>> linksByPart = new HashMap<>();
    private final Map<Long, Set<Door>> linksByChunk = new HashMap<>(), waiting = new HashMap<>();
    private final Set<Door> changedFrames = new HashSet<>();
    private final Set<Long> readyChunks = new HashSet<>(), changedChunks = new HashSet<>();
    private final TreeSet<Door> scheduled = new TreeSet<>(Comparator.comparingLong((Door door) -> door.due)
            .thenComparingLong(door -> door.order));
    private ServerLevel level;
    private long nextOrder;

    public enum Permission { ADMINISTRATOR, DOOR_ACCESS }
    public enum LinkStatus { UNLINKED, LINKED, UNLOADED, STALE }
    public record Card(UUID id, String name, Permission permission) { }
    private enum Work { CLOSE, RESTORE, REPAIR }
    private static final long[] NO_LOCKS = {};

    static final class Door {
        final AccessDoorTarget target;
        final Set<Reader> readers = new HashSet<>();
        final Set<Long> chunks = new HashSet<>();
        final long order;
        boolean verified, initialized, secured;
        long due;
        Work work;
        Long waitingFor;

        Door(AccessDoorTarget target, long order) {
            this.target = target;
            this.order = order;
            for (BlockPos pos : target.parts()) chunks.add(chunk(pos));
        }
    }

    public static AccessControlData get(ServerLevel level) {
        //? if >=1.21 {
        AccessControlData data = level.getDataStorage().computeIfAbsent(FACTORY, NAME);
        //?} else {
        /*AccessControlData data = level.getDataStorage().computeIfAbsent(AccessControlData::load, AccessControlData::new, NAME);
        *///?}
        data.level = level;
        return data;
    }

    public Reader existing(BlockPos pos) { return readers.get(pos); }

    public Reader reader(BlockPos pos) {
        return readers.computeIfAbsent(pos.immutable(), key -> {
            readersByChunk.computeIfAbsent(chunk(key), ignored -> new HashSet<>()).add(key);
            return new Reader(this, key);
        });
    }

    public void removeReader(BlockPos pos) {
        Reader removed = readers.remove(pos);
        if (removed == null) return;
        removeFrom(readersByChunk, chunk(pos), pos);
        unindex(removed);
        if (removed.configured || removed.door != null) setDirty();
    }

    public void doorRemoved(BlockPos pos) {
        Set<Door> linked = linksByPart.get(pos);
        if (linked != null) for (Door door : List.copyOf(linked)) invalidate(door);
    }

    private void index(Reader reader) {
        if (reader.door == null || reader.invalidated) return;
        Door door = door(reader.door);
        reader.link = door;
        door.readers.add(reader);
        updateSecurity(door);
    }

    private void unindex(Reader reader) {
        Door door = reader.link;
        if (door == null) return;
        reader.link = null;
        door.readers.remove(reader);
        updateSecurity(door);
        if (door.readers.isEmpty() && door.work == null) discard(door);
    }

    private Door door(AccessDoorTarget target) {
        return doors.computeIfAbsent(target, key -> {
            Door door = new Door(key, nextOrder++);
            for (BlockPos part : key.parts()) linksByPart.computeIfAbsent(part, ignored -> new HashSet<>()).add(door);
            for (long chunk : door.chunks) linksByChunk.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(door);
            return door;
        });
    }

    private static <K, V> void removeFrom(Map<K, Set<V>> index, K key, V value) {
        Set<V> set = index.get(key);
        if (set != null && set.remove(value) && set.isEmpty()) index.remove(key);
    }

    private void discard(Door door) {
        doors.remove(door.target, door);
        for (BlockPos part : door.target.parts()) removeFrom(linksByPart, part, door);
        for (long chunk : door.chunks) removeFrom(linksByChunk, chunk, door);
        changedFrames.remove(door);
        cancel(door);
    }

    private void invalidate(Door door) {
        if (door.work == Work.REPAIR && door.readers.isEmpty()) return;
        boolean repair = (door.secured || door.work == Work.RESTORE) && AccessDoorMotion.nativePower(door.target);
        if (door.secured) changedChunks.addAll(door.chunks);
        for (Reader reader : door.readers) {
            reader.link = null;
            reader.invalidated = true;
            reader.changed();
        }
        door.readers.clear();
        door.secured = false;
        door.verified = false;
        changedFrames.remove(door);
        if (repair && level != null) plan(door, Work.REPAIR, level.getGameTime());
        else discard(door);
        setDirty();
    }

    private void updateSecurity(Door door) {
        if (door == null) return;
        boolean secured = door.readers.stream().anyMatch(reader -> reader.configured && reader.enabled);
        if (door.secured == secured) return;
        door.secured = secured;
        if (level == null) return;
        changedChunks.addAll(door.chunks);
        door.initialized = true;
        door.verified = false;
        if (secured) {
            plan(door, Work.CLOSE, level.getGameTime());
            for (BlockPos part : door.target.parts()) {
                BlockState state = AccessDoorTarget.loadedState(level, part);
                if (state != null && state.getBlock() instanceof SlidingEntranceBlock
                        && state.getValue(SlidingEntranceBlock.HALF) == DoubleBlockHalf.LOWER) SlidingDoorSensors.changed(level, part);
            }
        } else if (AccessDoorMotion.nativePower(door.target)) plan(door, Work.RESTORE, level.getGameTime());
        else cancel(door);
    }

    static long chunk(BlockPos pos) { return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4); }

    Door securedAt(BlockPos pos, BlockState state) {
        Set<Door> indexed = linksByPart.get(pos);
        if (indexed != null) for (Door door : indexed) {
            if (door.secured && door.target.matches(state)) return door;
        }
        return null;
    }

    long[] locksInChunk(long chunk) {
        Set<Door> indexed = linksByChunk.get(chunk);
        if (indexed == null) return NO_LOCKS;
        var positions = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for (Door door : indexed) if (door.secured) for (BlockPos pos : door.target.parts()) {
            if (chunk(pos) == chunk) positions.add(pos.asLong());
        }
        return positions.toLongArray();
    }

    public void topologyChanged(BlockPos pos) {
        markFrame(pos);
        for (var direction : Direction.values()) markFrame(pos.relative(direction));
    }

    private void markFrame(BlockPos pos) {
        Set<Door> linked = linksByPart.get(pos);
        if (linked != null) for (Door door : linked) if (door.work != Work.REPAIR) {
            door.verified = false;
            changedFrames.add(door);
        }
    }

    public void chunkReady(long chunk) {
        if (readersByChunk.containsKey(chunk) || linksByChunk.containsKey(chunk) || waiting.containsKey(chunk)) readyChunks.add(chunk);
    }

    private BlockPos verify(Door door) {
        if (door.verified) return null;
        AccessDoorTarget.Resolution resolved = AccessDoorTarget.resolve(level, door.target.root());
        if (resolved.unloaded()) return resolved.missing();
        if (door.target.equals(resolved.target())) door.verified = true;
        else invalidate(door);
        return null;
    }

    private void cancel(Door door) {
        scheduled.remove(door);
        if (door.waitingFor != null) removeFrom(waiting, door.waitingFor, door);
        door.waitingFor = null;
        door.work = null;
    }

    private void plan(Door door, Work work, long due) {
        cancel(door);
        door.work = work;
        door.due = due;
        scheduled.add(door);
        setDirty();
    }

    private void waitFor(Door door, BlockPos position) {
        scheduled.remove(door);
        if (door.waitingFor != null) removeFrom(waiting, door.waitingFor, door);
        door.waitingFor = chunk(position);
        waiting.computeIfAbsent(door.waitingFor, ignored -> new HashSet<>()).add(door);
    }

    void hold(Door door, long until) {
        if (door.secured && doors.get(door.target) == door)
            plan(door, Work.CLOSE, door.work == Work.CLOSE ? Math.max(door.due, until) : until);
    }

    boolean ready(Door door) {
        return verify(door) == null && door.verified && AccessDoorMotion.missing(level, door.target, door.chunks) == null;
    }

    void tick() {
        long now = level.getGameTime();
        if (readyChunks.isEmpty() && changedFrames.isEmpty() && changedChunks.isEmpty()
                && (scheduled.isEmpty() || scheduled.first().due > now)) return;
        for (long chunk : readyChunks) {
            Set<BlockPos> readerPositions = readersByChunk.get(chunk);
            if (readerPositions != null) for (BlockPos pos : List.copyOf(readerPositions)) {
                BlockState state = AccessDoorTarget.loadedState(level, pos);
                if (state != null && !(state.getBlock() instanceof OfficeBadgeReaderBlock)) removeReader(pos);
            }
            Set<Door> wake = waiting.remove(chunk);
            if (wake != null) for (Door door : wake) {
                door.waitingFor = null;
                if (door.work != null) scheduled.add(door);
                else changedFrames.add(door);
            }
            Set<Door> loaded = linksByChunk.get(chunk);
            if (loaded != null) for (Door door : loaded) {
                if (!door.initialized && door.secured && door.work == null) plan(door, Work.CLOSE, now);
                door.initialized = true;
            }
        }
        readyChunks.clear();
        if (!changedFrames.isEmpty()) {
            List<Door> checks = List.copyOf(changedFrames);
            changedFrames.clear();
            for (Door door : checks) if (doors.get(door.target) == door) {
                BlockPos missing = verify(door);
                if (missing != null && door.work == null) waitFor(door, missing);
            }
        }
        for (int checked = 0; checked < 32 && !scheduled.isEmpty() && scheduled.first().due <= now; checked++) {
            Door door = scheduled.pollFirst();
            boolean finished;
            if (door.work == Work.REPAIR) {
                AccessDoorMotion.Repair repair = AccessDoorMotion.restoreSurvivors(level, door.target);
                if (repair.missing() != null) { waitFor(door, repair.missing()); continue; }
                finished = repair.finished();
            } else {
                BlockPos missing = verify(door);
                if (missing == null && door.verified) missing = AccessDoorMotion.missing(level, door.target, door.chunks);
                if (missing != null) { waitFor(door, missing); continue; }
                if (!door.verified) continue;
                finished = door.work == Work.RESTORE ? AccessDoorMotion.restorePower(level, door.target)
                        : AccessDoorMotion.closeSafely(level, door.target);
            }
            if (doors.get(door.target) != door || door.work == null) continue;
            if (finished) {
                cancel(door);
                if (door.readers.isEmpty()) discard(door);
                setDirty();
            } else plan(door, door.work, now + 10);
        }
        if (!changedChunks.isEmpty()) {
            for (long chunk : changedChunks) AccessControlNetworking.syncLocks(level, chunk, locksInChunk(chunk));
            changedChunks.clear();
        }
    }

    public static boolean validDuration(int seconds) { return seconds >= 1 && seconds <= MAX_SECONDS; }

    public static final class Reader {
        private final AccessControlData data;
        private final BlockPos pos;
        private final Map<UUID, Card> cards = new LinkedHashMap<>();
        private int admins, seconds = DEFAULT_SECONDS;
        private boolean enabled = true, configured;
        private long enrollmentUntil, revision;
        private AccessDoorTarget door;
        private Door link;
        private boolean invalidated;
        private long nextFeedback;

        private Reader(AccessControlData data, BlockPos pos) { this.data = data; this.pos = pos; }

        public BlockPos pos() { return pos; }
        public boolean configured() { return configured; }
        public boolean enabled() { return enabled; }
        public int seconds() { return seconds; }
        public int cardCount() { return cards.size(); }
        public int administratorCount() { return admins; }
        public long revision() { return revision; }
        public AccessDoorTarget door() { return door; }
        Door link() { return link; }
        public Permission permission(UUID id) { Card card = cards.get(id); return card == null ? null : card.permission(); }
        public boolean administrator(UUID id) { return permission(id) == Permission.ADMINISTRATOR; }
        public boolean authorized(UUID id) { return cards.containsKey(id); }
        boolean feedbackReady(long now) {
            if (now < nextFeedback) return false;
            nextFeedback = now + 6;
            return true;
        }
        public List<Card> page(int offset, int size) { return cards.values().stream().skip(offset).limit(size).toList(); }

        public int enrollmentRemaining(long now) {
            return (int) Math.min(ENROLLMENT_TICKS, Math.max(0, enrollmentUntil - now));
        }

        public Permission enroll(UUID id, String name, long now) {
            if (id == null || !AccessCardItem.validName(name) || authorized(id)) return null;
            if (configured && enrollmentRemaining(now) == 0) return null;
            Permission permission = configured ? Permission.DOOR_ACCESS : Permission.ADMINISTRATOR;
            configured = true;
            putCard(new Card(id, name, permission));
            data.updateSecurity(link);
            changed();
            return permission;
        }

        public void refreshName(UUID id, String name) {
            Card card = cards.get(id);
            if (card == null || card.name().equals(name) || !AccessCardItem.validName(name)) return;
            putCard(new Card(id, name, card.permission()));
            changed();
        }

        public boolean setPermission(UUID id, Permission permission) {
            Card card = cards.get(id);
            if (card == null || card.permission() == permission || lastAdministrator(card)) return false;
            putCard(new Card(id, card.name(), permission));
            changed();
            return true;
        }

        public boolean removeCard(UUID id) {
            Card card = cards.get(id);
            if (card == null || lastAdministrator(card)) return false;
            cards.remove(id);
            if (card.permission() == Permission.ADMINISTRATOR) admins--;
            changed();
            return true;
        }

        private boolean lastAdministrator(Card card) { return admins == 1 && card.permission() == Permission.ADMINISTRATOR; }

        private void putCard(Card card) {
            Card previous = cards.put(card.id(), card);
            if (previous != null && previous.permission() == Permission.ADMINISTRATOR) admins--;
            if (card.permission() == Permission.ADMINISTRATOR) admins++;
        }

        public void setEnrollment(boolean active, long now) {
            long until = active ? now + ENROLLMENT_TICKS : 0;
            if (until == enrollmentUntil) return;
            enrollmentUntil = until;
            changed();
        }

        public void setEnabled(boolean value) {
            if (enabled == value) return;
            enabled = value;
            data.updateSecurity(link);
            changed();
        }

        public void setDuration(int value) {
            if (!validDuration(value) || seconds == value) return;
            seconds = value;
            changed();
        }

        public boolean setDoor(AccessDoorTarget target) {
            if (!invalidated && java.util.Objects.equals(door, target)) return false;
            data.unindex(this);
            door = target;
            invalidated = false;
            data.index(this);
            changed();
            return true;
        }

        public LinkStatus linkStatus(ServerLevel level) {
            if (door == null) return LinkStatus.UNLINKED;
            if (invalidated) return LinkStatus.STALE;
            if (link == null) return LinkStatus.STALE;
            BlockPos missing = data.verify(link);
            if (invalidated) return LinkStatus.STALE;
            if (missing != null || AccessDoorMotion.missing(level, door, link.chunks) != null) return LinkStatus.UNLOADED;
            return LinkStatus.LINKED;
        }

        private void changed() { revision++; data.setDirty(); }
    }

    private static AccessControlData load(CompoundTag tag) {
        AccessControlData data = new AccessControlData();
        ListTag saved = tag.getList("readers", Tag.TAG_COMPOUND);
        for (int i = 0; i < saved.size(); i++) {
            CompoundTag entry = saved.getCompound(i);
            if (!entry.contains("pos", Tag.TAG_LONG)) continue;
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            if (data.readers.containsKey(pos)) continue;
            Reader reader = data.reader(pos);
            reader.configured = entry.getBoolean("configured");
            reader.enabled = !entry.contains("enabled") || entry.getBoolean("enabled");
            int seconds = entry.getInt("seconds");
            if (validDuration(seconds)) reader.seconds = seconds;
            reader.enrollmentUntil = Math.max(0, entry.getLong("enrollment_until"));
            reader.door = AccessDoorTarget.load(entry.getCompound("door"));
            reader.invalidated = reader.door != null && entry.getBoolean("link_invalidated");
            ListTag cards = entry.getList("cards", Tag.TAG_COMPOUND);
            for (int row = 0; row < cards.size(); row++) {
                CompoundTag card = cards.getCompound(row);
                if (!card.hasUUID("id")) continue;
                String name = card.getString("name");
                reader.putCard(new Card(card.getUUID("id"), AccessCardItem.validName(name) ? name : "",
                        card.getBoolean("administrator") ? Permission.ADMINISTRATOR : Permission.DOOR_ACCESS));
            }
            reader.configured |= !reader.cards.isEmpty();
            data.index(reader);
        }
        ListTag jobs = tag.getList("door_work", Tag.TAG_COMPOUND);
        for (int i = 0; i < jobs.size(); i++) {
            CompoundTag entry = jobs.getCompound(i);
            AccessDoorTarget target = AccessDoorTarget.load(entry.getCompound("door"));
            if (target == null) continue;
            Work work = entry.getBoolean("repair") ? Work.REPAIR : entry.getBoolean("restore") ? Work.RESTORE : Work.CLOSE;
            Door door = data.doors.get(target);
            if ((work == Work.CLOSE) != (door != null && door.secured)) continue;
            if (door == null) door = data.door(target);
            data.plan(door, work, Math.max(0L, entry.getLong("due")));
        }
        data.setDirty(false);
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag
            //? if >=1.21 {
            , HolderLookup.Provider registries
            //?}
    ) {
        ListTag saved = new ListTag();
        for (Reader reader : readers.values()) {
            if (!reader.configured && reader.door == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", reader.pos.asLong());
            entry.putBoolean("configured", reader.configured);
            entry.putBoolean("enabled", reader.enabled);
            entry.putInt("seconds", reader.seconds);
            entry.putLong("enrollment_until", reader.enrollmentUntil);
            if (reader.door != null) {
                entry.put("door", reader.door.save());
                entry.putBoolean("link_invalidated", reader.invalidated);
            }
            ListTag cards = new ListTag();
            for (Card card : reader.cards.values()) {
                CompoundTag row = new CompoundTag();
                row.putUUID("id", card.id());
                row.putString("name", card.name());
                row.putBoolean("administrator", card.permission() == Permission.ADMINISTRATOR);
                cards.add(row);
            }
            entry.put("cards", cards);
            saved.add(entry);
        }
        tag.put("readers", saved);
        ListTag jobs = new ListTag();
        for (Door door : doors.values()) if (door.work != null) {
            CompoundTag entry = new CompoundTag();
            entry.put("door", door.target.save());
            entry.putBoolean("restore", door.work == Work.RESTORE);
            entry.putBoolean("repair", door.work == Work.REPAIR);
            entry.putLong("due", door.due);
            jobs.add(entry);
        }
        tag.put("door_work", jobs);
        return tag;
    }
}
