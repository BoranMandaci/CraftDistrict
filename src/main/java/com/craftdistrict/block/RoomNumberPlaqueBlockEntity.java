package com.craftdistrict.block;

import com.craftdistrict.registry.CraftDistrictRoomNumbers;
import net.minecraft.core.BlockPos;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One small sign record, synchronized only when edited; no ticker or inventory. */
public final class RoomNumberPlaqueBlockEntity extends BlockEntity {
    private String number;
    private boolean reversed;
    public RoomNumberPlaqueBlockEntity(BlockPos pos, BlockState state) {
        super(CraftDistrictRoomNumbers.PLAQUE.get(), pos, state); number = ((RoomNumberPlaqueBlock) state.getBlock()).defaultNumber();
    }
    public String number() { return number; }
    public boolean reversed() { return reversed; }
    public int kind() { return ((RoomNumberPlaqueBlock) getBlockState().getBlock()).kind(); }
    public static boolean valid(String text) {
        if (text == null || text.isEmpty() || text.length() > 4) return false;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) < '0' || text.charAt(i) > '9') return false;
        return true;
    }
    public static boolean valid(int kind, String text) {
        if (kind == 0) return valid(text);
        if (text == null || text.isEmpty() || text.length() > 9) return false;
        if (kind == 1) return valid(text.startsWith("-") ? text.substring(1) : text);
        if (kind != 2) return false;
        int split = text.indexOf('-');
        return split < 0 ? valid(text) : valid(text.substring(0, split)) && valid(text.substring(split + 1));
    }
    public static boolean draft(int kind, String text) {
        if (text.isEmpty()) return true;
        if (kind == 0) return valid(text);
        if (kind == 1) return text.equals("-") || valid(kind, text);
        if (kind != 2 || text.length() > 9) return false;
        int split = text.indexOf('-');
        return split < 0 ? valid(text) : valid(text.substring(0, split)) && (split == text.length() - 1 || valid(text.substring(split + 1)));
    }
    public void setNumber(String text) {
        setSign(text, reversed);
    }
    public void setSign(String text, boolean arrowReversed) {
        if (level == null || level.isClientSide || !valid(kind(), text) || text.equals(number) && reversed == arrowReversed) return;
        number = text; reversed = kind() != 0 && arrowReversed; setChanged();
        // Editing changes only this record; both legacy CONFIGURED states use the same body and renderer.
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override
    //? if >=1.21 {
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
    //?} else {
    /*protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    *///?}
        tag.putString("RoomNumber", number);
        tag.putBoolean("ArrowReversed", reversed);
    }
    @Override
    //? if >=1.21 {
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
    //?} else {
    /*public void load(CompoundTag tag) {
        super.load(tag);
    *///?}
        String loaded = tag.getString("RoomNumber");
        number = valid(kind(), loaded) ? loaded : ((RoomNumberPlaqueBlock) getBlockState().getBlock()).defaultNumber();
        reversed = kind() != 0 && tag.getBoolean("ArrowReversed");
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(
            //? if >=1.21 {
            HolderLookup.Provider lookup
            //?}
    ) {
        CompoundTag tag = new CompoundTag(); tag.putString("RoomNumber", number); tag.putBoolean("ArrowReversed", reversed); return tag;
    }
}
