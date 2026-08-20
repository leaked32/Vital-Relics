package com.example.vitalrelics.common;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// public static BooleanValue configHaloRealityPiercer;
public class Relic {
	public String id;
	// for empty case, it's effective everywhere.
	// "in_hotbar",
	// "in_inventory": In inventory (including hotbar),
	// "in_curios_api_slots"
	// "in_touhou_little_maid_curios_slots"
	public List<String> effective_slots = new ArrayList<>();
	public String curio_slot = "charm";
	public String tooltip = "Placeholder tooltip";
	public String rarity = "common";
	public String texture = "brown_ring.png";

	// parse either an array or "all_negative"
	public final List<String> immune_to_effects = new ArrayList<>();
	public final Map<String, Integer> add_effects = new LinkedHashMap<>();
	public final Map<String, Integer> special_abilities = new LinkedHashMap<>();


	public Properties properties = new Properties();
	public Ticks ticks = new Ticks();
	public Callbacks callbacks = new Callbacks();

	public static class Properties {
		public static class Info {

			public Double add = null;
			public Double mul_base = null;
			public Double mul_total = null;


			public static Info basic() {

				Info prop = new Info();
				prop.add = 0.0;
				prop.mul_base = 0.0;
				prop.mul_total = 1.0;

				return prop;
			}
		}

		public Info attack_damage = null;
		public Info attack_speed = null;
		public Info block_interaction_range = null;
		public Info entity_interaction_range = null;
		public Info knockback_resistance = null;
		public Info max_health = null;

	}

	public static class Ticks {
		public static class Info {
			public int interval_ticks = 1;
			public Double add = null;
			public Double ratio_add = null;

			public static Info basic() {
				Info prop = new Info();
				prop.interval_ticks = 1; // Run on every ticks
				prop.add = 0.0;
				prop.ratio_add = 0.0;

				return prop;
			}
		}


		public Info heal = null;
		public Info feed = null;
	}

	public static class Callbacks {

		public static class Info {
			public Double modifier = null;
			public Double flat = null;
			public Double minimum = null;
			public Double ratio_minimum = null;
			public Double maximum = null;
			public Double ratio_maximum = null;


			public double process(final double amount, final double reference) {
				double result = amount;

				if (modifier != null)
					result *= modifier;
				if (flat != null)
					result += flat;

				final double lower = lowerBound(reference);
				final double upper = upperBound(reference);

				return sigmoidClamp(result, lower, upper);
			}

			private double lowerBound(final double reference) {
				double lower = Double.NEGATIVE_INFINITY;

				if (minimum != null)
					lower = Math.max(lower, minimum);

				if (ratio_minimum != null)
					lower = Math.max(lower, reference * ratio_minimum);

				return lower;
			}

			private double upperBound(final double reference) {
				double upper = Double.POSITIVE_INFINITY;

				if (maximum != null)
					upper = Math.min(upper, maximum);

				if (ratio_maximum != null)
					upper = Math.min(upper, reference * ratio_maximum);

				return upper;
			}

			private static double sigmoidClamp(
					final double value,
					final double minimum,
					final double maximum) {

				if (Double.isInfinite(minimum) && Double.isInfinite(maximum))
					return value;

				// Only an upper bound.
				if (Double.isInfinite(minimum))
					return maximum - Math.log1p(Math.exp(maximum - value));

				// Only a lower bound.
				if (Double.isInfinite(maximum))
					return minimum + Math.log1p(Math.exp(value - minimum));

				if (minimum >= maximum)
					return minimum;

				final double center = (minimum + maximum) * 0.5;
				final double range = maximum - minimum;

				return minimum + range /
						(1.0 + Math.exp(-4.0 * (value - center) / range));
			}
		}
		public Info damage_taken = null;
		public Info damage_dealt = null;
		public Info invulnerable_time_taken = null;
		public Info invulnerable_time_dealt = null;
	}

	public boolean isImmuneToEffect(
			final String effectId,
			final boolean negative) {

		if (immune_to_effects.contains(effectId))
			return true;

		return negative && immune_to_effects.contains("all_negative");
	}


	/*
	 * Tooltip
	 */
	public List<String> getTooltipLines() {
		final List<String> out = new ArrayList<>();

		if (tooltip != null && !tooltip.isBlank()) {
			out.add(tooltip);
			out.add("");
		}

		addProperty(out, "Attack Damage", properties.attack_damage);
		addProperty(out, "Attack Speed", properties.attack_speed);
		addProperty(out, "Block Interaction Range", properties.block_interaction_range);
		addProperty(out, "Entity Interaction Range", properties.entity_interaction_range);
		addProperty(out, "Knockback Resistance", properties.knockback_resistance);
		addProperty(out, "Max Health", properties.max_health);

		addTick(out, "Health", ticks.heal);
		addTick(out, "Hunger", ticks.feed);

		addCallback(out, "Damage Taken", callbacks.damage_taken);
		addCallback(out, "Damage Dealt", callbacks.damage_dealt);
		addCallback(out, "Invulnerability Time Taken", callbacks.invulnerable_time_taken);
		addCallback(out, "Invulnerability Time Dealt", callbacks.invulnerable_time_dealt);

		if (immune_to_effects.contains("all_negative"))
			out.add("Immune to all negative effects");
		else
			for (final String x : immune_to_effects)
				out.add("Immune to " + displayName(x));

		for (final String x : add_effects.keySet())
			out.add("Grants " + displayName(x));

		for (final String x : special_abilities.keySet())
			out.add("Ability: " + displayName(x));

		while (!out.isEmpty() && out.get(out.size() - 1).isEmpty())
			out.remove(out.size() - 1);

		return out;
	}

	private static void addProperty(
			final List<String> out, final String name, final Properties.Info x) {
		if (x == null) return;
		if (nz(x.add)) out.add(signed(x.add) + " " + name);
		if (nz(x.mul_base)) out.add(signedPercent(x.mul_base) + " " + name + " from base");
		if (x.mul_total != null && x.mul_total != 1.0)
			out.add("x" + fmt(x.mul_total) + " total " + name);
	}

	private static void addTick(
			final List<String> out, final String name, final Ticks.Info x) {
		if (x == null) return;

		final String prefix = "Every " + fmt(x.interval_ticks / 20.0) + "s: ";
		if (nz(x.add)) out.add(prefix + signed(x.add) + " " + name);
		if (nz(x.ratio_add))
			out.add(prefix + signedPercent(x.ratio_add) + " Max " + name);
	}

	private static void addCallback(
			final List<String> out, final String name, final Callbacks.Info x) {
		if (x == null) return;

		if (x.modifier != null)
			out.add(signedPercentRaw((x.modifier - 1.0) * 100.0) + " " + name);
		if (nz(x.flat)) out.add(signed(x.flat) + " " + name);
		if (x.minimum != null) out.add("Minimum " + name + ": " + fmt(x.minimum));
		if (x.ratio_minimum != null)
			out.add("Minimum " + name + ": " + fmt(x.ratio_minimum * 100.0) + "% of reference");
		if (x.maximum != null) out.add("Maximum " + name + ": " + fmt(x.maximum));
		if (x.ratio_maximum != null)
			out.add("Maximum " + name + ": " + fmt(x.ratio_maximum * 100.0) + "% of reference");
	}

	private static boolean nz(final Double x) {
		return x != null && x != 0.0;
	}

	private static String signed(final double x) {
		return (x >= 0.0 ? "+" : "") + fmt(x);
	}

	private static String signedPercent(final double x) {
		return signedPercentRaw(x * 100.0);
	}

	private static String signedPercentRaw(final double x) {
		return (x >= 0.0 ? "+" : "") + fmt(x) + "%";
	}

	private static String fmt(final double x) {
		if (Math.abs(x - Math.round(x)) < 0.000001)
			return Long.toString(Math.round(x));

		return String.format("%.2f", x)
				.replaceAll("0+$", "")
				.replaceAll("\\.$", "");
	}

	private static String displayName(final String x) {
		final StringBuilder out = new StringBuilder();

		for (final String word : x.split("_")) {
			if (word.isEmpty()) continue;
			if (!out.isEmpty()) out.append(' ');
			out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}

		return out.toString();
	}

}