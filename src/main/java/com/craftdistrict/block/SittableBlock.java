package com.craftdistrict.block;

import net.minecraft.world.level.block.state.BlockState;

/** Seat geometry shared by furniture with different placement and multiblock lifecycles. */
public interface SittableBlock {
    double seatHeight();
    default double forwardOffset() { return 0; }
    default boolean isSittable(BlockState state) { return true; }
}
