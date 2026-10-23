package com.craftdistrict.registry;

import com.craftdistrict.CraftDistrict;
import com.craftdistrict.entity.SeatEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class CraftDistrictEntities {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(CraftDistrict.MOD_ID,Registries.ENTITY_TYPE);
    public static final RegistrySupplier<EntityType<SeatEntity>> SEAT=TYPES.register("seat",()->EntityType.Builder
            .of(SeatEntity::new,MobCategory.MISC).sized(.01F,.01F).clientTrackingRange(8).updateInterval(20)
            .noSave().noSummon().fireImmune().build("craftdistrict:seat"));
    private CraftDistrictEntities() { }
    public static void register() { TYPES.register(); }
}
