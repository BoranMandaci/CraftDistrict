package com.craftdistrict.forge;

import com.craftdistrict.CraftDistrict;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CraftDistrict.MOD_ID)
public final class CraftDistrictForge {
    public CraftDistrictForge(FMLJavaModLoadingContext context) {
        EventBuses.registerModEventBus(CraftDistrict.MOD_ID, context.getModEventBus());
        CraftDistrict.init();
    }
}
