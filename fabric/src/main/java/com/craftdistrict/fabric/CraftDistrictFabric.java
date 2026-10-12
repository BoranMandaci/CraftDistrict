package com.craftdistrict.fabric;

import com.craftdistrict.CraftDistrict;
import net.fabricmc.api.ModInitializer;

public final class CraftDistrictFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CraftDistrict.init();
    }
}
