package com.example.vitalrelics.common;

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
			final Path external_path) {

		try {
			copy_to_external(internal_path, external_path);

			Map<String, Object> root =
					Json.parseObject(read_external_file(external_path));

			final Object customized = root.get("customized");

			if (!Boolean.TRUE.equals(customized)) {
				// System.out.println("`load_external_file` overwrote the old file. ");

				Files.delete(external_path);
				copy_to_external(internal_path, external_path);

				root = Json.parseObject(read_external_file(external_path));
			}
//			else {
				// System.out.println("`load_external_file` preserved the old file. ");
//			}

			return root;
		} catch (IOException exception) {
			throw new RuntimeException("Failed to reset external relic configuration", exception);
		}
	}


}
