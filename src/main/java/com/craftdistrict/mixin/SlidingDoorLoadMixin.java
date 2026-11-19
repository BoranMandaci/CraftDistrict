package com.craftdistrict.mixin;

import com.craftdistrict.block.SlidingDoorBlockEntity;
import com.craftdistrict.block.SlidingEntranceBlock;
import com.craftdistrict.door.SlidingDoorSensors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Upgrade existing placed entrances before chunk data is sent; no registry or blockstate migration. */
@Mixin(LevelChunk.class)
public abstract class SlidingDoorLoadMixin {
    @Inject(method="runPostLoad",at=@At("TAIL"))
    private void craftdistrict$slidingRenderRecords(CallbackInfo ci) {
        LevelChunk chunk=(LevelChunk)(Object)this;
        if(chunk.getLevel() instanceof ServerLevel level)
            chunk.findBlocks(state->state.getBlock() instanceof SlidingEntranceBlock&&state.getValue(SlidingEntranceBlock.HALF)==DoubleBlockHalf.LOWER,
                    (pos,state)-> {
                        if(chunk.getBlockEntity(pos) instanceof SlidingDoorBlockEntity motion&&!state.getValue(SlidingEntranceBlock.OPEN)&&motion.moving(level.getGameTime()))
                            SlidingDoorSensors.watchClosing(level,pos);
                    });
    }
}
