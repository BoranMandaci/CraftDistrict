package com.craftdistrict;

import com.craftdistrict.client.CraftDistrictClient;
import com.craftdistrict.registry.CraftDistrictBlocks;
import com.craftdistrict.registry.CraftDistrictCreativeTabs;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;

public final class CraftDistrict {
    public static final String MOD_ID = "craftdistrict";
    private static final Logger LOGGER = LogUtils.getLogger();

    private CraftDistrict() {
    }

    public static void init() {
        CraftDistrictBlocks.register();
        CraftDistrictCreativeTabs.register();
        EnvExecutor.runInEnv(Env.CLIENT, () -> CraftDistrictClient::init);
        LOGGER.info("CraftDistrict initialized");
    }
}
