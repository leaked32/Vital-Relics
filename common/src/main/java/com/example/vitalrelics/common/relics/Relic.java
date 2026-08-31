package com.example.vitalrelics.common.relics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.vitalrelics.common.RelicText.displayName;

public class Relic {
	/*
	 * Do not remove my comments.
	 *
	 * Design Strength Table
	 *                  Common   Uncommon   Rare   Epic
	 * Ring              1.0       1.5      2.2     3.2
	 * Bracelet          1.2       1.8      2.6     3.7
	 * Charm/Pendant     1.3       2.0      3.0     8.5
	 * Necklace          1.5       2.3      3.4     10.0
	 * Head              1.5       2.3      3.4     10.0
	 *
	 * So, we'll add new relics for necklace and head later right?
	 *   it shouldn't be halos for head relics if they are not epic.
	 *
	 */

	public String id;
	public String display_name = null;
	public final List<String> effective_slots = new ArrayList<>();
	public String curio_slot = "charm";
	public String tooltip = "Placeholder tooltip";
	public String rarity = "common";
	public String texture = "brown_ring.png";

	public final List<String> immune_to_effects = new ArrayList<>();
	public final Map<String, Integer> granted_effects = new LinkedHashMap<>();

	/*
	Passive Skills
	 */
	public static final String PASSIVE_SKILL_RETARGET_ARROW = "retarget_arrow";
	public static final String PASSIVE_SKILL_REALITY_SEVERANCE = "reality_severance";
	public static final String PASSIVE_SKILL_METAL_MENDING = "metal_mending";
	public static final String PASSIVE_SKILL_FLIGHT = "flight";
	public static final String PASSIVE_SKILL_EMPOWERED_ARROW = "empowered_arrows";
	public static final String PASSIVE_SKILL_LIFESTEAL = "lifesteal";
	public static final String PASSIVE_SKILL_THORNS = "thorns";
	public static final String PASSIVE_SKILL_ARROW_DEFLECTION = "arrow_deflection";
	public static final String PASSIVE_SKILL_FIRE_RESISTANCE = "fire_resistance";
	public static final String PASSIVE_SKILL_LAVA_SWIMMER = "lava_swimmer";
	public static final String PASSIVE_SKILL_IRON_CURTAIN = "iron_curtain";
	public static final String PASSIVE_SKILL_LINGERING_WOUND = "lingering_wound";

	public final Map<String, Double> passive_skills = new LinkedHashMap<>();

	/*
	Spells
	 */
	public static final String SPELL_TELEPORT = "teleport";
	public static final String SPELL_CURSE = "curse";
	public static final String SPELL_HEAL = "heal";
	public static final String SPELL_HEALING_RAY = "healing_ray";
	public static final String SPELL_CLEANSE = "cleanse";
	public static final String SPELL_DASH = "dash";
	public static final String SPELL_ARC_BURST = "arc_burst";
	public static final String SPELL_REPULSE = "repulse";
	public static final String SPELL_ABSORPTION = "absorption";
	public static final String SPELL_SKY_LAUNCH = "sky_launch";
	public static final String SPELL_SHADOW_EXCHANGE = "shadow_exchange";
	public static final String SPELL_PHANTOM_STEP = "phantom_step";
	public static final String SPELL_UPGRADE_ENCHANTED_BOOK = "upgrade_enchanted_book";
	public static final String SPELL_ENCHANTMENT_ASCENSION = "enchantment_ascension";
	public static final String SPELL_PURIFY_CURSE = "purify_curse";
	public static final String SPELL_PURIFY_PENALTY = "purify_penalty";
	public static final String SPELL_DISENCHANTMENT = "disenchantment";

	public final Map<String, Spells.Info> available_spells = new LinkedHashMap<>();


	public final Map<String, Properties.Info> properties = new LinkedHashMap<>();
	public final Map<String, Ticks.Info> ticks = new LinkedHashMap<>();
	public final Map<String, Callbacks.Info> callbacks = new LinkedHashMap<>();
	public final Map<String, Double> enemy_spawn = new LinkedHashMap<>();

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

	public static String itemDisplayName(final String id) {
		return displayName(id);
	}
}
