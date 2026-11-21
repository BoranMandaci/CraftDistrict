package com.craftdistrict.mixin;

import com.craftdistrict.block.OfficeWallUnitBlock;
import com.craftdistrict.block.SlidingEntranceBlock;
import com.craftdistrict.item.AccessCardItem;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow public ServerPlayer player;
    @Unique private UUID craftdistrict$creativeCardTransfer;
    @Unique private long craftdistrict$creativeCardTransferUntil;

    // Vanilla assumes every outline fits within one block of its owning cell's center.
    // Only a hit on the actual recessed blinds surface gets its real outline bounds;
    // vanilla reach, line-of-use processing and permission checks remain in place.
    @ModifyConstant(method = "handleUseItemOn", constant = @Constant(doubleValue = 1.0000001),
            require = 3, allow = 4)
    private double craftdistrict$mountedBlindHitLimit(double original, ServerboundUseItemOnPacket packet) {
        double limit = OfficeWallUnitBlock.interactionHitLimit(player.level(), packet.getHitResult(), original);
        return SlidingEntranceBlock.interactionHitLimit(player.level(), packet.getHitResult(), limit);
    }

    // Creative slot packets can construct items. Keep credentials from the server's own inventory only.
    @ModifyVariable(method = "handleSetCreativeModeSlot", at = @At("STORE"), ordinal = 0)
    private ItemStack craftdistrict$creativeCard(ItemStack incoming, ServerboundSetCreativeModeSlotPacket packet) {
        //? if >=1.21 {
        int slot = packet.slotNum();
        //?} else {
        /*int slot = packet.getSlotNum();
        *///?}
        long now = player.level().getGameTime();
        UUID id = AccessCardItem.identity(incoming);
        if (slot >= 1 && slot < player.inventoryMenu.slots.size()) {
            UUID previous = AccessCardItem.identity(player.inventoryMenu.getSlot(slot).getItem());
            if (previous != null && !previous.equals(id)) {
                craftdistrict$creativeCardTransfer = previous;
                craftdistrict$creativeCardTransferUntil = now + 40;
            }
        }
        if (incoming.isEmpty() || !(incoming.getItem() instanceof AccessCardItem)) return incoming;
        ItemStack card = incoming.copyWithCount(1);
        if (id == null || !AccessCardItem.isCarriedBy(player, id) && !id.equals(AccessCardItem.identity(player.inventoryMenu.getCarried()))
                && !(id.equals(craftdistrict$creativeCardTransfer) && now < craftdistrict$creativeCardTransferUntil))
            AccessCardItem.issue(card);
        return card;
    }
}
