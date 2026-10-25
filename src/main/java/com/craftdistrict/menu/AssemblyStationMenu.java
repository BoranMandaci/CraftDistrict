package com.craftdistrict.menu;

import com.craftdistrict.assembly.*;
import com.craftdistrict.network.AssemblyNetworking;
import com.craftdistrict.registry.CraftDistrictAssembly;
import com.craftdistrict.registry.CraftDistrictBlocks;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class AssemblyStationMenu extends AbstractContainerMenu {
    public static final int WIDTH = 316, HEIGHT = 236, INVENTORY_X = 77, INVENTORY_Y = 158, HOTBAR_Y = 216;
    private final Player owner;
    private final Inventory inventory;
    private final Level level;
    private final BlockPos pos;
    private final long session;
    private final List<AssemblyCatalog.Entry> openedCatalog;
    private final Map<ResourceLocation, AssemblyRecipe> allowedRecipes;
    private final DataSlot feedback = DataSlot.standalone();
    private final DataSlot acknowledgement = DataSlot.standalone();
    private int nextSequence, reloadTicks, clientSequence, pendingAck;
    private boolean closed, pending;
    private long lastRequestTick = Long.MIN_VALUE;
    private ResourceLocation requestedRecipe;

    public AssemblyStationMenu(int id, Inventory inventory, FriendlyByteBuf extra) {
        this(id, inventory, extra.readBlockPos(), extra.readLong());
    }

    public AssemblyStationMenu(int id, Inventory inventory, BlockPos pos, long session) {
        super(CraftDistrictAssembly.MENU.get(), id);
        this.owner = inventory.player; this.inventory = inventory; this.level = owner.level();
        this.pos = pos.immutable(); this.session = session;
        this.openedCatalog = AssemblyCatalog.entries(level.getRecipeManager());
        Map<ResourceLocation, AssemblyRecipe> recipes = new HashMap<>();
        openedCatalog.forEach(entry -> recipes.put(entry.id(), entry.recipe()));
        this.allowedRecipes = Map.copyOf(recipes);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, 9 + row * 9 + column, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        // Feedback precedes its acknowledgement, so the client never enables a second in-flight request.
        addDataSlot(feedback); addDataSlot(acknowledgement);
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        long session = ThreadLocalRandom.current().nextLong();
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("block.craftdistrict.assembly_station"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
                return new AssemblyStationMenu(id, inventory, pos, session);
            }
            @Override public void saveExtraData(FriendlyByteBuf buffer) { buffer.writeBlockPos(pos); buffer.writeLong(session); }
        });
    }

    @Override public boolean stillValid(Player player) {
        return !closed && player == owner && !player.isSpectator() && player.isAlive() && player.level() == level
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64
                && level.hasChunkAt(pos) && level.getBlockState(pos).is(CraftDistrictBlocks.Survival.ASSEMBLY_STATION.get());
    }

    @Override public void removed(Player player) { closed = true; super.removed(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (!(index < 27 ? moveItemStackTo(stack, 27, 36, false) : moveItemStackTo(stack, 0, 27, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    public void craft(ServerPlayer player, long suppliedSession, int sequence, ResourceLocation id, int operations) {
        if (!AssemblyCrafting.validQuantity(operations) || closed || player.containerMenu != this || !stillValid(player) || suppliedSession != session
                || !acceptSequence(suppliedSession, sequence)) return;
        AssemblyRecipe expected = allowedRecipes.get(id);
        AssemblyRecipe recipe = AssemblyCatalog.find(level.getRecipeManager(), id);
        if (expected == null || recipe == null) { finish(AssemblyCrafting.Result.STALE_RECIPE); return; }
        if (expected != recipe) { invalidate(player); return; }
        long tick = level.getGameTime();
        if (!allowCraft(tick)) { finish(AssemblyCrafting.Result.BUSY); return; }
        AssemblyCrafting.Plan plan = AssemblyCrafting.plan(recipe, AssemblyCrafting.snapshot(inventory), operations);
        finish(AssemblyCrafting.commit(inventory, plan));
    }

    private boolean acceptSequence(long suppliedSession, int sequence) {
        if (suppliedSession != session || sequence < 0 || sequence != nextSequence || nextSequence == Integer.MAX_VALUE) return false;
        nextSequence++;
        return true;
    }

    private boolean allowCraft(long tick) {
        if (tick == lastRequestTick) return false;
        lastRequestTick = tick;
        return true;
    }

    private void finish(AssemblyCrafting.Result result) {
        feedback.set(result.ordinal());
        // The full monotonic sequence is validated on the server; only its acknowledgement needs a short.
        acknowledgement.set(nextSequence & 0x7fff);
        broadcastChanges();
    }

    private void invalidate(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("screen.craftdistrict.assembly.reload"), true);
        player.closeContainer();
    }

    @Override public void broadcastChanges() {
        if (!level.isClientSide && !closed && ++reloadTicks >= 10) {
            reloadTicks = 0;
            if (!AssemblyCatalog.same(openedCatalog, AssemblyCatalog.entries(level.getRecipeManager()))) {
                if (owner instanceof ServerPlayer player) invalidate(player);
                return;
            }
        }
        super.broadcastChanges();
    }

    public AssemblyCrafting.Result feedback() {
        int index = feedback.get();
        return index >= 0 && index < AssemblyCrafting.Result.values().length ? AssemblyCrafting.Result.values()[index] : AssemblyCrafting.Result.BUSY;
    }
    public ResourceLocation requestedRecipe() { return requestedRecipe; }
    public boolean pending() {
        if (pending && acknowledgement.get() == pendingAck) pending = false;
        return pending;
    }
    public void requestCraft(ResourceLocation recipe) {
        requestCraft(recipe, 1);
    }
    public void requestCraft(ResourceLocation recipe, int operations) {
        if (!AssemblyCrafting.validQuantity(operations)) return;
        if (!level.isClientSide || pending() || clientSequence == Integer.MAX_VALUE) return;
        requestedRecipe = recipe; pending = true; pendingAck = (clientSequence + 1) & 0x7fff;
        AssemblyNetworking.request(owner, containerId, session, clientSequence++, recipe, operations);
    }
}
