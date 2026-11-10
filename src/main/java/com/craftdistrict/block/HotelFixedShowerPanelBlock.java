package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
//?}

/** Fixed modules share corner/row geometry, without door pairing or opening interactions. */
public final class HotelFixedShowerPanelBlock extends HotelShowerEnclosureBlock {
    //? if >=1.21 {
    public static final MapCodec<HotelFixedShowerPanelBlock> CODEC = simpleCodec(HotelFixedShowerPanelBlock::new);
    //?}
    public HotelFixedShowerPanelBlock(Properties properties) { super(properties); }
    @Override protected boolean sliding() { return false; }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
