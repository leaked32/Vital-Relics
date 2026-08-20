package com.example.vitalrelics.common;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Acquisition {
	public final Map<String, Crafting> recipes = new LinkedHashMap<>();
	public final Map<String, List<Loot>> loot = new LinkedHashMap<>();
	public final List<String> undefined = new ArrayList<>();

	public static class Crafting {
		public String type;
		public List<String> pattern = new ArrayList<>();
		public Map<String, String> key = new LinkedHashMap<>();
		public List<String> ingredients = new ArrayList<>();
		public int count = 1;
	}

	public static class Loot {
		public String table;
		public double chance;
	}
}