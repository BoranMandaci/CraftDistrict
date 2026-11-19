package com.craftdistrict.block;

import com.craftdistrict.registry.CraftDistrictDoors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class RotaryEntranceBlockEntity extends BlockEntity {
    private static final int REVOLUTION_TICKS = 200;
    private AABB bounds;

    public RotaryEntranceBlockEntity(BlockPos pos, BlockState state) {
        super(CraftDistrictDoors.ROTARY.get(), pos, state);
    }

    public static float revolvingAngle(long gameTime, float partialTick) {
        return (Math.floorMod(gameTime, REVOLUTION_TICKS) + partialTick) * (360F / REVOLUTION_TICKS);
    }

    // The controller occupies the middle cell of the 3x3x3 door.
    public AABB getRenderBoundingBox() {
        if (bounds == null) bounds = new AABB(worldPosition).inflate(1);
        return bounds;
    }
}
