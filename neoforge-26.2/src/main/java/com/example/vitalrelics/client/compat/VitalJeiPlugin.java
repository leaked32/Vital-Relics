package com.example.vitalrelics.client.compat;

import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.relics.Acquisition;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiShapedRecipeBuilder;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JeiPlugin
public final class VitalJeiPlugin implements IModPlugin {
	private static final Identifier UID =
			Identifier.fromNamespaceAndPath(Manifest.MODID, "jei_plugin");

	@Override
	public Identifier getPluginUid() {
		return UID;
	}

	@Override
	public void registerRecipes(final IRecipeRegistration registration) {
		final List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();

		for (final Map.Entry<String, Acquisition.Data.Crafting> entry :
				Acquisition.get().data.recipes.entrySet()) {

			final Identifier id =
					Identifier.fromNamespaceAndPath(Manifest.MODID, entry.getKey());

			final ItemStack output = itemStack(id, entry.getValue().count);

			if (output.isEmpty())
				continue;

			final CraftingRecipe recipe = switch (entry.getValue().type) {
				case "shaped" -> shaped(registration, entry.getValue(), output);
				case "shapeless" -> shapeless(entry.getValue(), output);
				default -> null;
			};

			if (recipe != null) {
				final ResourceKey<Recipe<?>> recipeKey =
						ResourceKey.create(Registries.RECIPE, id);

				recipes.add(new RecipeHolder<>(recipeKey, recipe));
			}
		}

		registration.addRecipes(RecipeTypes.CRAFTING, recipes);
	}

	private static CraftingRecipe shaped(
			final IRecipeRegistration registration,
			final Acquisition.Data.Crafting definition,
			final ItemStack output) {

		final SlotDisplay outputDisplay =
				new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(output));

		final IJeiShapedRecipeBuilder builder = registration
				.getVanillaRecipeFactory()
				.createShapedRecipeBuilder(CraftingBookCategory.MISC, outputDisplay);

		for (final Map.Entry<String, String> entry : definition.key.entrySet())
			builder.define(entry.getKey().charAt(0), ingredient(entry.getValue()));

		for (final String row : definition.pattern)
			builder.pattern(row);

		return builder.build();
	}

	private static CraftingRecipe shapeless(
			final Acquisition.Data.Crafting definition,
			final ItemStack output) {

		final List<Ingredient> ingredients = definition.ingredients.stream()
				.map(VitalJeiPlugin::ingredient)
				.toList();

		return new ShapelessRecipe(
				new Recipe.CommonInfo(false),
				new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
				ItemStackTemplate.fromNonEmptyStack(output),
				ingredients
		);
	}

	private static Ingredient ingredient(final String itemId) {
		if (itemId == null)
			return Ingredient.of();

		final Identifier id = Identifier.tryParse(itemId);

		if (id == null)
			return Ingredient.of();

		return BuiltInRegistries.ITEM
				.get(id)
				.map(holder -> Ingredient.of(holder.value()))
				.orElseGet(Ingredient::of);
	}

	private static ItemStack itemStack(final Identifier id, final int count) {
		return BuiltInRegistries.ITEM
				.get(id)
				.map(holder -> new ItemStack(holder.value(), count))
				.orElse(ItemStack.EMPTY);
	}
}
