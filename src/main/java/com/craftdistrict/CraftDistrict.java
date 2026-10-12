package com.craftdistrict;

import com.craftdistrict.registry.CraftDistrictBlocks;
import com.craftdistrict.registry.CraftDistrictCreativeTabs;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class CraftDistrict {
    public static final String MOD_ID = "craftdistrict";
    private static final Logger LOGGER = LogUtils.getLogger();

    private CraftDistrict() {
    }

    public static void init() {
        CraftDistrictBlocks.register();
        CraftDistrictCreativeTabs.register();
        LOGGER.info("CraftDistrict initialized");
    }
}
