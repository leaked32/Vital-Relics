package com.example.vitalrelics.common.platform;

import com.example.vitalrelics.common.Relic;

import java.util.List;

public interface MySpellPlatform {
	List<Relic> gatherRelics(MyLivingEntity caster);

	void syncSpellHud(
			MyLivingEntity caster,
			String spellId,
			int cooldownTicks
	);

	void clearSpellHud(MyLivingEntity caster);

	void showSelectedSpell(
			MyLivingEntity caster,
			String spellId
	);

	void showSpellCooldown(
			MyLivingEntity caster,
			String spellId,
			int remainingTicks
	);
}
