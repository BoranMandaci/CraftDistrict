package com.craftdistrict;

import com.craftdistrict.access.AccessControl;
import com.craftdistrict.access.AccessDoorSecurity;
import com.craftdistrict.client.CraftDistrictClient;
import com.craftdistrict.registry.CraftDistrictBlocks;
import com.craftdistrict.registry.CraftDistrictCreativeTabs;
import com.craftdistrict.registry.CraftDistrictEntities;
import com.craftdistrict.registry.CraftDistrictItems;
import com.craftdistrict.registry.CraftDistrictAssembly;
import com.craftdistrict.registry.CraftDistrictRoomNumbers;
import com.craftdistrict.network.RoomNumberNetworking;
import com.craftdistrict.network.AssemblyNetworking;
import com.craftdistrict.network.LightDyeNetworking;
import com.craftdistrict.network.LightColorNetworking;
import com.craftdistrict.network.OfficeTurnstileNetworking;
import com.craftdistrict.network.AccessControlNetworking;
import com.craftdistrict.registry.CraftDistrictAccessControl;
import com.craftdistrict.lighting.LightProximityData;
import com.craftdistrict.entity.Seating;
import com.craftdistrict.entity.MedicalResting;
import com.craftdistrict.block.PoliceOfficeSecurityBlock;
import com.craftdistrict.block.SecurityScannerBlock;
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
        CraftDistrictItems.register();
        CraftDistrictAccessControl.register();
        AccessControl.register();
        AccessDoorSecurity.register();
        PoliceOfficeSecurityBlock.registerLegacyCleanup();
        SecurityScannerBlock.registerPassageTracking();
        CraftDistrictAssembly.register();
        CraftDistrictRoomNumbers.register();
        com.craftdistrict.registry.CraftDistrictDoors.register();
        com.craftdistrict.door.SlidingDoorSensors.register();
        RoomNumberNetworking.register();
        AssemblyNetworking.register();
        LightDyeNetworking.register();
        LightColorNetworking.register();
        OfficeTurnstileNetworking.register();
        AccessControlNetworking.register();
        LightProximityData.register();
        com.craftdistrict.lighting.LedLightGroups.register();
        com.craftdistrict.block.MovingDoorGroups.register();
        CraftDistrictEntities.register();
        Seating.init();
        MedicalResting.init();
        CraftDistrictCreativeTabs.register();
        EnvExecutor.runInEnv(Env.CLIENT, () -> CraftDistrictClient::init);
        LOGGER.info("CraftDistrict initialized");
    }
}
