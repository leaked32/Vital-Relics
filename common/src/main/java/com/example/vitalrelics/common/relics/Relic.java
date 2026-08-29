package com.example.vitalrelics.common.relics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.vitalrelics.common.RelicText.displayName;

public class Relic {
	/*
	Do not remove my comments.
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
	 * Passive skills
	 * - Passive skills activates automatically on condition.
	 * - Passive skill level cannot be stacked or summed, only the highest level counts.
	 *
	 * Available Passive skills:
	 * - `retarget_arrow`: Reflected arrow minimum damage = ATTACK_DAMAGE × level
	 * - `arrow_deflection`: Reflects one incoming arrow; reflected damage and speed are
	 *      multiplied by level, and cooldown is 5 / level seconds
	 * - `reality_severance`: level% attack-damage contribution, level-block radius,
	 *      roughly level/4 debuff strength
	 * - `metal_mending`: Repairs up to level durability every 4 seconds
	 * - `flight`: Any level > 0 grants flight; flight speed = vanilla flight speed × level,
	 *     does not change the speed if the level is 1.0
	 * - `empowered_arrows`: Multiplies arrow charge, velocity, and base damage by level
	 * - `lifesteal`: Heals the bearer for damage dealt × level
	 * - `thorns`: Reflects received damage × level; reflection is limited by a cooldown
	 * - `fire_resistance`: Extinguish fire.
	 * - `iron_curtain`: super invulnerable time
	 */
	public static final String PASSIVE_SKILL_RETARGET_ARROW = "retarget_arrow";
	public static final String PASSIVE_SKILL_REALITY_SEVERANCE = "reality_severance";
	public static final String PASSIVE_SKILL_METAL_MENDING = "metal_mending";
	public static final String PASSIVE_SKILL_FLIGHT = "flight";
	public static final String PASSIVE_SKILL_EMPOWERED_ARROW = "empowered_arrows";
	public static final String PASSIVE_SKILL_LIFESTEAL = "lifesteal";
	public static final String PASSIVE_SKILL_THORNS = "thorns";
	public static final String PASSIVE_SKILL_ARROW_DEFLECTION = "arrow_deflection";
	// public static final String PASSIVE_SKILL_FIRE_RESISTANCE = "fire_resistance";
	public static final String PASSIVE_SKILL_IRON_CURTAIN = "iron_curtain";

	public final Map<String, Double> passive_skills = new LinkedHashMap<>();

	/*
	 * Each map is intentionally open-ended. Adding a new configuration entry
	 * no longer requires adding a field to this class or a parser branch.
	 */
	public final Map<String, Properties.Info> properties = new LinkedHashMap<>();
	public final Map<String, Ticks.Info> ticks = new LinkedHashMap<>();

	/*
	 * - `damage_dealt`
	 * - `damage_taken`
	 * - `invulnerable_time_taken`
	 * - `invulnerable_time_dealt`
	 */
	public final Map<String, Callbacks.Info> callbacks = new LinkedHashMap<>();

	public final Map<String, Double> enemy_spawn = new LinkedHashMap<>();

	/*
	 * Available Spells:
	 * - `teleport`:
	 *     BLOCK hit: center for thin blocks; try above, if blocked, try before the hit face
	 *     MISS / sky -> teleport as far along look direction as possible
	 *
	 * - `curse`: Calls `directAttack` with the pointed living entity
	 * - `heal`: Restores `amount` health plus `ratio` of the caster's maximum health
	 * - `cleanse`: Removes all negative effects from the caster
	 * - `dash`: Launches the caster forward by `strength`, with optional `vertical` velocity
	 * - `arc_burst`: Repeatedly damages hostile targets within `range`;
	 *      each hit deals `intensity`% attack damage, repeated `count` times,
	 *      with optional `weaken` debuff strength
	 * - `repulse`: Pushes hostile living entities within `range` away from the caster
	 *      using `strength`, with optional `vertical` lift
	 * - `absorption`: Grants Absorption for `duration_ticks` with the configured `amplifier`
	 * - `sky_launch`: Launches hostile living entities within `range` upward by `strength`
	 * - `shadow_exchange`: Swaps positions with the pointed hostile living entity within `range`
	 * - `phantom_step`: Instantly moves forward up to `range` blocks and damages hostile
	 *      living entities crossed for `intensity`% attack damage
	 * - `upgrade_enchanted_book`: Upgrades the first non-max-level enchantment
	 *      on the enchanted book held in the main hand by one level,
	 *      consuming experience_cost experience levels. Creative players do not pay the cost.
	 * - `enchantment_ascension`: Upgrades the first non-max-level enchantment
	 *      on any enchanted item held in the main hand by one level,
	 *      consuming experience_cost experience levels. Creative players do not pay the cost.
	 * - `purify_penalty`:Removes the first curse from the item held in the main hand;
	 *      if no curse exists, resets its anvil repair-cost penalty to zero instead.
	 *      Consumes experience_cost experience levels; creative players do not pay the cost.
	 */
	public static final String SPELL_TELEPORT = "teleport";
	public static final String SPELL_CURSE = "curse";
	public static final String SPELL_HEAL = "heal";
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
	public static final String SPELL_PURIFY_PENALTY = "purify_penalty";

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

	public static String itemDisplayName(final String id) {
		return displayName(id);
	}
}
