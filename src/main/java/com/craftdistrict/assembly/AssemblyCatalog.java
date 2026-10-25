package com.craftdistrict.assembly;

import com.craftdistrict.registry.CraftDistrictAssembly;
import com.craftdistrict.registry.DecorationCollection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;
import java.util.*;

/** Membership is derived solely from the existing Creative collections. */
public final class AssemblyCatalog {
    public record Entry(ResourceLocation id, AssemblyRecipe recipe) { }
    private AssemblyCatalog() { }

    public static List<DecorationCollection> collections() { return Membership.COLLECTIONS; }
    public static List<DecorationCollection> collectionsFor(Item item) { return Membership.BY_ITEM.getOrDefault(item, List.of()); }
    public static boolean isDecoration(Item item) { return Membership.BY_ITEM.containsKey(item); }
    public static String normalizeSearch(String value) {
        // Fold German sharp S as well as Turkish dotted/dotless I consistently.
        return value.strip().toUpperCase(Locale.ROOT).toLowerCase(Locale.ROOT).replace("\u00df", "ss").replace("\u0307", "").replace('\u0131', 'i');
    }
    public static boolean matches(Item item, DecorationCollection collection, String normalizedName, String normalizedQuery) {
        return (collection == null || collectionsFor(item).contains(collection)) && normalizedName.contains(normalizedQuery);
    }

    private static final class Membership {
        static final List<DecorationCollection> COLLECTIONS = Arrays.stream(DecorationCollection.values())
                .filter(collection -> collection != DecorationCollection.SURVIVAL_CRAFTING).toList();
        static final Map<Item, List<DecorationCollection>> BY_ITEM = build();
        private static Map<Item, List<DecorationCollection>> build() {
            Map<Item, List<DecorationCollection>> result = new IdentityHashMap<>();
            for (DecorationCollection collection : COLLECTIONS) {
                for (Item item : collection.items()) result.computeIfAbsent(item, ignored -> new ArrayList<>()).add(collection);
            }
            result.replaceAll((item, values) -> List.copyOf(values));
            return Collections.unmodifiableMap(result);
        }
    }

    public static AssemblyRecipe find(RecipeManager manager, ResourceLocation id) {
        //? if >=1.21 {
        var recipe = manager.byKey(id).map(holder -> holder.value()).orElse(null);
        //?} else {
        /*var recipe = manager.byKey(id).orElse(null);
        *///?}
        return recipe instanceof AssemblyRecipe assembly ? assembly : null;
    }

    public static List<Entry> entries(RecipeManager manager) {
        List<Entry> entries = new ArrayList<>();
        //? if >=1.21 {
        manager.getAllRecipesFor(CraftDistrictAssembly.RECIPE_TYPE.get()).forEach(holder -> entries.add(new Entry(holder.id(), holder.value())));
        //?} else {
        /*manager.getAllRecipesFor(CraftDistrictAssembly.RECIPE_TYPE.get()).forEach(recipe -> entries.add(new Entry(recipe.getId(), recipe)));
        *///?}
        entries.sort(Comparator.comparing(entry -> entry.id().toString()));
        Set<Item> outputs = Collections.newSetFromMap(new IdentityHashMap<>());
        entries.removeIf(entry -> !outputs.add(entry.recipe().outputItem()));
        return List.copyOf(entries);
    }

    public static boolean same(List<Entry> left, List<Entry> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            if (!left.get(i).id().equals(right.get(i).id()) || left.get(i).recipe() != right.get(i).recipe()) return false;
        }
        return true;
    }
}
