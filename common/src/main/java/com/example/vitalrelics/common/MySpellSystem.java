package com.example.vitalrelics.common;

import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MySpellPlatform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MySpellSystem {
	@FunctionalInterface
	public interface Handler {
		boolean activate(MyLivingEntity livingEntity, Relic.Spells.Info spell);
	}

	public static final String CAST_SPELL = "cast_spell";
	public static final String SWITCH_SPELL_NEXT = "switch_spell_next";
	public static final String SWITCH_SPELL_PREVIOUS = "switch_spell_previous";

	public static final MySpellSystem INSTANCE = new MySpellSystem();

	private final Map<String, Handler> handlers = new HashMap<>();

	private MySpellSystem() {}

	public void register(final String abilityId, final Handler handler) {
		if (handlers.putIfAbsent(abilityId, handler) != null) {
			throw new IllegalStateException(
					"Spell already registered: " + abilityId
			);
		}
	}

	public void syncSpellHud(
			final MyLivingEntity caster,
			final MySpellPlatform platform) {

		final int tick = caster.serverTick();
		if (tick < 0)
			return;

		final Map<String, Relic.Spells.Info> spells =
				RelicSpells.gatherSpells(platform.gatherRelics(caster));

		final List<String> spellIds = new ArrayList<>(spells.keySet());

		final String selected =
				Scheduler.INSTANCE().selectedSpell(
						caster.uuid(),
						spellIds,
						tick
				);

		if (selected == null) {
			platform.clearSpellHud(caster);
			return;
		}

		syncSpellHud(caster, selected, tick, platform);
	}

	private void syncSpellHud(
			final MyLivingEntity caster,
			final String spellId,
			final int tick,
			final MySpellPlatform platform) {

		final int cooldownTicks =
				Scheduler.INSTANCE().getSpellCooldownRemaining(
						caster.uuid(), spellId, tick
				);

		platform.syncSpellHud(caster, spellId, cooldownTicks);
	}

	public void activate(
			final MyLivingEntity caster,
			String abilityId,
			final MySpellPlatform platform) {

		if (caster.isClientSide() ||
				!abilityId.matches("[a-z0-9_./-]{1,64}")) {
			return;
		}

		final int tick = caster.serverTick();
		if (tick < 0)
			return;

		final Map<String, Relic.Spells.Info> spells =
				RelicSpells.gatherSpells(platform.gatherRelics(caster));

		final List<String> spellIds = new ArrayList<>(spells.keySet());

		if (abilityId.equals(SWITCH_SPELL_NEXT) ||
				abilityId.equals(SWITCH_SPELL_PREVIOUS)) {

			final int direction =
					abilityId.equals(SWITCH_SPELL_NEXT) ? 1 : -1;

			final String selected = Scheduler.INSTANCE().selectSpell(
					caster.uuid(),
					spellIds,
					direction,
					tick
			);

			if (selected != null) {
				syncSpellHud(caster, selected, tick, platform);
				platform.showSelectedSpell(caster, selected);
			}
			return;
		}

		if (!abilityId.equals(CAST_SPELL))
			return;

		abilityId = Scheduler.INSTANCE().selectedSpell(
				caster.uuid(),
				spellIds,
				tick
		);

		if (abilityId == null)
			return;

		final Relic.Spells.Info spell = spells.get(abilityId);

		if (spell == null)
			return;

		final int remainingTicks =
				Scheduler.INSTANCE().getSpellCooldownRemaining(
						caster.uuid(), abilityId, tick
				);

		if (remainingTicks > 0) {
			platform.showSpellCooldown(
					caster,
					abilityId,
					remainingTicks
			);
			return;
		}

		final Handler handler = handlers.get(abilityId);

		if (handler != null && handler.activate(caster, spell)) {
			Scheduler.INSTANCE().setSpellCooldown(
					caster.uuid(),
					abilityId,
					tick,
					RelicSpells.cooldownTicks(spell)
			);

			syncSpellHud(caster, abilityId, tick, platform);
		}
	}
}
