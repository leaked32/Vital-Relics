package com.example.vitalrelics.common.materials;


import com.example.vitalrelics.common.Manifest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.vitalrelics.common.utils.ConfigurationFiles.load_external_file;

public final class MaterialLoader {

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
				Manifest.INTERNAL_PATH_TO_MATERIAL, externalPath, Manifest.OPT_MATERIAL_VER);

		if (!(root.get("materials") instanceof List<?> entries))
			throw new IllegalArgumentException("'materials' must be a JSON array");

		final MaterialLoader loader = new MaterialLoader();

		for (final Object entry : entries) {
			if (!(entry instanceof Map<?, ?> rawMaterial))
				throw new IllegalArgumentException("Each material must be a JSON object");

			final Material material = loadMaterial(rawMaterial);
			if (loader.find(material.id) != null)
				throw new IllegalArgumentException("Duplicate material id: " + material.id);
			loader.materials_.add(material);
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
		if (!material.id.matches("[a-z0-9_]+"))
			throw new IllegalArgumentException("Invalid material id: " + material.id);
		material.texture = requiredString(rawMaterial, "texture");

		setString(rawMaterial, "display_name", value -> material.display_name = value);
		setString(rawMaterial, "tooltip", value -> material.tooltip = value);
		setString(rawMaterial, "rarity", value -> material.rarity = value);

		final Object rawMaxStackSize = rawMaterial.get("max_stack_size");
		if (rawMaxStackSize != null) {
			if (!(rawMaxStackSize instanceof Number value))
				throw new IllegalArgumentException("max_stack_size must be a number");

			final double size = value.doubleValue();
			// Keep materials compatible with the oldest supported Item.Properties API.
			if (!Double.isFinite(size) || size != Math.rint(size) || size < 1 || size > 64)
				throw new IllegalArgumentException("max_stack_size must be an integer from 1 to 64");
			material.max_stack_size = (int) size;
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
