package com.craftdistrict.mixin;

import com.craftdistrict.block.OfficeWallUnitBlock;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow public ServerPlayer player;

    // Vanilla assumes every outline fits within one block of its owning cell's center.
    // Only a hit on the actual recessed blinds surface gets its real outline bounds;
    // vanilla reach, line-of-use processing and permission checks remain in place.
    @ModifyConstant(method = "handleUseItemOn", constant = @Constant(doubleValue = 1.0000001),
            require = 3, allow = 4)
    private double craftdistrict$mountedBlindHitLimit(double original, ServerboundUseItemOnPacket packet) {
        return OfficeWallUnitBlock.interactionHitLimit(player.level(), packet.getHitResult(), original);
    }
}
