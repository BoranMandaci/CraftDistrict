package com.craftdistrict.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Sparse, per-dimension dye palette. No ticking, block entities or per-color block states. */
public final class LightDyeData extends SavedData {
    private static final String NAME = "craftdistrict_light_dyes";
    //? if >=1.21 {
    private static final Factory<LightDyeData> FACTORY = new Factory<>(LightDyeData::new,
            (tag, registries) -> load(tag), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    //?}
    private final Long2ObjectOpenHashMap<Long2ByteOpenHashMap> chunks = new Long2ObjectOpenHashMap<>();
    // A merged/inherited display color does not create a refundable dye that was never paid.
    private final Long2ByteOpenHashMap credits = new Long2ByteOpenHashMap();
    public LightDyeData() { credits.defaultReturnValue((byte)-2); }

    public static LightDyeData get(ServerLevel level) {
        //? if >=1.21 {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(LightDyeData::load, LightDyeData::new, NAME);
        *///?}
    }

    private static long chunk(BlockPos pos) { return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4); }

    public int color(BlockPos pos) {
        Long2ByteOpenHashMap values = chunks.get(chunk(pos));
        return values == null ? -1 : values.get(pos.asLong());
    }

    public int appliedDye(BlockPos pos) {
        int credit = credits.get(pos.asLong());
        return credit == -2 ? color(pos) : credit;
    }
    public boolean inherit(BlockPos pos, int color) {
        int paid = appliedDye(pos);
        boolean changed = display(pos, color);
        if (paid != color) credits.put(pos.asLong(), (byte)paid);
        else credits.remove(pos.asLong());
        if (changed) setDirty();
        return changed;
    }
    public boolean put(BlockPos pos, int color) {
        if (color(pos) == color && (color != -1 || !credits.containsKey(pos.asLong()))) return false;
        credits.remove(pos.asLong());
        boolean changed = display(pos, color);
        setDirty(); return changed;
    }
    /** Free Creative fabric colors never create a Survival refund credit. */
    public void putUnpaid(BlockPos pos, int color) {
        put(pos, color);
        credits.put(pos.asLong(), (byte)-1);
        setDirty();
    }
    private boolean display(BlockPos pos, int color) {
        if (color < -1 || color > 15) throw new IllegalArgumentException("Invalid light dye");
        if (color(pos) == color) return false;
        long chunk = chunk(pos);
        if (color == -1) {
            Long2ByteOpenHashMap values = chunks.get(chunk);
            if (values != null) {
                values.remove(pos.asLong());
                if (values.isEmpty()) chunks.remove(chunk);
            }
        } else {
            Long2ByteOpenHashMap values = chunks.computeIfAbsent(chunk, key -> {
                Long2ByteOpenHashMap map = new Long2ByteOpenHashMap();
                map.defaultReturnValue((byte)-1);
                return map;
            });
            values.put(pos.asLong(), (byte)color);
        }
        setDirty();
        return true;
    }

    /** Validate the whole per-fixture/per-panel dye cost before any settings are committed. */
    public Payment payment(net.minecraft.world.entity.player.Player player, java.util.List<BlockPos> parts,
                           int selected, boolean perPanel, boolean refund) {
        int count = perPanel ? parts.size() : 1;
        net.minecraft.world.item.DyeItem dye = net.minecraft.world.item.DyeItem.byColor(net.minecraft.world.item.DyeColor.byId(selected));
        int available = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i); if (stack.is(dye)) available += stack.getCount();
        }
        if (!player.getAbilities().instabuild && available < count) return null;
        int[] refunds = new int[16];
        if (refund && !player.getAbilities().instabuild) for (int i = 0; i < (perPanel ? parts.size() : 1); i++) {
            int previous = appliedDye(parts.get(i)); if (previous >= 0) refunds[previous]++;
        }
        return new Payment(java.util.List.copyOf(parts), selected, count, refunds);
    }
    public final class Payment {
        private final java.util.List<BlockPos> parts;
        private final int selected, count;
        private final int[] refunds;
        private boolean committed;
        private Payment(java.util.List<BlockPos> parts, int selected, int count, int[] refunds) {
            this.parts = parts; this.selected = selected; this.count = count; this.refunds = refunds;
        }
        public void commit(net.minecraft.world.entity.player.Player player) {
            if (committed) throw new IllegalStateException("Dye payment already committed");
            committed = true;
            var inventory = player.getInventory();
            if (!player.getAbilities().instabuild) {
                var dye = net.minecraft.world.item.DyeItem.byColor(net.minecraft.world.item.DyeColor.byId(selected));
                int remaining = count;
                for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
                    var stack = inventory.getItem(i); if (!stack.is(dye)) continue;
                    int used = Math.min(remaining, stack.getCount()); stack.shrink(used); remaining -= used;
                }
            }
            for (BlockPos pos : parts) put(pos, selected);
            if (!player.getAbilities().instabuild) {
                for (int color = 0; color < 16; color++) for (int remaining = refunds[color]; remaining > 0; remaining -= 64) {
                    var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.DyeItem.byColor(net.minecraft.world.item.DyeColor.byId(color)), Math.min(64, remaining));
                    inventory.add(stack); if (!stack.isEmpty()) player.drop(stack, false);
                }
                inventory.setChanged(); player.inventoryMenu.broadcastChanges();
            }
        }
    }

    public Long2ByteMap snapshot(ChunkPos pos) {
        Long2ByteOpenHashMap values = chunks.get(pos.toLong());
        return values == null ? Long2ByteMaps.EMPTY_MAP : values.clone();
    }

    public static LightDyeData load(CompoundTag tag) {
        LightDyeData data = new LightDyeData();
        ListTag entries = tag.getList("colors", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            int color = entry.getByte("color");
            if (entry.contains("pos", Tag.TAG_LONG) && color >= 0 && color <= 15)
                data.put(BlockPos.of(entry.getLong("pos")), color);
        }
        ListTag applied = tag.getList("applied", Tag.TAG_COMPOUND);
        for (int i = 0; i < applied.size(); i++) {
            CompoundTag entry = applied.getCompound(i); int credit = entry.getByte("dye");
            if (entry.contains("pos", Tag.TAG_LONG) && credit >= -1 && credit <= 15)
                data.credits.put(entry.getLong("pos"), (byte)credit);
        }
        data.setDirty(false);
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag
            //? if >=1.21 {
            , HolderLookup.Provider registries
            //?}
    ) {
        ListTag entries = new ListTag();
        for (Long2ByteOpenHashMap chunk : chunks.values()) for (var value : chunk.long2ByteEntrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", value.getLongKey());
            entry.putByte("color", value.getByteValue());
            entries.add(entry);
        }
        tag.put("colors", entries);
        ListTag applied = new ListTag();
        for (var entry : credits.long2ByteEntrySet()) {
            CompoundTag value = new CompoundTag(); value.putLong("pos", entry.getLongKey()); value.putByte("dye", entry.getByteValue()); applied.add(value);
        }
        tag.put("applied", applied);
        return tag;
    }
}
