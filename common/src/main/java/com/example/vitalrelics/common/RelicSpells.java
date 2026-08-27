package com.example.vitalrelics.common;

import com.example.vitalrelics.common.utils.MyMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RelicSpells {
	private RelicSpells() {}

	public static final MyMap<Map<String, Relic.Spells.Info>>
			LIVING_ENTITY_SPELLS = new MyMap<>(0);

	/*
	 * The default score keeps the old teleport behaviour: intensity times
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

	public static int cooldownTicks(final Relic.Spells.Info spell) {
		final double recovery = numberParameter(spell, "recovery", 0.0);

		if (recovery <= 0.0)
			return Integer.MAX_VALUE;

		return Math.max(1, Math.round(20.0F / (float) recovery));
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

	public static class CurrentState {
		public Map<String, Relic.Spells.Info> spells = Map.of();
	}

	public static void updateForLivingEntity(
			final UUID uuid,
			final List<Relic> relics) {

		LIVING_ENTITY_SPELLS.put(uuid, 0, gatherSpells(relics));
	}
}
