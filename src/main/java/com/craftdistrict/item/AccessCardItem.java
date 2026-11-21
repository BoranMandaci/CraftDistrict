package com.craftdistrict.item;

import com.craftdistrict.access.AccessControl;
import com.craftdistrict.block.OfficeBadgeReaderBlock;
import com.craftdistrict.menu.AccessCardMenu;
import net.minecraft.nbt.CompoundTag;
//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import java.util.UUID;

public final class AccessCardItem extends Item {
    public static final int MAX_NAME = 24, COLOR_COUNT = 16;
    public static final int DEFAULT_COLOR = DyeColor.BLUE.getId();
    private static final String KEY = "craftdistrict_access_card";
    private static final CompoundTag EMPTY = new CompoundTag();

    public AccessCardItem(Properties properties) { super(properties.stacksTo(1)); }

    private static CompoundTag data(ItemStack stack) {
        //? if >=1.21 {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        //?} else {
        /*CompoundTag root = stack.getTag();
        *///?}
        return root != null && root.get(KEY) instanceof CompoundTag card ? card : EMPTY;
    }

    public static UUID identity(ItemStack stack) {
        if (!(stack.getItem() instanceof AccessCardItem)) return null;
        CompoundTag card = data(stack);
        return card.hasUUID("id") ? card.getUUID("id") : null;
    }

    public static UUID initialize(ItemStack stack) {
        UUID existing = identity(stack);
        if (existing != null) return existing;
        return issue(stack);
    }

    public static UUID issue(ItemStack stack) {
        UUID id = UUID.randomUUID();
        write(stack, id, name(stack), color(stack));
        return id;
    }

    public static boolean isCarriedBy(Player player, UUID card) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (card.equals(identity(inventory.getItem(slot)))) return true;
        }
        return false;
    }

    public static String name(ItemStack stack) {
        String name = data(stack).getString("name");
        return validName(name) ? name : "";
    }

    public static int color(ItemStack stack) {
        CompoundTag card = data(stack);
        int color = card.getInt("color");
        return card.contains("color") && validColor(color) ? color : DEFAULT_COLOR;
    }

    public static boolean validName(String name) {
        return name.length() <= MAX_NAME && name.codePoints().noneMatch(c -> Character.isISOControl(c)
                || Character.getType(c) == Character.FORMAT || c == 0xa7);
    }

    public static boolean validColor(int color) { return color >= 0 && color < COLOR_COUNT; }

    public static Component displayName(String name) {
        return name.isBlank() ? Component.translatable("item.craftdistrict.access_card") : Component.literal(name);
    }

    public static Component colorName(int color) {
        return Component.translatable("item.minecraft." + DyeColor.byId(color).getName() + "_dye");
    }

    public static boolean customize(ItemStack stack, UUID id, String name, int color) {
        if (!id.equals(identity(stack)) || !validName(name) || !validColor(color)) return false;
        write(stack, id, name.strip(), color);
        return true;
    }

    private static void write(ItemStack stack, UUID id, String name, int color) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putInt("color", color);
        //? if >=1.21 {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> root.put(KEY, tag));
        //?} else {
        /*stack.getOrCreateTag().put(KEY, tag);
        *///?}
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer && !data(stack).hasUUID("id")) initialize(stack);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() != HitResult.Type.MISS)
            return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer server) AccessCardMenu.open(server, hand, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (!(level.getBlockState(context.getClickedPos()).getBlock() instanceof OfficeBadgeReaderBlock)) return InteractionResult.PASS;
        if (context.getPlayer() instanceof ServerPlayer player) {
            if (player.isShiftKeyDown()) AccessControl.present(player, context.getClickedPos(), context.getHand(), context.getItemInHand());
            else AccessControl.swipe(player, context.getClickedPos(), context.getHand(), context.getItemInHand());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public Component getName(ItemStack stack) { return displayName(name(stack)); }

    @Override public void appendHoverText(ItemStack stack,
            //? if >=1.21 {
            TooltipContext context,
            //?} else {
            /*Level level,
            *///?}
            List<Component> tooltip, TooltipFlag flags) {
        tooltip.add(Component.translatable("item.craftdistrict.access_card.color", colorName(color(stack))));
        tooltip.add(Component.translatable("item.craftdistrict.access_card.edit"));
    }
}
