package com.craftdistrict.menu;

import com.craftdistrict.item.WrenchItem;
import com.craftdistrict.lighting.LightControls;
import com.craftdistrict.lighting.LightDyeData;
import com.craftdistrict.network.LightColorNetworking;
import com.craftdistrict.network.LightDyeNetworking;
import com.craftdistrict.registry.CraftDistrictAssembly;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** One menu session owns one light; all dye exchanges run on the server thread. */
public final class LightColorMenu extends AbstractContainerMenu {
    public enum Feedback { READY, MISSING_DYE, STALE }
    private final Player owner;
    private final Level level;
    private final BlockPos pos;
    private final List<BlockPos> parts;
    private final List<BlockState> structure;
    private final long session;
    private final DataSlot color = DataSlot.standalone();
    private final DataSlot feedback = DataSlot.standalone();
    private final DataSlot acknowledgement = DataSlot.standalone();
    private int nextSequence, clientSequence, pendingAck, requestedColor;
    private boolean closed, pending;
    // An unconfirmed choice belongs only to this client menu, never to the saved light palette.
    private int selectedColor = -1;
    private boolean hasSelection, showFeedback = true;

    public LightColorMenu(int id, Inventory inventory, FriendlyByteBuf extra) {
        this(id, inventory, extra.readBlockPos(), extra.readLong());
    }

    public LightColorMenu(int id, Inventory inventory, BlockPos pos, long session) {
        super(CraftDistrictAssembly.LIGHT_COLOR_MENU.get(), id);
        owner = inventory.player; level = owner.level(); this.pos = pos.immutable(); this.session = session;
        BlockState state = level.getBlockState(pos);
        parts = List.copyOf(LightControls.parts(state, pos));
        structure = parts.stream().map(part -> shape(level.getBlockState(part))).toList();
        color.set(level instanceof ServerLevel server ? LightDyeData.get(server).color(pos) : -1);
        // Publish color and feedback before acknowledging the request.
        addDataSlot(color); addDataSlot(feedback); addDataSlot(acknowledgement);
    }

    private static BlockState shape(BlockState state) {
        return LightControls.dyeable(state) ? state.setValue(LightControls.LIT, false).setValue(LightControls.REDSTONE, false) : state;
    }

    private static boolean permitted(BlockState state, Level level, BlockPos pos, Player player) {
        if (player == null || !player.isAlive() || player.isSpectator() || !player.mayBuild()
                || player.level() != level || !WrenchItem.isHeldBy(player) || !LightControls.dyeable(state)
                || player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 64) return false;
        List<BlockPos> parts = LightControls.parts(state, pos);
        for (BlockPos part : parts) if (!level.hasChunkAt(part) || !level.getWorldBorder().isWithinBounds(part)
                || !level.mayInteract(player, part)) return false;
        return LightControls.complete(level, state, parts);
    }

    public static InteractionResult open(BlockState state, Level level, BlockPos pos, Player player) {
        if (!permitted(state, level, pos, player)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server)) return InteractionResult.PASS;
        long session = ThreadLocalRandom.current().nextLong();
        MenuRegistry.openExtendedMenu(server, new ExtendedMenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("screen.craftdistrict.light_color.title"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
                return new LightColorMenu(id, inventory, pos, session);
            }
            @Override public void saveExtraData(FriendlyByteBuf buffer) { buffer.writeBlockPos(pos); buffer.writeLong(session); }
        });
        return InteractionResult.CONSUME;
    }

    @Override public boolean stillValid(Player player) {
        if (closed || player != owner || !level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!permitted(state, level, pos, player) || !parts.equals(LightControls.parts(state, pos))) return false;
        for (int i = 0; i < parts.size(); i++) if (shape(level.getBlockState(parts.get(i))) != structure.get(i)) return false;
        return true;
    }

    @Override public void removed(Player player) {
        closed = true; pending = false; hasSelection = false; selectedColor = -1;
        super.removed(player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    public void confirm(ServerPlayer player, long suppliedSession, int sequence, int selected, int expected) {
        if (selected < 0 || selected > 15 || expected < -1 || expected > 15 || player.containerMenu != this
                || !stillValid(player) || suppliedSession != session || sequence < 0 || sequence != nextSequence
                || nextSequence == Integer.MAX_VALUE) return;
        nextSequence++; // Consume a sequence once, including failed/stale requests.
        ServerLevel server = player.serverLevel();
        LightDyeData data = LightDyeData.get(server);
        int previous = data.color(pos);
        boolean consistent = true;
        for (BlockPos part : parts) consistent &= data.color(part) == previous;
        if (expected != color.get() || previous != color.get() || !consistent) {
            color.set(previous); finish(Feedback.STALE); return;
        }
        if (selected == previous) { complete(player); return; }
        Inventory inventory = player.getInventory();
        DyeItem dye = DyeItem.byColor(DyeColor.byId(selected));
        boolean creative = player.getAbilities().instabuild;
        int slot = -1;
        if (!creative) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).is(dye) && !inventory.getItem(i).isEmpty()) { slot = i; break; }
            }
            if (slot < 0) { finish(Feedback.MISSING_DYE); return; }
        }
        // Validation is complete; no other packet can interleave with this exchange.
        if (!creative) inventory.getItem(slot).shrink(1);
        for (BlockPos part : parts) data.put(part, selected);
        color.set(selected);
        if (!creative && previous >= 0) {
            ItemStack refund = new ItemStack(DyeItem.byColor(DyeColor.byId(previous)));
            inventory.add(refund);
            if (!refund.isEmpty()) player.drop(refund, false);
        }
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        LightDyeNetworking.changed(server, parts);
        complete(player);
    }

    private void complete(ServerPlayer player) {
        finish(Feedback.READY);
        closed = true;
        player.closeContainer();
    }

    private void finish(Feedback result) {
        feedback.set(result.ordinal()); acknowledgement.set(nextSequence & 0x7fff); broadcastChanges();
    }
    public int color() { return color.get(); }
    public int selectedColor() { return hasSelection ? selectedColor : color.get(); }
    public Feedback feedback() {
        if (level.isClientSide && !showFeedback) return Feedback.READY;
        int value = feedback.get(); return value >= 0 && value < Feedback.values().length ? Feedback.values()[value] : Feedback.STALE;
    }
    public int requestedColor() { return requestedColor; }
    public boolean pending() {
        if (pending && acknowledgement.get() == pendingAck) { pending = false; showFeedback = true; }
        return pending;
    }
    public void chooseColor(int selected) {
        if (!level.isClientSide || closed || pending() || selected < 0 || selected > 15) return;
        selectedColor = selected; hasSelection = true; showFeedback = false;
    }
    /** Only the Done button submits a transaction; Escape/menu removal never calls this. */
    public void confirmSelection() {
        int selected = selectedColor();
        if (!level.isClientSide || closed || pending() || selected < 0 || selected > 15 || selected == color.get()
                || clientSequence == Integer.MAX_VALUE) return;
        requestedColor = selected; pending = true; showFeedback = false; pendingAck = (clientSequence + 1) & 0x7fff;
        LightColorNetworking.request(containerId, session, clientSequence++, selected, color.get());
    }
}
