package com.craftdistrict.menu;

import com.craftdistrict.item.AccessCardItem;
import com.craftdistrict.registry.CraftDistrictAccessControl;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** The server holds the original inventory stack while its editor is open. */
public final class AccessCardMenu extends AbstractContainerMenu {
    private final Player owner;
    private final long session;
    private final String name;
    private final int color;
    private final InteractionHand hand;
    private final ItemStack stack;
    private final UUID cardId;

    public AccessCardMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readLong(), buffer.readUtf(AccessCardItem.MAX_NAME), buffer.readUnsignedByte(), null, ItemStack.EMPTY, null);
    }

    private AccessCardMenu(int id, Inventory inventory, long session, String name, int color,
                           InteractionHand hand, ItemStack stack, UUID cardId) {
        super(CraftDistrictAccessControl.ACCESS_CARD.get(), id);
        owner = inventory.player;
        this.session = session;
        this.name = name;
        this.color = color;
        this.hand = hand;
        this.stack = stack;
        this.cardId = cardId;
    }

    public static void open(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (!player.isAlive() || player.isSpectator() || player.getItemInHand(hand) != stack || !(stack.getItem() instanceof AccessCardItem)) return;
        UUID cardId = AccessCardItem.initialize(stack);
        long session = ThreadLocalRandom.current().nextLong();
        String name = AccessCardItem.name(stack);
        int color = AccessCardItem.color(stack);
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("screen.craftdistrict.access_card.title"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
                return new AccessCardMenu(id, inventory, session, name, color, hand, stack, cardId);
            }
            @Override public void saveExtraData(FriendlyByteBuf buffer) {
                buffer.writeLong(session);
                buffer.writeUtf(name, AccessCardItem.MAX_NAME);
                buffer.writeByte(color);
            }
        });
    }

    public long session() { return session; }
    public String name() { return name; }
    public int color() { return color; }

    public void save(ServerPlayer player, long submitted, String name, int color) {
        if (player.containerMenu != this || submitted != session || !stillValid(player)
                || !AccessCardItem.customize(stack, cardId, name, color)) return;
        player.getInventory().setChanged();
        player.closeContainer();
        player.inventoryMenu.broadcastChanges();
    }

    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator()
                && (player.level().isClientSide || hand != null && player.getItemInHand(hand) == stack
                && cardId.equals(AccessCardItem.identity(stack)));
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
