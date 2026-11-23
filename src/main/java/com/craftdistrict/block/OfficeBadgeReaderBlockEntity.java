package com.craftdistrict.block;

import com.craftdistrict.registry.CraftDistrictAccessControl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Transient client feedback for the existing indicator lenses. */
public final class OfficeBadgeReaderBlockEntity extends BlockEntity {
    private static final int PULSE_TICKS = 3;
    private static final int DURATION = PULSE_TICKS * 6;
    private long started = -DURATION;
    private int indicator;

    public OfficeBadgeReaderBlockEntity(BlockPos pos, BlockState state) {
        super(CraftDistrictAccessControl.BADGE_INDICATOR.get(), pos, state);
    }

    void blink(long time, boolean granted) {
        started = time;
        indicator = granted ? 1 : 2;
    }

    public int visibleIndicator(long time) {
        long age = time - started;
        return age >= 0 && age < DURATION && age % (PULSE_TICKS * 2) < PULSE_TICKS ? indicator : 0;
    }
}
