package com.craftdistrict.mixin.client;

import com.craftdistrict.block.OfficeTurnstileBlock;
import com.craftdistrict.block.OfficeTurnstileBlockEntity;
import com.craftdistrict.network.OfficeTurnstileNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class OfficeTurnstileMovementMixin {
    @Unique private long craftdistrict$lastTurnstilePush = -4;
    @Unique private long craftdistrict$lastTurnstilePos;

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void craftdistrict$pushTurnstile(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (type != MoverType.SELF || movement.horizontalDistanceSqr() < 1e-8) return;
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!OfficeTurnstileBlock.canPush(player)) return;

        var level = player.level();
        BlockPos pos = player.blockPosition();
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof OfficeTurnstileBlock)) {
            int x = Mth.floor(player.getX() + movement.x * OfficeTurnstileBlock.APPROACH_LOOKAHEAD);
            int z = Mth.floor(player.getZ() + movement.z * OfficeTurnstileBlock.APPROACH_LOOKAHEAD);
            if (x == pos.getX() && z == pos.getZ()) return;
            pos = new BlockPos(x, pos.getY(), z);
            state = level.getBlockState(pos);
        }
        if (!(state.getBlock() instanceof OfficeTurnstileBlock gate)) return;
        int direction = gate.pushDirection(state, pos, player, movement);
        if (direction == 0) return;

        long tick = level.getGameTime(), key = pos.asLong();
        long since = tick - craftdistrict$lastTurnstilePush;
        if (key == craftdistrict$lastTurnstilePos && since >= 0 && since < 4) return;
        if (level.getBlockEntity(pos) instanceof OfficeTurnstileBlockEntity turnstile && turnstile.busy(tick)) return;
        craftdistrict$lastTurnstilePush = tick;
        craftdistrict$lastTurnstilePos = key;
        OfficeTurnstileNetworking.push(pos, direction);
    }
}
