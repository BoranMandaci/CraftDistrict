package com.craftdistrict.block;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.core.BlockPos;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Only edited ducts need storage; effective connections are synchronized as ordinary blockstates. */
public final class DuctConnectionData extends SavedData {
    private static final String NAME = "craftdistrict_duct_connections";
    //? if >=1.21 {
    private static final Factory<DuctConnectionData> FACTORY = new Factory<>(DuctConnectionData::new,
            (tag, registries) -> load(tag), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    //?}
    private final Long2ByteOpenHashMap disabled = new Long2ByteOpenHashMap();

    public static DuctConnectionData get(ServerLevel level) {
        //? if >=1.21 {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(DuctConnectionData::load, DuctConnectionData::new, NAME);
        *///?}
    }

    public int mask(BlockPos pos) { return disabled.get(pos.asLong()); }
    public void put(BlockPos pos, int mask) {
        if (mask < 0 || mask > 63) throw new IllegalArgumentException("Invalid duct connection mask");
        if (mask(pos) == mask) return;
        if (mask == 0) disabled.remove(pos.asLong());
        else disabled.put(pos.asLong(), (byte)mask);
        setDirty();
    }

    public static DuctConnectionData load(CompoundTag tag) {
        DuctConnectionData data = new DuctConnectionData();
        ListTag entries = tag.getList("connections", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            int mask = entry.getByte("disabled");
            if (entry.contains("pos", Tag.TAG_LONG) && mask > 0 && mask <= 63)
                data.put(BlockPos.of(entry.getLong("pos")), mask);
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
        for (var value : disabled.long2ByteEntrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", value.getLongKey()); entry.putByte("disabled", value.getByteValue());
            entries.add(entry);
        }
        tag.put("connections", entries);
        return tag;
    }
}
