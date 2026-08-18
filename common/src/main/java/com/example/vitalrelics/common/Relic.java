package com.example.vitalrelics.common;


import java.util.ArrayList;
import java.util.List;

// public static BooleanValue configHaloRealityPiercer;
public class Relic {
	public String id;
	public String curio_slot = "charm";
	public String tooltip = "Placeholder tooltip";
	public String rarity = "common";
	public String texture = "brown_ring.png";

	// parse either an array or "all_negative"
	public final List<String> immune_to_effects = new ArrayList<>();
	public final List<String> special_abilities = new ArrayList<>();


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

}