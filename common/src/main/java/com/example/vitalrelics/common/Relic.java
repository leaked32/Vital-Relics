package com.example.vitalrelics.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Relic {
	public String id;
	public String display_name = null;
	public final List<String> effective_slots = new ArrayList<>();
	public String curio_slot = "charm";
	public String tooltip = "Placeholder tooltip";
	public String rarity = "common";
	public String texture = "brown_ring.png";

	public final List<String> immune_to_effects = new ArrayList<>();
	public final Map<String, Integer> granted_effects = new LinkedHashMap<>();
	public final Map<String, Integer> passive_abilities = new LinkedHashMap<>();

	/*
	 * Each map is intentionally open-ended. Adding a new configuration entry
	 * no longer requires adding a field to this class or a parser branch.
	 */
	public final Map<String, Properties.Info> properties = new LinkedHashMap<>();
	public final Map<String, Ticks.Info> ticks = new LinkedHashMap<>();
	public final Map<String, Callbacks.Info> callbacks = new LinkedHashMap<>();
	public final Map<String, Spells.Info> available_spells = new LinkedHashMap<>();

	public static class Properties {
		public static class Info {
			public Double add = null;
			public Double mul_base = null;
			public Double mul_total = null;

			public static Info basic() {
				final Info result = new Info();
				result.add = 0.0;
				result.mul_base = 0.0;
				result.mul_total = 1.0;
				return result;
			}
		}
	}

	public static class Ticks {
		public static class Info {
			public int interval_ticks = 1;
			public Double add = null;
			public Double ratio_add = null;

			public static Info basic() {
				final Info result = new Info();
				result.interval_ticks = 1;
				result.add = 0.0;
				result.ratio_add = 0.0;
				return result;
			}
		}
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

				return sigmoidClamp(result, lowerBound(reference), upperBound(reference));
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

				if (Double.isInfinite(minimum))
					return maximum - Math.log1p(Math.exp(maximum - value));

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
	}

	public static class Spells {
		public static class Info {
			public final Map<String, Object> parameters = new LinkedHashMap<>();
		}
	}

	public boolean isImmuneToEffect(
			final String effectId,
			final boolean negative) {

		return immune_to_effects.contains(effectId) ||
				(negative && immune_to_effects.contains("all_negative"));
	}

	public List<String> getTooltipLines() {
		final List<String> out = new ArrayList<>();

		if (tooltip != null && !tooltip.isBlank()) {
			out.add(tooltip);
			out.add("");
		}

		for (final var entry : properties.entrySet())
			addProperty(out, displayName(entry.getKey()), entry.getValue());

		for (final var entry : ticks.entrySet())
			addTick(out, displayName(entry.getKey()), entry.getValue());

		for (final var entry : callbacks.entrySet())
			addCallback(out, displayName(entry.getKey()), entry.getValue());

		if (immune_to_effects.contains("all_negative"))
			out.add("Immune to all negative effects");
		else
			for (final String effect : immune_to_effects)
				out.add("Immune to " + displayName(effect));

		for (final var effect : granted_effects.entrySet())
			out.add("Grants " + displayName(effect.getKey()) + " " + effect.getValue());

		for (final var ability : passive_abilities.entrySet())
			out.add("Ability: " + displayName(ability.getKey()) + " " + ability.getValue());

		for (final String spellId : available_spells.keySet())
			out.add("Spell: " + displayName(spellId));

		while (!out.isEmpty() && out.get(out.size() - 1).isEmpty())
			out.remove(out.size() - 1);

		return out;
	}

	private static void addProperty(
			final List<String> out,
			final String name,
			final Properties.Info value) {

		if (nz(value.add))
			out.add(signed(value.add) + " " + name);
		if (nz(value.mul_base))
			out.add(signedPercent(value.mul_base) + " " + name + " from base");
		if (value.mul_total != null && value.mul_total != 1.0)
			out.add("x" + fmt(value.mul_total) + " total " + name);
	}

	private static void addTick(
			final List<String> out,
			final String name,
			final Ticks.Info value) {

		final String prefix = "Every " + fmt(value.interval_ticks / 20.0) + "s: ";

		if (nz(value.add))
			out.add(prefix + signed(value.add) + " " + name);
		if (nz(value.ratio_add))
			out.add(prefix + signedPercent(value.ratio_add) + " Max " + name);
	}

	private static void addCallback(
			final List<String> out,
			final String name,
			final Callbacks.Info value) {

		if (value.modifier != null)
			out.add(signedPercentRaw((value.modifier - 1.0) * 100.0) + " " + name);
		if (nz(value.flat))
			out.add(signed(value.flat) + " " + name);
		if (value.minimum != null)
			out.add("Minimum " + name + ": " + fmt(value.minimum));
		if (value.ratio_minimum != null)
			out.add("Minimum " + name + ": " +
					fmt(value.ratio_minimum * 100.0) + "% of reference");
		if (value.maximum != null)
			out.add("Maximum " + name + ": " + fmt(value.maximum));
		if (value.ratio_maximum != null)
			out.add("Maximum " + name + ": " +
					fmt(value.ratio_maximum * 100.0) + "% of reference");
	}

	private static boolean nz(final Double value) {
		return value != null && value != 0.0;
	}

	private static String signed(final double value) {
		return (value >= 0.0 ? "+" : "") + fmt(value);
	}

	private static String signedPercent(final double value) {
		return signedPercentRaw(value * 100.0);
	}

	private static String signedPercentRaw(final double value) {
		return (value >= 0.0 ? "+" : "") + fmt(value) + "%";
	}

	private static String fmt(final double value) {
		if (Math.abs(value - Math.round(value)) < 0.000001)
			return Long.toString(Math.round(value));

		return String.format("%.2f", value)
				.replaceAll("0+$", "")
				.replaceAll("\\.$", "");
	}

	private static String displayName(final String id) {
		final StringBuilder out = new StringBuilder();

		for (final String word : id.split("_")) {
			if (word.isEmpty())
				continue;

			if (!out.isEmpty())
				out.append(' ');

			out.append(Character.toUpperCase(word.charAt(0)))
					.append(word.substring(1));
		}

		return out.toString();
	}

	public static String itemDisplayName(final String id) {
		return displayName(id);
	}
}
