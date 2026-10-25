package com.craftdistrict.assembly;

import com.craftdistrict.registry.CraftDistrictAssembly;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?} else {
/*import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
*///?}
import java.util.List;

public final class AssemblyRecipe implements Recipe<
        //? if >=1.21 {
        RecipeInput
        //?} else {
        /*Container
        *///?}
        > {
    public static final int MAX_INGREDIENTS = 8;
    private final List<AssemblyIngredient> requirements;
    private final ItemStack output;
    //? if <1.21 {
    /*private ResourceLocation id;
    public void setId(ResourceLocation id) { this.id = id; }
    @Override public ResourceLocation getId() { return id; }
    *///?}

    public AssemblyRecipe(List<AssemblyIngredient> requirements, ItemStack output) {
        if (requirements.isEmpty() || requirements.size() > MAX_INGREDIENTS) {
            throw new IllegalArgumentException("Assembly recipes require 1..8 ingredients");
        }
        if (output.isEmpty() || output.getCount() > output.getMaxStackSize() || !AssemblyCatalog.isDecoration(output.getItem())) {
            throw new IllegalArgumentException("Assembly output must be an existing CraftDistrict decoration and fit one stack");
        }
        this.requirements = List.copyOf(requirements);
        this.output = output.copy();
    }

    public List<AssemblyIngredient> requirements() { return requirements; }
    public ItemStack output() { return output.copy(); }
    public Item outputItem() { return output.getItem(); }

    // Assembly recipes never match a vanilla crafting grid or appear in its recipe book.
    //? if >=1.21 {
    @Override public boolean matches(RecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return output(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return output(); }
    //?} else {
    /*@Override public boolean matches(Container input, Level level) { return false; }
    @Override public ItemStack assemble(Container input, RegistryAccess registries) { return output(); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return output(); }
    *///?}
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public RecipeSerializer<?> getSerializer() { return CraftDistrictAssembly.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return CraftDistrictAssembly.RECIPE_TYPE.get(); }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        requirements.forEach(requirement -> result.add(requirement.ingredient()));
        return result;
    }
}
