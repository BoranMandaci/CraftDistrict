package com.craftdistrict.mixin;

import com.craftdistrict.access.AccessControlData;
import com.craftdistrict.access.AccessDoorTarget;
import com.craftdistrict.block.OfficeBadgeReaderBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class AccessControlBlockRemovalMixin {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void craftdistrict$invalidateAccessLinks(BlockState state, Level level, BlockPos pos,
                                                     BlockState replacement, boolean moving, CallbackInfo ci) {
        if (state.getBlock() == replacement.getBlock() || !(level instanceof ServerLevel server)) return;
        if (state.getBlock() instanceof OfficeBadgeReaderBlock) AccessControlData.get(server).removeReader(pos);
        else if (AccessDoorTarget.supports(state.getBlock())) AccessControlData.get(server).doorRemoved(pos);
    }
}
