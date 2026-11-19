package com.craftdistrict.mixin;

import com.craftdistrict.block.BarDoorBlock;
import com.craftdistrict.block.BarDoorBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Existing saved Bar Doors acquire their unticked render record before chunk data is sent. */
@Mixin(LevelChunk.class)
public abstract class BarDoorChunkMixin {
    @Inject(method="runPostLoad",at=@At("TAIL"))
    private void craftdistrict$barRenderRecords(CallbackInfo ci) {
        LevelChunk chunk=(LevelChunk)(Object)this;
        if(chunk.getLevel() instanceof ServerLevel level)
            chunk.findBlocks(state->state.getBlock() instanceof BarDoorBlock,(pos,state)-> {
                var entity=chunk.getBlockEntity(pos);
                if((state.getValue(BarDoorBlock.OPEN)||state.getValue(BarDoorBlock.SWING)!=4
                        ||entity instanceof BarDoorBlockEntity bar&&bar.remaining(level.getGameTime())>0)
                        &&!level.getBlockTicks().hasScheduledTick(pos,state.getBlock()))
                    level.scheduleTick(pos.immutable(),state.getBlock(),2);
            });
    }
}
