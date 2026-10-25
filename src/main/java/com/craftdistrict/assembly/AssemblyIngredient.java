package com.craftdistrict.assembly;

import net.minecraft.world.item.crafting.Ingredient;

/** Quantities are separate from vanilla Ingredient so tags retain normal matching semantics. */
public record AssemblyIngredient(Ingredient ingredient, int count) {
    public static final int MAX_COUNT = 4096;

    public AssemblyIngredient {
        if (ingredient == null || ingredient.isEmpty() || count < 1 || count > MAX_COUNT) {
            throw new IllegalArgumentException("Assembly ingredient must be nonempty, with count 1.." + MAX_COUNT);
        }
    }
}
