package com.example.vitalrelics.common.platform;

import com.example.vitalrelics.common.Relic;

import java.util.List;

public interface MySpellRuntime {
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

	void showCurseRequiresTarget(MyLivingEntity caster);

	boolean teleportAlongLook(MyLivingEntity caster, double range);

	MyLivingEntity pointedLivingEntity(
			MyLivingEntity caster,
			double range
	);

	boolean cleanseHarmfulEffects(MyLivingEntity caster);

	boolean shadowExchange(
			MyLivingEntity caster,
			double range
	);

	boolean phantomStep(
			MyLivingEntity caster,
			double range,
			float intensity
	);

	boolean upgradeEnchantedBook(
			MyLivingEntity caster,
			int experienceCost
	);
}
