package com.craftdistrict.item;

import com.craftdistrict.block.OverbedTableBlock;
import com.craftdistrict.CraftDistrict;
import com.craftdistrict.block.ControlledLight;
import com.craftdistrict.block.KitchenExhaustDuctBlock;
import com.craftdistrict.block.ModularDiningTableBlock;
import com.craftdistrict.block.ConferenceTableBlock;
import com.craftdistrict.block.PoliceWaitingBenchBlock;
import com.craftdistrict.block.SittableBlock;
import com.craftdistrict.block.LyingBlock;
import com.craftdistrict.lighting.LightControls;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/** Server-owned utility actions use ordinary Minecraft durability and break feedback. */
public final class WrenchItem extends Item {
    private static final ThreadLocal<InteractionHand> ACTION_HAND = new ThreadLocal<>();
    public WrenchItem(Properties properties) { super(properties); }
    public static boolean isHeldBy(Player player) {
        return player != null && (player.getMainHandItem().getItem() instanceof WrenchItem
                || player.getOffhandItem().getItem() instanceof WrenchItem);
    }
    public static InteractionHand hand(Player player) {
        if (player.getMainHandItem().getItem() instanceof WrenchItem) return InteractionHand.MAIN_HAND;
        return player.getOffhandItem().getItem() instanceof WrenchItem ? InteractionHand.OFF_HAND : null;
    }
    public static void used(Player player, InteractionHand hand, ItemStack wrench) {
        if (player == null || player.level().isClientSide || player.getAbilities().instabuild || hand == null
                || wrench.isEmpty() || !(wrench.getItem() instanceof WrenchItem) || player.getItemInHand(hand) != wrench) return;
        //? if >=1.21 {
        wrench.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        //?} else {
        /*wrench.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
        *///?}
    }
    public static void usedIfHeld(Player player) {
        if (player == null) return;
        InteractionHand hand = ACTION_HAND.get();
        if (hand == null || !(player.getItemInHand(hand).getItem() instanceof WrenchItem)) hand = hand(player);
        if (hand != null) used(player, hand, player.getItemInHand(hand));
    }
    private static InteractionResult action(UseOnContext context, BlockState before, Supplier<InteractionResult> operation) {
        ItemStack wrench = context.getItemInHand(); int damage = wrench.getDamageValue();
        InteractionHand previous = ACTION_HAND.get(); ACTION_HAND.set(context.getHand());
        InteractionResult result;
        try { result = operation.get(); }
        finally { if (previous == null) ACTION_HAND.remove(); else ACTION_HAND.set(previous); }
        if (!context.getLevel().isClientSide && result.consumesAction() && !wrench.isEmpty()
                && wrench.getDamageValue() == damage && context.getLevel().getBlockState(context.getClickedPos()) != before)
            used(context.getPlayer(), context.getHand(), wrench);
        return result;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos)) return InteractionResult.FAIL;
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        if (block instanceof ModularDiningTableBlock table) return action(context, state, () -> table.toggleSupport(state, level, pos, player));
        if (block instanceof ConferenceTableBlock table) return action(context, state, () -> table.toggleSupport(state, level, pos, player));
        if (block instanceof PoliceWaitingBenchBlock bench) return action(context, state, () -> bench.toggleSupport(state, level, pos, player));
        if (block instanceof KitchenExhaustDuctBlock duct)
            return action(context, state, () -> duct.toggleConnection(state, level, pos, player, context.getClickedFace(), context.getClickLocation()));
        if (block instanceof OverbedTableBlock table) return action(context, state, () -> table.cycleDesign(state, level, pos, player));
        if (block instanceof ControlledLight) return LightControls.use(state, level, pos, player, context.getHand());
        // Invoke only existing open/close actions, never general furniture, beds, or GUIs.
        var id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !CraftDistrict.MOD_ID.equals(id.getNamespace()) || !state.hasProperty(BlockStateProperties.OPEN)
                || block instanceof SittableBlock || block instanceof LyingBlock || block instanceof BedBlock)
            return InteractionResult.PASS;
        var hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, context.isInside());
        //? if >=1.21 {
        return action(context, state, () -> state.useWithoutItem(level, player, hit));
        //?} else {
        /*return action(context, state, () -> state.use(level, player, context.getHand(), hit));
        *///?}
    }
}
