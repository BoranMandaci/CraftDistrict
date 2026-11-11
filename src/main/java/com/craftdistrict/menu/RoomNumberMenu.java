package com.craftdistrict.menu;

import com.craftdistrict.block.RoomNumberPlaqueBlock;
import com.craftdistrict.block.RoomNumberPlaqueBlockEntity;
import com.craftdistrict.registry.CraftDistrictRoomNumbers;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.concurrent.ThreadLocalRandom;

/** Pending edits never touch the plaque until one validated server confirmation succeeds. */
public final class RoomNumberMenu extends AbstractContainerMenu {
    private final Player owner;
    private final BlockPos pos;
    private final RoomNumberPlaqueBlockEntity target;
    private final long session;
    private final String original;
    private final boolean originalReversed;
    private final int kind;
    private final DataSlot stale = DataSlot.standalone();
    private boolean closed;
    public RoomNumberMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), buffer.readLong(), buffer.readUtf(9), buffer.readBoolean(), buffer.readUnsignedByte());
    }
    private RoomNumberMenu(int id, Inventory inventory, BlockPos pos, long session, String original, boolean reversed, int kind) {
        super(CraftDistrictRoomNumbers.MENU.get(), id);
        owner = inventory.player; this.pos = pos.immutable(); this.session = session; this.original = original;
        this.originalReversed = reversed; this.kind = kind;
        target = owner.level().getBlockEntity(pos) instanceof RoomNumberPlaqueBlockEntity plaque ? plaque : null;
        addDataSlot(stale);
    }
    private static boolean permitted(Level level, BlockPos pos, Player player) {
        return player != null && player.isAlive() && !player.isSpectator() && player.mayBuild() && player.level() == level
                && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(player, pos)
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64
                && level.getBlockState(pos).getBlock() instanceof RoomNumberPlaqueBlock;
    }
    public static InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!permitted(level, pos, player)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server) || !(level.getBlockEntity(pos) instanceof RoomNumberPlaqueBlockEntity plaque)) return InteractionResult.PASS;
        long session = ThreadLocalRandom.current().nextLong(); String original = plaque.number(); boolean reversed = plaque.reversed(); int kind = plaque.kind();
        MenuRegistry.openExtendedMenu(server, new ExtendedMenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable(key(kind, "title")); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) { return new RoomNumberMenu(id, inventory, pos, session, original, reversed, kind); }
            @Override public void saveExtraData(FriendlyByteBuf buffer) { buffer.writeBlockPos(pos); buffer.writeLong(session); buffer.writeUtf(original, 9); buffer.writeBoolean(reversed); buffer.writeByte(kind); }
        });
        return InteractionResult.CONSUME;
    }
    public String original() { return original; }
    public boolean originalReversed() { return originalReversed; }
    public int kind() { return kind; }
    private static String key(int kind, String suffix) { return "screen.craftdistrict." + (kind == 1 ? "floor_indicator" : kind == 2 ? "room_sign" : "room_number") + "." + suffix; }
    public Component text(String suffix) { return Component.translatable(key(kind, suffix)); }
    public long session() { return session; }
    public boolean stale() { return stale.get() != 0; }
    @Override public boolean stillValid(Player player) {
        return !closed && player == owner && (owner.level().isClientSide || permitted(owner.level(), pos, owner)
                && target != null && !target.isRemoved() && owner.level().getBlockEntity(pos) == target);
    }
    public void confirm(ServerPlayer player, long suppliedSession, String number, boolean reversed) {
        if (player.containerMenu != this || suppliedSession != session || !stillValid(player) || !RoomNumberPlaqueBlockEntity.valid(kind, number) || target.kind() != kind) return;
        if (!original.equals(target.number()) || originalReversed != target.reversed()) { stale.set(1); broadcastChanges(); return; }
        closed = true;
        target.setSign(number, kind != 0 && reversed);
        player.closeContainer();
    }
    @Override public void removed(Player player) { closed = true; super.removed(player); }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
