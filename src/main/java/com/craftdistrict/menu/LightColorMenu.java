package com.craftdistrict.menu;

import com.craftdistrict.item.WrenchItem;
import com.craftdistrict.block.HotelGuestBedBlock;
import com.craftdistrict.lighting.*;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** One confirmation owns the settings, per-panel dye exchange, and one Wrench use. */
public final class LightColorMenu extends AbstractContainerMenu {
    public enum Feedback { READY, MISSING_DYE, STALE }
    private final Player owner;
    private final Level level;
    private final BlockPos pos;
    private final List<BlockPos> parts;
    private final List<BlockState> structure;
    private final LedLightGroups.Group group;
    private final long session;
    private final boolean allowsColor;
    private final boolean colorOnly;
    private final InteractionHand wrenchHand;
    private final ItemStack wrench;
    private final DataSlot color = DataSlot.standalone(), mode = DataSlot.standalone(), effect = DataSlot.standalone();
    private final DataSlot power = DataSlot.standalone(), active = DataSlot.standalone();
    private final DataSlot countLow = DataSlot.standalone(), countHigh = DataSlot.standalone();
    private final DataSlot feedback = DataSlot.standalone(), acknowledgement = DataSlot.standalone();
    private int nextSequence, clientSequence, pendingAck, requestedColor;
    private boolean closed, pending, selectedPower;
    private int selectedColor = -1, selectedMode, selectedEffect;
    private boolean hasColorSelection, hasModeSelection, hasEffectSelection, hasPowerSelection, showFeedback = true;

    public LightColorMenu(int id, Inventory inventory, FriendlyByteBuf extra) {
        this(id, inventory, extra.readBlockPos(), extra.readLong(), extra.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }
    public LightColorMenu(int id, Inventory inventory, BlockPos pos, long session) {
        this(id, inventory, pos, session, WrenchItem.hand(inventory.player));
    }
    public LightColorMenu(int id, Inventory inventory, BlockPos pos, long session, InteractionHand usedHand) {
        super(CraftDistrictAssembly.LIGHT_COLOR_MENU.get(), id);
        owner = inventory.player; level = owner.level(); this.pos = pos.immutable(); this.session = session;
        BlockState state = level.getBlockState(pos);
        colorOnly = state.getBlock() instanceof HotelGuestBedBlock bed && bed.dyeable();
        allowsColor = colorOnly || LightControls.dyeable(state);
        group = LightControls.group(level, state, pos);
        parts = group == null ? List.copyOf(LightControls.parts(state, pos)) : LedLightGroups.get((ServerLevel)level).allParts(group);
        structure = group == null ? parts.stream().map(part -> LightControls.shape(level.getBlockState(part))).toList() : List.of();
        wrenchHand = usedHand; wrench = wrenchHand == null ? ItemStack.EMPTY : owner.getItemInHand(wrenchHand);
        color.set(group != null ? group.color() : allowsColor && level instanceof ServerLevel server ? LightDyeData.get(server).color(pos) : -1);
        mode.set((group == null ? LightControls.mode(level, state, pos) : group.mode()).ordinal());
        effect.set((group == null ? LightControls.effect(state) : group.effect()).ordinal());
        power.set(!colorOnly && (group == null ? LightControls.active(level, state, pos) : group.manual()) ? 1 : 0);
        active.set(!colorOnly && LightControls.active(level, state, pos) ? 1 : 0);
        int count = group == null ? 1 : group.size(); countLow.set(count & 0xffff); countHigh.set(count >>> 16);
        addDataSlot(color); addDataSlot(mode); addDataSlot(effect); addDataSlot(power); addDataSlot(active);
        addDataSlot(countLow); addDataSlot(countHigh); addDataSlot(feedback); addDataSlot(acknowledgement);
    }
    private static boolean permitted(BlockState state, Level level, BlockPos pos, Player player) {
        if (player == null || !player.isAlive() || player.isSpectator() || !player.mayBuild()
                || player.level() != level || !WrenchItem.isHeldBy(player)
                || !(LightControls.settingsEligible(state.getBlock()) || state.getBlock() instanceof HotelGuestBedBlock bed && bed.dyeable())
                || player.distanceToSqr(pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5) > 64) return false;
        List<BlockPos> parts = LightControls.parts(state, pos);
        for (BlockPos part : parts) if (!level.hasChunkAt(part) || !level.getWorldBorder().isWithinBounds(part)
                || !level.mayInteract(player, part)) return false;
        return LightControls.complete(level, state, parts);
    }
    public static InteractionResult open(BlockState state, Level level, BlockPos pos, Player player, InteractionHand usedHand) {
        if (!permitted(state, level, pos, player)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server)) return InteractionResult.PASS;
        long session = ThreadLocalRandom.current().nextLong();
        MenuRegistry.openExtendedMenu(server, new ExtendedMenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable(state.getBlock() instanceof HotelGuestBedBlock
                    ? "screen.craftdistrict.bedding_color.title" : "screen.craftdistrict.light_settings.title"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) {
                return new LightColorMenu(id, inventory, pos, session, usedHand);
            }
            @Override public void saveExtraData(FriendlyByteBuf buffer) { buffer.writeBlockPos(pos); buffer.writeLong(session); buffer.writeBoolean(usedHand == InteractionHand.OFF_HAND); }
        });
        return InteractionResult.CONSUME;
    }
    @Override public boolean stillValid(Player player) {
        if (closed || player != owner || !level.hasChunkAt(pos) || wrenchHand == null
                || owner.getItemInHand(wrenchHand) != wrench || wrench.isEmpty()) return false;
        BlockState state = level.getBlockState(pos);
        if (!permitted(state, level, pos, player)) return false;
        if (group != null) return LedLightGroups.get((ServerLevel)level).find(pos) == group;
        if (!parts.equals(LightControls.parts(state, pos))) return false;
        for (int i = 0; i < parts.size(); i++) if (LightControls.shape(level.getBlockState(parts.get(i))) != structure.get(i)) return false;
        return true;
    }
    @Override public void broadcastChanges() {
        if (!level.isClientSide && level.hasChunkAt(pos) && LightControls.settingsEligible(level.getBlockState(pos).getBlock()))
            active.set(LightControls.active(level, level.getBlockState(pos), pos) ? 1 : 0);
        super.broadcastChanges();
    }
    @Override public void removed(Player player) {
        closed = true; pending = false; hasColorSelection = hasModeSelection = hasEffectSelection = hasPowerSelection = false;
        super.removed(player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    public void confirm(ServerPlayer player, long suppliedSession, int sequence, int selected, int expectedColor,
                        int selectedMode, int selectedEffect, int expectedMode, int expectedEffect,
                        boolean selectedPower, boolean expectedPower) {
        if (selected < -1 || selected > 15 || expectedColor < -1 || expectedColor > 15
                || !LightMode.valid(selectedMode) || !LightMode.valid(expectedMode)
                || !LightEffect.valid(selectedEffect) || !LightEffect.valid(expectedEffect)
                || player.containerMenu != this || !stillValid(player) || suppliedSession != session
                || sequence < 0 || sequence != nextSequence || nextSequence == Integer.MAX_VALUE) return;
        nextSequence++;
        if (colorOnly) {
            confirmBedding(player, selected, expectedColor, selectedMode, selectedEffect, expectedMode, expectedEffect, selectedPower, expectedPower);
            return;
        }
        ServerLevel server = player.serverLevel(); LightDyeData data = LightDyeData.get(server);
        BlockState current = server.getBlockState(pos);
        int previous = group != null ? group.color() : allowsColor ? data.color(pos) : -1;
        LightMode actualMode = group == null ? LightControls.mode(server, current, pos) : group.mode();
        LightEffect actualEffect = group == null ? LightControls.effect(current) : group.effect();
        boolean manual = group == null ? LightControls.active(server, current, pos) : group.manual();
        boolean consistent = true;
        for (BlockPos part : parts) {
            if (!server.getWorldBorder().isWithinBounds(part) || !server.mayInteract(player, part)) { finish(Feedback.STALE); return; }
            if (group != null) { consistent &= data.color(part) == previous; continue; }
            BlockState other = server.getBlockState(part);
            consistent &= (!allowsColor || data.color(part) == previous)
                    && other.getValue(LightControls.REDSTONE) == current.getValue(LightControls.REDSTONE)
                    && LightControls.effect(other) == actualEffect;
        }
        if (expectedColor != color.get() || previous != color.get() || expectedMode != mode.get()
                || expectedEffect != effect.get() || actualMode.ordinal() != mode.get()
                || actualEffect.ordinal() != effect.get() || !consistent || (!allowsColor && selected != -1)
                || (selected == -1 && previous != -1) || expectedPower != (power.get() != 0)
                || (group != null || actualMode == LightMode.MANUAL) && manual != (power.get() != 0)) {
            color.set(previous); mode.set(actualMode.ordinal()); effect.set(actualEffect.ordinal()); power.set(manual ? 1 : 0);
            finish(Feedback.STALE); return;
        }
        boolean recolor = allowsColor && selected != previous;
        boolean changed = selectedMode != actualMode.ordinal() || selectedEffect != actualEffect.ordinal()
                || LightMode.byId(selectedMode) == LightMode.MANUAL && selectedPower != manual;
        if (!recolor && !changed) { complete(player); return; }
        LightDyeData.Payment payment = recolor ? data.payment(player, parts, selected, group != null, true) : null;
        if (recolor && payment == null) { finish(Feedback.MISSING_DYE); return; }
        boolean manualChoice = LightMode.byId(selectedMode) == LightMode.MANUAL ? selectedPower : group == null ? manual : group.manual();
        if (!LightControls.setSettings(server, current, pos, LightMode.byId(selectedMode), LightEffect.byId(selectedEffect), manualChoice)) {
            finish(Feedback.STALE); return;
        }
        if (recolor) {
            payment.commit(player);
            if (group != null) LedLightGroups.get(server).color(group, selected);
            color.set(selected);
            LightDyeNetworking.changed(server, group == null ? parts : LedLightGroups.get(server).loadedParts(server, group));
        }
        WrenchItem.used(player, wrenchHand, wrench);
        mode.set(selectedMode); effect.set(selectedEffect); power.set(manualChoice ? 1 : 0);
        complete(player);
    }
    private void confirmBedding(ServerPlayer player, int selected, int expected, int selectedMode, int selectedEffect,
                                int expectedMode, int expectedEffect, boolean selectedPower, boolean expectedPower) {
        ServerLevel server = player.serverLevel(); BlockState state = server.getBlockState(pos);
        LightDyeData data = LightDyeData.get(server); int previous = data.color(parts.get(0));
        boolean consistent = parts.stream().allMatch(part -> data.color(part) == previous);
        if (!(state.getBlock() instanceof HotelGuestBedBlock bed) || expected != color.get() || previous != color.get()
                || !consistent || selectedMode != 0 || selectedEffect != 0 || expectedMode != 0 || expectedEffect != 0
                || selectedPower || expectedPower || selected == -1 && previous != -1) {
            color.set(previous); finish(Feedback.STALE); return;
        }
        if (selected == previous) { complete(player); return; }
        if (!bed.color(server, pos, player, selected)) { finish(Feedback.MISSING_DYE); return; }
        color.set(selected); WrenchItem.used(player, wrenchHand, wrench); complete(player);
    }
    private void complete(ServerPlayer player) { finish(Feedback.READY); closed = true; player.closeContainer(); }
    private void finish(Feedback result) { feedback.set(result.ordinal()); acknowledgement.set(nextSequence & 0x7fff); broadcastChanges(); }
    public boolean allowsColor() { return allowsColor; }
    public boolean colorOnly() { return colorOnly; }
    public boolean targets(BlockPos target) { return parts.contains(target); }
    public int color() { return color.get(); }
    public LightMode mode() { return LightMode.byId(mode.get()); }
    public LightEffect effect() { return LightEffect.byId(effect.get()); }
    public boolean manualPower() { return power.get() != 0; }
    public boolean active() { return active.get() != 0; }
    public int requiredDyes() { return (countLow.get() & 0xffff) | (countHigh.get() & 0xffff) << 16; }
    public int selectedColor() { return hasColorSelection ? selectedColor : color.get(); }
    public LightMode selectedMode() { return hasModeSelection ? LightMode.byId(selectedMode) : mode(); }
    public LightEffect selectedEffect() { return hasEffectSelection ? LightEffect.byId(selectedEffect) : effect(); }
    public boolean selectedPower() { return hasPowerSelection ? selectedPower : manualPower(); }
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
        if (!level.isClientSide || closed || pending() || !allowsColor || selected < 0 || selected > 15) return;
        selectedColor = selected; hasColorSelection = true; showFeedback = false;
    }
    public void chooseMode(LightMode selected) {
        if (!level.isClientSide || closed || pending() || colorOnly) return;
        selectedMode = selected.ordinal(); hasModeSelection = true; showFeedback = false;
    }
    public void chooseEffect(LightEffect selected) {
        if (!level.isClientSide || closed || pending() || colorOnly) return;
        selectedEffect = selected.ordinal(); hasEffectSelection = true; showFeedback = false;
    }
    public void choosePower(boolean enabled) {
        if (!level.isClientSide || closed || pending() || colorOnly || selectedMode() != LightMode.MANUAL) return;
        selectedPower = enabled; hasPowerSelection = true; showFeedback = false;
    }
    public void confirmSelection() {
        if (!level.isClientSide || closed || pending() || clientSequence == Integer.MAX_VALUE) return;
        requestedColor = selectedColor(); pending = true; showFeedback = false; pendingAck = (clientSequence+1) & 0x7fff;
        LightColorNetworking.request(containerId, session, clientSequence++, selectedColor(), color.get(),
                selectedMode().ordinal(), selectedEffect().ordinal(), mode.get(), effect.get(), selectedPower(), manualPower());
    }
}
