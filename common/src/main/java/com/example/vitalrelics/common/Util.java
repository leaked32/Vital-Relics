package com.example.vitalrelics.common;

import com.example.vitalrelics.common.utils.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Util {

	public static void copy_to_external(
			final String internal,
			final Path external) {

		if (Files.exists(external))
			return;

		try {
			final Path parent = external.getParent();

			if (parent != null)
				Files.createDirectories(parent);

			try (InputStream in =
						 RelicLoader.class.getClassLoader()
								 .getResourceAsStream(internal)) {

				if (in == null)
					throw new IllegalStateException(
							"Bundled resource not found: " + internal
					);

				Files.copy(in, external);
			}
		} catch (IOException exception) {
			throw new RuntimeException(
					"Failed to create external relic configuration",
					exception
			);
		}
	}

	public static String read_external_file(final Path path) {

		final String text;
		try {
			if (Files.isRegularFile(path)) {
				text = Files.readString(path, StandardCharsets.UTF_8);
			} else {
				throw new RuntimeException("`relics.json` is not a regular file");
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load relics.json", e);
		}
		return text;
	}

	public static Map<String, Object> load_external_file(
			final String internal_path,
			final Path external_path,
			final String expected_version) {

		try {
			copy_to_external(internal_path, external_path);

			Map<String, Object> root = parse_configuration(external_path);

			if (!Boolean.TRUE.equals(root.get("customized"))) {
				/*
				 * Non-customized files belong to Vital Relics.
				 * Always refresh them from the bundled configuration.
				 */
				System.out.println("`load_external_file` overwrote the old file. ");

				Files.delete(external_path);
				copy_to_external(internal_path, external_path);

				root = parse_configuration(external_path);
				set_version(root, expected_version);
				save_configuration(external_path, root);

				return root;
			}

			/*
			 * Pre-versioning customized files are assumed to use the current
			 * format. Preserve their contents and only add metadata.
			 */
			if (!root.containsKey("_meta")) {
				set_version(root, expected_version);
				save_configuration(external_path, root);

				System.out.println(
						"[Vital Relics] Added version metadata to " +
								external_path.toAbsolutePath()
				);

				return root;
			} else {
				System.out.println("`load_external_file` preserved the old file. ");
			}

			validate_version(root, expected_version);
			return root;
		} catch (RuntimeException | IOException exception) {
			throw ConfigurationException.configuration_error(external_path, exception);
		}
	}

	private static Map<String, Object> parse_configuration(final Path path) {
		return Json.parseObject(read_external_file(path));
	}

	private static void save_configuration(
			final Path path,
			final Map<String, Object> root) throws IOException {

		Files.writeString(
				path,
				Json.stringify(root),
				StandardCharsets.UTF_8
		);
	}

	private static void set_version(
			final Map<String, Object> root,
			final String version) {

		final Map<String, Object> meta = new java.util.LinkedHashMap<>();
		meta.put("version", version);

		/*
		 * Rebuild so _meta stays near the beginning instead of being appended
		 * after the entire configuration.
		 */
		final Map<String, Object> upgraded = new java.util.LinkedHashMap<>();
		upgraded.put("_meta", meta);
		upgraded.putAll(root);

		root.clear();
		root.putAll(upgraded);
	}

	private static void validate_version(
			final Map<String, Object> root,
			final String expected_version) {

		final Object raw_meta = root.get("_meta");

		if (!(raw_meta instanceof Map<?, ?> meta))
			throw new IllegalArgumentException("'_meta' must be a JSON object");

		final Object raw_version = meta.get("version");

		if (!(raw_version instanceof String version))
			throw new IllegalArgumentException("'_meta.version' must be a string");

		if (!expected_version.equals(version)) {
			throw new IllegalArgumentException(
					"Configuration version '" + version +
							"' is incompatible with required version '" +
							expected_version + "'"
			);
		}
	}

//	public static Map<String, Object> load_external_file(
//			final String internal_path,
//			final Path external_path,
//			final String expected_version) {
//
//		try {
//			copy_to_external(internal_path, external_path);
//
//			Map<String, Object> root =
//					Json.parseObject(read_external_file(external_path));
//
//			final Object customized = root.get("customized");
//
//			if (!Boolean.TRUE.equals(customized)) {
//				// System.out.println("`load_external_file` overwrote the old file. ");
//
//				Files.delete(external_path);
//				copy_to_external(internal_path, external_path);
//
//				root = Json.parseObject(read_external_file(external_path));
//			}
////			else {
//				// System.out.println("`load_external_file` preserved the old file. ");
////			}
//
//			return root;
//		} catch (IOException exception) {
//			throw new RuntimeException("Failed to reset external relic configuration", exception);
//		}
//	}


}
