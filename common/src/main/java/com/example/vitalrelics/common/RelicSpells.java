package com.example.vitalrelics.common;

import com.example.vitalrelics.common.relics.Relic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RelicSpells {
	private RelicSpells() {}
	/*
	 * The default score keeps the old teleport behavior: intensity times
	 * recovery. Other spell types may supply a numeric "priority" parameter
	 * without requiring a new Java field.
	 */
	public static boolean isTheSpellBetter(
			final Relic.Spells.Info base,
			final Relic.Spells.Info candidate) {

		return score(candidate) > score(base);
	}

	private static double score(final Relic.Spells.Info spell) {
		final Object priority = spell.parameters.get("priority");

		if (priority instanceof Number value)
			return value.doubleValue();

		return numberParameter(spell, "intensity", 0.0) *
				numberParameter(spell, "recovery", 1.0);
	}

	public static double numberParameter(
			final Relic.Spells.Info spell,
			final String key,
			final double fallback) {

		final Object value = spell.parameters.get(key);

		return value instanceof Number number
				? number.doubleValue()
				: fallback;
	}

	private static final int MAX_COOLDOWN_TICKS = 20 * 60;

	/*
	 * There are potential issues if ticks are unstable on large-scale modpacks.
	 * Therefore, the ticks should be stablized.
	 */
	public static int cooldownTicks(final Relic.Spells.Info spell) {
		final double recovery =
				numberParameter(spell, "recovery", Double.NaN);

		if (!Double.isFinite(recovery) || recovery <= 0.0) {
			throw new IllegalArgumentException(
					"Spell recovery must be finite and positive: " + recovery
			);
		}

		final double calculatedTicks = 20.0 / recovery;

		return (int) Math.max(1L, Math.min(Math.round(calculatedTicks), MAX_COOLDOWN_TICKS));
	}

	public static Map<String, Relic.Spells.Info> gatherSpells(
			final List<Relic> relics) {

		final Map<String, Relic.Spells.Info> result =
				new LinkedHashMap<>();

		for (final Relic relic : relics) {
			for (final var entry : relic.available_spells.entrySet()) {
				final Relic.Spells.Info current = result.get(entry.getKey());

				if (current == null || isTheSpellBetter(current, entry.getValue()))
					result.put(entry.getKey(), entry.getValue());
			}
		}

		return result;
	}

}
