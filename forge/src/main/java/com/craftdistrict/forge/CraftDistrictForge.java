package com.craftdistrict.forge;

import com.craftdistrict.CraftDistrict;
import net.minecraftforge.fml.common.Mod;

@Mod(CraftDistrict.MOD_ID)
public final class CraftDistrictForge {
    public CraftDistrictForge() {
        CraftDistrict.init();
    }
}
