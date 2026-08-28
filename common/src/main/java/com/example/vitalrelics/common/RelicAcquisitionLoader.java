package com.example.vitalrelics.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.vitalrelics.common.utils.ConfigurationFiles.load_external_file;

public class RelicAcquisitionLoader {
	private RelicAcquisitionLoader() {}

	public final static RelicAcquisitionLoader INSTANCE = new RelicAcquisitionLoader();
	public final Acquisition data = new Acquisition();

	public void load(final Path external_path) {
		if (external_path == null) {
			throw new RuntimeException("RelicLoader#load `external_path` cannot be null");
		}

		final Map<String, Object> root = load_external_file(
				Manifest.INTERNAL_PATH_TO_RECIPES,
				external_path,
				Manifest.OPT_RECIPES_VER
		);

		data.recipes.clear();
		data.loot.clear();
		data.undefined.clear();

		parseRecipes(root.get("recipes"));
		parseLoot(root.get("loot"));
		parseUndefined(root.get("undefined"));
	}

	private void parseRecipes(final Object raw) {
		if (raw == null)
			return;

		if (!(raw instanceof Map<?, ?> recipes))
			throw new IllegalArgumentException("'recipes' must be an object");

		for (final var entry : recipes.entrySet()) {
			if (!(entry.getKey() instanceof String relicId))
				throw new IllegalArgumentException("Recipe IDs must be strings");

			if (!(entry.getValue() instanceof Map<?, ?> map))
				throw new IllegalArgumentException("Recipe '" + relicId + "' must be an object");

			final Acquisition.Crafting recipe = new Acquisition.Crafting();

			if (!(map.get("type") instanceof String type))
				throw new IllegalArgumentException("Recipe '" + relicId + "' requires type");

			recipe.type = type;

			if (map.get("count") instanceof Number count)
				recipe.count = count.intValue();

			if (recipe.count <= 0)
				throw new IllegalArgumentException("Recipe count must be > 0");

			if ("shaped".equals(type)) {
				parseStringList(recipe.pattern, map.get("pattern"));

				if (map.get("key") instanceof Map<?, ?> keys) {
					for (final var key : keys.entrySet()) {
						if (!(key.getKey() instanceof String symbol) || symbol.length() != 1)
							throw new IllegalArgumentException("Recipe key must be one character");

						if (!(key.getValue() instanceof String item))
							throw new IllegalArgumentException("Recipe key value must be an item ID");

						recipe.key.put(symbol, item);
					}
				}

				trimPattern(recipe.pattern);
			} else if ("shapeless".equals(type)) {
				parseStringList(recipe.ingredients, map.get("ingredients"));
			} else {
				throw new IllegalArgumentException(
						"Unsupported recipe type '" + type + "' for " + relicId);
			}

			data.recipes.put(relicId, recipe);
		}
	}

	private void parseLoot(final Object raw) {
		if (raw == null)
			return;

		if (!(raw instanceof Map<?, ?> loot))
			throw new IllegalArgumentException("'loot' must be an object");

		for (final var entry : loot.entrySet()) {
			if (!(entry.getKey() instanceof String relicId))
				throw new IllegalArgumentException("Loot IDs must be strings");

			if (!(entry.getValue() instanceof List<?> rules))
				throw new IllegalArgumentException("Loot '" + relicId + "' must be an array");

			final List<Acquisition.Loot> out = new ArrayList<>();

			for (final Object rawRule : rules) {
				if (!(rawRule instanceof Map<?, ?> map))
					throw new IllegalArgumentException("Loot rule must be an object");

				if (!(map.get("table") instanceof String table))
					throw new IllegalArgumentException("Loot rule requires table");

				if (!(map.get("chance") instanceof Number chance))
					throw new IllegalArgumentException("Loot rule requires chance");

				final Acquisition.Loot rule = new Acquisition.Loot();
				rule.table = table;
				rule.chance = chance.doubleValue();

				if (rule.chance < 0.0 || rule.chance > 1.0)
					throw new IllegalArgumentException("Loot chance must be between 0 and 1");

				out.add(rule);
			}

			data.loot.put(relicId, out);
		}
	}

	private void parseUndefined(final Object raw) {
		parseStringList(data.undefined, raw);
	}

	private static void parseStringList(final List<String> out, final Object raw) {
		if (raw == null)
			return;

		if (!(raw instanceof List<?> list))
			throw new IllegalArgumentException("Expected an array");

		for (final Object value : list) {
			if (!(value instanceof String string))
				throw new IllegalArgumentException("Array entries must be strings");

			out.add(string);
		}
	}

	private static void trimPattern(final List<String> pattern) {
		while (!pattern.isEmpty() && pattern.get(0).isBlank())
			pattern.remove(0);

		while (!pattern.isEmpty() && pattern.get(pattern.size() - 1).isBlank())
			pattern.remove(pattern.size() - 1);

		if (pattern.isEmpty())
			throw new IllegalArgumentException("Shaped recipe pattern cannot be empty");

		int left = Integer.MAX_VALUE;
		int right = -1;

		for (final String row : pattern) {
			for (int i = 0; i < row.length(); ++i) {
				if (row.charAt(i) != ' ') {
					left = Math.min(left, i);
					right = Math.max(right, i);
				}
			}
		}

		if (right < left)
			throw new IllegalArgumentException("Shaped recipe pattern cannot be empty");

		for (int i = 0; i < pattern.size(); ++i) {
			final String row = pattern.get(i);
			final String padded = row + " ".repeat(Math.max(0, right + 1 - row.length()));
			pattern.set(i, padded.substring(left, right + 1));
		}
	}
}