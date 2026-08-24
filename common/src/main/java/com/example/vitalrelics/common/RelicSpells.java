package com.example.vitalrelics.common;

import com.example.vitalrelics.common.scheduled.MyMap;
import com.example.vitalrelics.common.scheduled.Scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RelicSpells {


	public static MyMap<Relic.Spells> LIVING_ENTITY_SPELLS = new MyMap<>(0);


	public static boolean isTheSpellBetter(final Relic.Spells.Info base, final Relic.Spells.Info cmp) {
		if (cmp.intensity * cmp.recovery > base.intensity * cmp.recovery) {
			return true;
		}
		return false;
	}

	public static Relic.Spells gatherSpells(final List<Relic> relics) {
		Relic.Spells basic = new Relic.Spells();
		basic.teleport = Relic.Spells.Info.basic();

		for (final Relic relic : relics) {
			if (isTheSpellBetter(basic.teleport, relic.available_spells.teleport)) {
				basic.teleport = relic.available_spells.teleport;
			}
		}

		return basic;
	}

	public static class CurrentState {
		public Relic.Spells spells;
	}

	public static void UpdateForLivingEntity(final UUID uuid, final List<Relic> relics) {
		final var new_spells = gatherSpells(relics);
		final var old_spells = LIVING_ENTITY_SPELLS.get(uuid);


		LIVING_ENTITY_SPELLS.put(uuid, 0, new_spells);
	}



}
