package com.craftdistrict.assembly;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import java.util.ArrayList;
import java.util.List;
//? if >=1.21 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
//?} else {
/*import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.ShapedRecipe;
*///?}

/** Uses vanilla recipe-manager loading, synchronization and datapack replacement. */
public final class AssemblyRecipeSerializer implements RecipeSerializer<AssemblyRecipe> {
    //? if >=1.21 {
    private static final Codec<AssemblyIngredient> INGREDIENT = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(AssemblyIngredient::ingredient),
            Codec.intRange(1, AssemblyIngredient.MAX_COUNT).fieldOf("count").forGetter(AssemblyIngredient::count)
    ).apply(instance, AssemblyIngredient::new));
    private static final MapCodec<AssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            INGREDIENT.listOf(1, AssemblyRecipe.MAX_INGREDIENTS).fieldOf("ingredients").forGetter(AssemblyRecipe::requirements),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AssemblyRecipe::output)
    ).apply(instance, AssemblyRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, AssemblyRecipe> STREAM = new StreamCodec<>() {
        @Override public AssemblyRecipe decode(RegistryFriendlyByteBuf buffer) {
            int size = checkedSize(buffer.readVarInt());
            List<AssemblyIngredient> requirements = new ArrayList<>(size);
            for (int i = 0; i < size; i++) requirements.add(new AssemblyIngredient(
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt()));
            return new AssemblyRecipe(requirements, ItemStack.STREAM_CODEC.decode(buffer));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, AssemblyRecipe recipe) {
            buffer.writeVarInt(recipe.requirements().size());
            for (AssemblyIngredient requirement : recipe.requirements()) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, requirement.ingredient());
                buffer.writeVarInt(requirement.count());
            }
            ItemStack.STREAM_CODEC.encode(buffer, recipe.output());
        }
    };
    @Override public MapCodec<AssemblyRecipe> codec() { return CODEC; }
    @Override public StreamCodec<RegistryFriendlyByteBuf, AssemblyRecipe> streamCodec() { return STREAM; }
    //?} else {
    /*@Override public AssemblyRecipe fromJson(ResourceLocation id, JsonObject json) {
        var array = GsonHelper.getAsJsonArray(json, "ingredients");
        checkedSize(array.size());
        List<AssemblyIngredient> requirements = new ArrayList<>(array.size());
        for (var entry : array) {
            var object = entry.getAsJsonObject();
            requirements.add(new AssemblyIngredient(Ingredient.fromJson(object.get("ingredient")), GsonHelper.getAsInt(object, "count")));
        }
        AssemblyRecipe recipe = new AssemblyRecipe(requirements, ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")));
        recipe.setId(id);
        return recipe;
    }
    @Override public AssemblyRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        int size = checkedSize(buffer.readVarInt());
        List<AssemblyIngredient> requirements = new ArrayList<>(size);
        for (int i = 0; i < size; i++) requirements.add(new AssemblyIngredient(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
        AssemblyRecipe recipe = new AssemblyRecipe(requirements, buffer.readItem());
        recipe.setId(id);
        return recipe;
    }
    @Override public void toNetwork(FriendlyByteBuf buffer, AssemblyRecipe recipe) {
        buffer.writeVarInt(recipe.requirements().size());
        for (AssemblyIngredient requirement : recipe.requirements()) {
            requirement.ingredient().toNetwork(buffer);
            buffer.writeVarInt(requirement.count());
        }
        buffer.writeItem(recipe.output());
    }
    *///?}
    private static int checkedSize(int size) {
        if (size < 1 || size > AssemblyRecipe.MAX_INGREDIENTS) throw new IllegalArgumentException("Invalid assembly ingredient count");
        return size;
    }
}
