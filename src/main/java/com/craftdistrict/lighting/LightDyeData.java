package com.craftdistrict.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
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
    private final Long2ObjectOpenHashMap<Long2ByteOpenHashMap> chunks = new Long2ObjectOpenHashMap<>();

    public static LightDyeData get(ServerLevel level) {
        //? if >=1.21 {
        return level.getDataStorage().computeIfAbsent(new Factory<>(LightDyeData::new,
                (tag, registries) -> load(tag), DataFixTypes.SAVED_DATA_COMMAND_STORAGE), NAME);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(LightDyeData::load, LightDyeData::new, NAME);
        *///?}
    }

    private static long chunk(BlockPos pos) { return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4); }

    public int color(BlockPos pos) {
        Long2ByteOpenHashMap values = chunks.get(chunk(pos));
        return values == null ? -1 : values.get(pos.asLong());
    }

    public boolean put(BlockPos pos, int color) {
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

    public Long2ByteOpenHashMap snapshot(ChunkPos pos) {
        Long2ByteOpenHashMap values = chunks.get(pos.toLong());
        return values == null ? new Long2ByteOpenHashMap() : values.clone();
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
        return tag;
    }
}
