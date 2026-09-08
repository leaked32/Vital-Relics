package com.example.vitalrelics.common.materials;


import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.vitalrelics.common.utils.ConfigurationFiles.load_external_file;

public final class MaterialLoader {
	public static final String INTERNAL_PATH = "vitalrelics/materials.json";
	public static final String CONFIGURATION_VERSION = "0.1.0";

	public static final class Material {
		public String id;
		public String display_name = null;
		public String tooltip = "Crafting material";
		public String texture;
		public String rarity = "common";
		public int max_stack_size = 64;
	}

	private static MaterialLoader instance = null;

	private final List<Material> materials_ = new ArrayList<>();

	private MaterialLoader() {
	}

	public static void load(final Path externalPath) {
		if (instance != null)
			throw new IllegalStateException("MaterialLoader has already been initialized.");

		if (externalPath == null)
			throw new IllegalArgumentException("MaterialLoader#load externalPath cannot be null");

		final Map<String, Object> root = load_external_file(
				INTERNAL_PATH, externalPath, CONFIGURATION_VERSION);

		if (!(root.get("materials") instanceof List<?> entries))
			throw new IllegalArgumentException("'materials' must be a JSON array");

		final MaterialLoader loader = new MaterialLoader();

		for (final Object entry : entries) {
			if (!(entry instanceof Map<?, ?> rawMaterial))
				throw new IllegalArgumentException("Each material must be a JSON object");

			loader.materials_.add(loadMaterial(rawMaterial));
		}

		instance = loader;
	}

	public static MaterialLoader get() {
		if (instance == null)
			throw new IllegalStateException("MaterialLoader has not been initialized.");

		return instance;
	}

	public List<Material> materials() {
		return List.copyOf(materials_);
	}

	public Material find(final String id) {
		for (final Material material : materials_) {
			if (material.id.equals(id))
				return material;
		}

		return null;
	}

	private static Material loadMaterial(final Map<?, ?> rawMaterial) {
		final Material material = new Material();

		material.id = requiredString(rawMaterial, "id");
		material.texture = requiredString(rawMaterial, "texture");

		setString(rawMaterial, "display_name", value -> material.display_name = value);
		setString(rawMaterial, "tooltip", value -> material.tooltip = value);
		setString(rawMaterial, "rarity", value -> material.rarity = value);

		final Object rawMaxStackSize = rawMaterial.get("max_stack_size");
		if (rawMaxStackSize != null) {
			if (!(rawMaxStackSize instanceof Number value))
				throw new IllegalArgumentException("max_stack_size must be a number");

			material.max_stack_size = value.intValue();

			if (material.max_stack_size < 1 || material.max_stack_size > 99)
				throw new IllegalArgumentException("max_stack_size must be between 1 and 99");
		}

		return material;
	}

	private static String requiredString(final Map<?, ?> source, final String key) {
		final Object value = source.get(key);

		if (!(value instanceof String string) || string.isBlank())
			throw new IllegalArgumentException(key + " must be a non-empty string");

		return string;
	}

	private static void setString(
			final Map<?, ?> source, final String key,
			final java.util.function.Consumer<String> consumer
	) {

		final Object value = source.get(key);

		if (value instanceof String string)
			consumer.accept(string);
		else if (value != null)
			throw new IllegalArgumentException(key + " must be a string");
	}
}
