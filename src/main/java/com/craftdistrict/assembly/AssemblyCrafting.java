package com.craftdistrict.assembly;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Pure inventory simulation first; only a complete, revalidated plan may be committed. */
public final class AssemblyCrafting {
    public static final int MIN_OPERATIONS = 1, MAX_OPERATIONS = 64;
    public enum Result { READY, MISSING_MATERIALS, INVENTORY_FULL, CRAFTED, STALE_RECIPE, BUSY, INVALID_QUANTITY }
    public record Plan(Result result, List<ItemStack> before, List<ItemStack> after, int[] available, int[] allocated) { }
    private AssemblyCrafting() { }

    public static boolean validQuantity(int operations) { return operations >= MIN_OPERATIONS && operations <= MAX_OPERATIONS; }
    public static int requiredCount(AssemblyIngredient ingredient, int operations) {
        if (!validQuantity(operations)) throw new IllegalArgumentException("Assembly quantity must be 1..64");
        return Math.multiplyExact(ingredient.count(), operations);
    }

    public static List<ItemStack> snapshot(Inventory inventory) {
        List<ItemStack> items = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) items.add(inventory.getItem(i).copy());
        return items;
    }

    public static boolean same(List<ItemStack> left, List<ItemStack> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) if (!ItemStack.matches(left.get(i), right.get(i))) return false;
        return true;
    }

    public static Plan plan(AssemblyRecipe recipe, List<ItemStack> inventory) {
        return plan(recipe, inventory, 1);
    }

    public static Plan plan(AssemblyRecipe recipe, List<ItemStack> inventory, int operations) {
        if (inventory.size() != 36) throw new IllegalArgumentException("Expected main inventory and hotbar");
        List<AssemblyIngredient> requirements = recipe.requirements();
        if (!validQuantity(operations)) return new Plan(Result.INVALID_QUANTITY, List.of(), List.of(), new int[requirements.size()], new int[requirements.size()]);
        List<ItemStack> before = inventory.stream().map(ItemStack::copy).toList();
        int count = requirements.size(), sink = 37 + count, nodes = sink + 1;
        int[][] capacity = new int[nodes][nodes];
        int[] available = new int[count], allocated = new int[count];
        int required = 0;
        for (int r = 0; r < count; r++) {
            AssemblyIngredient ingredient = requirements.get(r);
            int demand = requiredCount(ingredient, operations);
            capacity[37 + r][sink] = demand; required = Math.addExact(required, demand);
            for (int slot = 0; slot < 36; slot++) {
                ItemStack stack = before.get(slot);
                if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                    capacity[1 + slot][37 + r] = stack.getCount(); available[r] += stack.getCount();
                }
            }
        }
        for (int slot = 0; slot < 36; slot++) capacity[0][1 + slot] = before.get(slot).getCount();
        int[][] residual = Arrays.stream(capacity).map(int[]::clone).toArray(int[][]::new);
        int flow = 0;
        // Bounded max-flow handles overlapping tags without greedy reservation or double spending.
        while (true) {
            int[] parent = new int[nodes]; Arrays.fill(parent, -1); parent[0] = 0;
            int[] queue = new int[nodes]; int first = 0, end = 1; queue[0] = 0;
            while (first < end && parent[sink] == -1) {
                int at = queue[first++];
                for (int next = 1; next < nodes; next++) if (parent[next] == -1 && residual[at][next] > 0) {
                    parent[next] = at; queue[end++] = next;
                }
            }
            if (parent[sink] == -1) break;
            int amount = Integer.MAX_VALUE;
            for (int at = sink; at != 0; at = parent[at]) amount = Math.min(amount, residual[parent[at]][at]);
            for (int at = sink; at != 0; at = parent[at]) { residual[parent[at]][at] -= amount; residual[at][parent[at]] += amount; }
            flow += amount;
        }
        for (int r = 0; r < count; r++) allocated[r] = requiredCount(requirements.get(r), operations) - residual[37 + r][sink];
        if (flow != required) return new Plan(Result.MISSING_MATERIALS, before, List.of(), available, allocated);
        List<ItemStack> after = new ArrayList<>(36);
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = before.get(slot).copy();
            stack.shrink(capacity[0][1 + slot] - residual[0][1 + slot]);
            after.add(stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        ItemStack output = recipe.output();
        int remaining = Math.multiplyExact(output.getCount(), operations);
        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            ItemStack stack = after.get(slot);
            if (!stack.isEmpty() && sameKind(stack, output)) {
                int amount = Math.min(remaining, Math.max(0, Math.min(stack.getMaxStackSize(), 64) - stack.getCount()));
                stack.grow(amount); remaining -= amount;
            }
        }
        for (int slot = 0; slot < 36 && remaining > 0; slot++) if (after.get(slot).isEmpty()) {
            int amount = Math.min(remaining, Math.min(output.getMaxStackSize(), 64));
            ItemStack stack = output.copy(); stack.setCount(amount); after.set(slot, stack); remaining -= amount;
        }
        return new Plan(remaining == 0 ? Result.READY : Result.INVENTORY_FULL, before,
                remaining == 0 ? List.copyOf(after) : List.of(), available, allocated);
    }

    private static boolean sameKind(ItemStack a, ItemStack b) {
        //? if >=1.21 {
        return ItemStack.isSameItemSameComponents(a, b);
        //?} else {
        /*return ItemStack.isSameItemSameTags(a, b);
        *///?}
    }

    public static Result commit(Inventory inventory, Plan plan) {
        if (plan.result() != Result.READY) return plan.result();
        if (!same(plan.before(), snapshot(inventory))) return Result.BUSY;
        for (int slot = 0; slot < 36; slot++) if (!ItemStack.matches(plan.before().get(slot), plan.after().get(slot)))
            inventory.setItem(slot, plan.after().get(slot).copy());
        inventory.setChanged();
        return Result.CRAFTED;
    }
}
