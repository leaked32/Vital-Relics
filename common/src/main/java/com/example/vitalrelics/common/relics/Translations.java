package com.example.vitalrelics.common.relics;

import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.utils.ConfigurationFiles;
import com.example.vitalrelics.common.utils.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class Translations {
	private static Translations translations = null;

	Translations() {}

	public static void load(final Path directory) {
		translations = new Translations();

		final Map<String, Map<String, String>> loaded = new LinkedHashMap<>();

		for (final String locale : Manifest.DEFAULT_LOCALES) {
			final Path external = directory.resolve(locale + ".json");

			final Map<String, Object> root = ConfigurationFiles.load_external_file(
					Manifest.INTERNAL_PATH_TO_LANG(locale),
					external,
					Manifest.OPT_LANG_VER
			);

			loaded.put(locale, readTable(root, external));
		}

		/*
		 * Load additional user-created locales. These have no bundled file
		 * against which they can be reset.
		 */
		try (Stream<Path> files = Files.list(directory)) {
			files.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith(".json"))
					.filter(path -> !loaded.containsKey(localeFrom(path)))
					.forEach(path -> loaded.put(
							localeFrom(path),
							readTable(
									Json.parseObject(ConfigurationFiles.read_external_file(path)),
									path
							)
					));
		} catch (IOException exception) {
			throw new RuntimeException(
					"Failed to load Vital Relics translations",
					exception
			);
		}

		translations.tables = Map.copyOf(loaded);
	}



	public static Translations get() {
		if (translations == null) {
			throw new RuntimeException("RelicTranslations has not been initialized yet. ");
		}

		return translations;
	}

	private volatile Map<String, Map<String, String>> tables =
			Map.of();

	private volatile String selectedLocale = "en_us";


	public void setSelectedLocale(final String locale) {
		selectedLocale = normalize(locale);
	}

	public String translate(
			final String key,
			final String fallback) {

		if (key == null)
			return fallback;

		final Map<String, String> selected =
				tables.get(selectedLocale);

		if (selected != null && selected.containsKey(key))
			return selected.get(key);

		final Map<String, String> english = tables.get("en_us");

		if (english != null && english.containsKey(key))
			return english.get(key);

		return fallback;
	}

	public static Map<String, String> readTable(
			final Map<String, Object> root,
			final Path path) {

		final Map<String, String> result = new LinkedHashMap<>();

		for (final var entry : root.entrySet()) {
			if (entry.getKey().equals("_meta")) {
				continue;
			}

			if (entry.getKey().equals("customized")) {
				// TODO, throw an exception since it should be in `_meta` now.
				throw new RuntimeException("customized should be in `_meta` now.");
			}

			if (!(entry.getValue() instanceof String value)) {
				throw new IllegalArgumentException(
						"Translation '" + entry.getKey() +
								"' in " + path.getFileName() +
								" must be a string"
				);
			}

			result.put(entry.getKey(), value);
		}

		return Map.copyOf(result);
	}

	public static String localeFrom(final Path path) {
		final String fileName = path.getFileName().toString();

		return normalize(fileName.substring(
				0,
				fileName.length() - ".json".length()
		));
	}

	public static String normalize(final String locale) {
		return locale.toLowerCase(Locale.ROOT).replace('-', '_');
	}
}