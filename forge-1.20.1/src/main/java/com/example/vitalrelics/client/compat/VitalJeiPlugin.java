package com.example.vitalrelics.client.compat;

import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.relics.Acquisition;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JeiPlugin
public final class VitalJeiPlugin implements IModPlugin {
	private static final ResourceLocation UID =
			new ResourceLocation(Manifest.MODID, "jei_plugin");

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerRecipes(final IRecipeRegistration registration) {
		final List<CraftingRecipe> recipes = new ArrayList<>();

		for (final Map.Entry<String, Acquisition.Data.Crafting> entry :
				Acquisition.get().data.recipes.entrySet()) {

			final ResourceLocation id =
					new ResourceLocation(Manifest.MODID, entry.getKey());

			final ItemStack output = itemStack(id, entry.getValue().count);

			if (output.isEmpty())
				continue;

			final CraftingRecipe recipe = switch (entry.getValue().type) {
				case "shaped" -> shaped(id, entry.getValue(), output);
				case "shapeless" -> shapeless(id, entry.getValue(), output);
				default -> null;
			};

			if (recipe != null)
				recipes.add(recipe);
		}

		registration.addRecipes(RecipeTypes.CRAFTING, recipes);
	}

	private static CraftingRecipe shaped(
			final ResourceLocation id,
			final Acquisition.Data.Crafting definition,
			final ItemStack output) {

		final int width = definition.pattern.get(0).length();
		final int height = definition.pattern.size();
		final NonNullList<Ingredient> ingredients = NonNullList.create();

		for (final String row : definition.pattern) {
			for (int x = 0; x < width; ++x) {
				final char symbol = row.charAt(x);

				ingredients.add(symbol == ' '
						? Ingredient.EMPTY
						: ingredient(definition.key.get(String.valueOf(symbol))));
			}
		}

		return new ShapedRecipe(
				id,
				"",
				CraftingBookCategory.MISC,
				width,
				height,
				ingredients,
				output
		);
	}

	private static CraftingRecipe shapeless(
			final ResourceLocation id,
			final Acquisition.Data.Crafting definition,
			final ItemStack output) {

		final NonNullList<Ingredient> ingredients = NonNullList.create();

		for (final String itemId : definition.ingredients)
			ingredients.add(ingredient(itemId));

		return new ShapelessRecipe(
				id,
				"",
				CraftingBookCategory.MISC,
				output,
				ingredients
		);
	}

	private static Ingredient ingredient(final String itemId) {
		if (itemId == null)
			return Ingredient.EMPTY;

		final ResourceLocation id = ResourceLocation.tryParse(itemId);

		if (id == null || !BuiltInRegistries.ITEM.containsKey(id))
			return Ingredient.EMPTY;

		return Ingredient.of(BuiltInRegistries.ITEM.get(id));
	}

	private static ItemStack itemStack(final ResourceLocation id, final int count) {
		if (!BuiltInRegistries.ITEM.containsKey(id))
			return ItemStack.EMPTY;

		final Item item = BuiltInRegistries.ITEM.get(id);
		return new ItemStack(item, count);
	}
}
