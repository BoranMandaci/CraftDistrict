package com.craftdistrict.mixin;

import com.craftdistrict.block.OfficeTurnstileBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Old Office Turnstiles already have the right block state, but need their renderer's record. */
@Mixin(LevelChunk.class)
public abstract class OfficeTurnstileChunkMixin {
    @Inject(method = "runPostLoad", at = @At("TAIL"))
    private void craftdistrict$restoreTurnstileControllers(CallbackInfo ci) {
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (chunk.getLevel() instanceof ServerLevel)
            chunk.findBlocks(state -> state.getBlock() instanceof OfficeTurnstileBlock,
                    (pos, state) -> chunk.getBlockEntity(pos.immutable(), LevelChunk.EntityCreationType.IMMEDIATE));
    }
}
