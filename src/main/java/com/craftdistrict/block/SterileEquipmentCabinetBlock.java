package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
//?}

/** Sterile cabinets use the same complete-pair joining rules as medical supply cabinets. */
public final class SterileEquipmentCabinetBlock extends MedicalSupplyCabinetBlock {
    //? if >=1.21 {
    public static final MapCodec<SterileEquipmentCabinetBlock> CODEC = simpleCodec(SterileEquipmentCabinetBlock::new);
    //?}
    public SterileEquipmentCabinetBlock(Properties properties) { super(Design.STERILE_CABINET, properties); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
