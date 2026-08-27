package com.example.vitalrelics.common;

import java.util.List;

public class Manifest {
	public static final String MODID = "vitalrelics";
	// public static final String VERSION = "0.6.5";

	// Update them only when definitions have changed.
	public static final String OPT_RELICS_VER = "0.6.5";
	public static final String OPT_RECIPES_VER = "0.6.2";
	public static final String OPT_LANG_VER = "0.6.2";

	public static final String INTERNAL_PATH_TO_RELICS = "vitalrelics/relics.json";
	public static final String INTERNAL_PATH_TO_RECIPES = "vitalrelics/recipes.json";


	public static final List<String> DEFAULT_LOCALES = List.of(
			"en_us",
			"zh_cn",
			"zh_tw",
			"ja_jp"
	);

	public static String INTERNAL_PATH_TO_LANG(final String locale) {
		return "vitalrelics/lang/" + locale + ".json";
	}

}
