package com.craftdistrict.mixin;

import com.craftdistrict.block.SlidingEntranceBlock;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Sliding leaves can overhang diagonally during opening and closing, regardless of their target OPEN state. */
@Mixin(BlockCollisions.class)
public abstract class SlidingDoorCollisionMixin {
    @Redirect(method="computeNext",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"),require=1,allow=1)
    private boolean craftdistrict$slidingOverhang(BlockState state,Block expected) {
        return state.is(expected)||expected==Blocks.MOVING_PISTON
                &&state.getBlock() instanceof SlidingEntranceBlock;
    }
}
