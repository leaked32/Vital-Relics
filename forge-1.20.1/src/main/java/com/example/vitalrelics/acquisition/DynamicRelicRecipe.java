package com.example.vitalrelics.acquisition;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Acquisition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DynamicRelicRecipe extends CustomRecipe {
	public DynamicRelicRecipe(
			final ResourceLocation id,
			final CraftingBookCategory category) {
		super(id, category);
	}

	@Override
	public boolean matches(final CraftingContainer input, final Level level) {
		return findMatch(input) != null;
	}

	@Override
	public ItemStack assemble(
			final CraftingContainer input,
			final RegistryAccess registryAccess) {

		final Match match = findMatch(input);
		if (match == null)
			return ItemStack.EMPTY;

		final ResourceLocation id =
				new ResourceLocation(VitalRelics.MODID, match.id);

		if (!BuiltInRegistries.ITEM.containsKey(id))
			return ItemStack.EMPTY;

		final Item item = BuiltInRegistries.ITEM.get(id);
		return new ItemStack(item, match.recipe.count);
	}

	@Override
	public boolean canCraftInDimensions(final int width, final int height) {
		return width >= 2 && height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return VitalRelics.DYNAMIC_RELIC_RECIPE.get();
	}

	private static Match findMatch(final CraftingContainer input) {
		for (final var entry : VitalRelics.acquisition.data.recipes.entrySet()) {
			final Acquisition.Crafting recipe = entry.getValue();

			if ("shaped".equals(recipe.type) && matchesShaped(input, recipe))
				return new Match(entry.getKey(), recipe);

			if ("shapeless".equals(recipe.type) && matchesShapeless(input, recipe))
				return new Match(entry.getKey(), recipe);
		}

		return null;
	}

	private static boolean matchesShapeless(
			final CraftingContainer input,
			final Acquisition.Crafting recipe) {

		final Map<String, Integer> actual = new HashMap<>();
		final Map<String, Integer> expected = new HashMap<>();

		for (int i = 0; i < input.getContainerSize(); ++i) {
			final ItemStack stack = input.getItem(i);

			if (!stack.isEmpty())
				actual.merge(itemId(stack), 1, Integer::sum);
		}

		for (final String id : recipe.ingredients)
			expected.merge(id, 1, Integer::sum);

		return actual.equals(expected);
	}

	private static boolean matchesShaped(
			final CraftingContainer input,
			final Acquisition.Crafting recipe) {

		final Grid grid = trim(input);

		if (grid.width != width(recipe.pattern) ||
				grid.height != recipe.pattern.size())
			return false;

		return matches(grid, recipe, false) || matches(grid, recipe, true);
	}

	private static boolean matches(
			final Grid grid,
			final Acquisition.Crafting recipe,
			final boolean mirror) {

		for (int y = 0; y < grid.height; ++y) {
			final String row = recipe.pattern.get(y);

			for (int x = 0; x < grid.width; ++x) {
				final int patternX = mirror ? grid.width - 1 - x : x;
				final char symbol = row.charAt(patternX);
				final ItemStack stack = grid.items.get(y * grid.width + x);

				if (symbol == ' ') {
					if (!stack.isEmpty())
						return false;
					continue;
				}

				final String expected = recipe.key.get(String.valueOf(symbol));

				if (expected == null || stack.isEmpty() || !expected.equals(itemId(stack)))
					return false;
			}
		}

		return true;
	}

	private static Grid trim(final CraftingContainer input) {
		int minX = input.getWidth();
		int minY = input.getHeight();
		int maxX = -1;
		int maxY = -1;

		for (int y = 0; y < input.getHeight(); ++y) {
			for (int x = 0; x < input.getWidth(); ++x) {
				final ItemStack stack = input.getItem(y * input.getWidth() + x);

				if (!stack.isEmpty()) {
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}

		if (maxX < minX)
			return new Grid(0, 0, List.of());

		final int width = maxX - minX + 1;
		final int height = maxY - minY + 1;
		final List<ItemStack> items = new ArrayList<>(width * height);

		for (int y = minY; y <= maxY; ++y)
			for (int x = minX; x <= maxX; ++x)
				items.add(input.getItem(y * input.getWidth() + x));

		return new Grid(width, height, items);
	}

	private static int width(final List<String> pattern) {
		return pattern.isEmpty() ? 0 : pattern.get(0).length();
	}

	private static String itemId(final ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private record Match(String id, Acquisition.Crafting recipe) {}
	private record Grid(int width, int height, List<ItemStack> items) {}
}