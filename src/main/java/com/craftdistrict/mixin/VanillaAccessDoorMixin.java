package com.craftdistrict.mixin;

import com.craftdistrict.access.AccessDoorSecurity;
import net.minecraft.core.BlockPos;
//? if <1.21 {
/*import net.minecraft.world.InteractionHand;
*///?}
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorBlock.class)
public abstract class VanillaAccessDoorMixin {
    //? if >=1.21 {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void craftdistrict$secureUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                                          CallbackInfoReturnable<InteractionResult> result) {
    //?} else {
    /*@Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void craftdistrict$secureUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit, CallbackInfoReturnable<InteractionResult> result) {
    *///?}
        if (AccessDoorSecurity.secured(level, pos, state)) result.setReturnValue(AccessDoorSecurity.denied(level, player));
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void craftdistrict$secureRedstone(BlockState state, Level level, BlockPos pos, Block neighbor,
                                               BlockPos from, boolean moving, CallbackInfo callback) {
        if (AccessDoorSecurity.secured(level, pos, state)) callback.cancel();
    }

    @Inject(method = "setOpen", at = @At("HEAD"), cancellable = true)
    private void craftdistrict$secureSetOpen(Entity source, Level level, BlockState state, BlockPos pos,
                                              boolean open, CallbackInfo callback) {
        if (AccessDoorSecurity.secured(level, pos, state)) callback.cancel();
    }
}
