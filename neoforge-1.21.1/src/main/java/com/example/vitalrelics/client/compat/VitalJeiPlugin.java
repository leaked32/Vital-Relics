package com.example.vitalrelics.client.compat;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Acquisition;
import com.example.vitalrelics.common.Manifest;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiShapedRecipeBuilder;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JeiPlugin
public final class VitalJeiPlugin implements IModPlugin {
	private static final ResourceLocation UID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "jei_plugin");

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerRecipes(final IRecipeRegistration registration) {
		final List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();

		for (final Map.Entry<String, Acquisition.Crafting> entry :
				VitalRelics.acquisition.data.recipes.entrySet()) {

			final ResourceLocation id =
					ResourceLocation.fromNamespaceAndPath(Manifest.MODID, entry.getKey());

			final ItemStack output = itemStack(id, entry.getValue().count);

			if (output.isEmpty())
				continue;

			final CraftingRecipe recipe = switch (entry.getValue().type) {
				case "shaped" -> shaped(registration, entry.getValue(), output);
				case "shapeless" -> shapeless(entry.getValue(), output);
				default -> null;
			};

			if (recipe != null)
				recipes.add(new RecipeHolder<>(id, recipe));
		}

		registration.addRecipes(RecipeTypes.CRAFTING, recipes);
	}

	private static CraftingRecipe shaped(
			final IRecipeRegistration registration,
			final Acquisition.Crafting definition,
			final ItemStack output) {

		final IJeiShapedRecipeBuilder builder = registration
				.getVanillaRecipeFactory()
				.createShapedRecipeBuilder(CraftingBookCategory.MISC, List.of(output));

		for (final Map.Entry<String, String> entry : definition.key.entrySet())
			builder.define(entry.getKey().charAt(0), ingredient(entry.getValue()));

		for (final String row : definition.pattern)
			builder.pattern(row);

		return builder.build();
	}

	private static CraftingRecipe shapeless(
			final Acquisition.Crafting definition,
			final ItemStack output) {

		final List<Ingredient> ingredients = definition.ingredients.stream()
				.map(VitalJeiPlugin::ingredient)
				.toList();

		return new ShapelessRecipe(
				"",
				CraftingBookCategory.MISC,
				output,
				NonNullList.copyOf(ingredients)
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