package com.craftdistrict;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class CraftDistrict {
    public static final String MOD_ID = "craftdistrict";
    private static final Logger LOGGER = LogUtils.getLogger();

    private CraftDistrict() {
    }

    public static void init() {
        LOGGER.info("CraftDistrict initialized");
    }
}
