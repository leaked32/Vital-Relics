package com.example.vitalrelics.common.platform;

import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Loader;

import java.util.List;

public class MyUtils {

	public static void removeImmuneEffects(
			final MyLivingEntity entity,
			final List<Relic> relics,
			final MyLivingEntity.MyEffectCategory category) {

		for (final MyLivingEntity.MyEffectInstance effect : entity.activeEffects()) {
			if (category != MyLivingEntity.MyEffectCategory.ALL &&
					effect.category() != category) {
				continue;
			}

			final boolean negative =
					effect.category() == MyLivingEntity.MyEffectCategory.NEGATIVE;

			if (Loader.isImmuneToEffect(
					relics,
					effect.id(),
					negative
			)) {
				entity.removeEffect(effect.id());
			}
		}
	}

	public static boolean cleanseEffects(
			final MyLivingEntity entity,
			final MyLivingEntity.MyEffectCategory category) {

		boolean removed = false;

		for (final MyLivingEntity.MyEffectInstance effect : entity.activeEffects()) {
			if (category != MyLivingEntity.MyEffectCategory.ALL &&
					effect.category() != category) {
				continue;
			}

			entity.removeEffect(effect.id());
			removed = true;
		}

		return removed;
	}

	public static void applyRelicEffects(
			final MyLivingEntity entity, final List<Relic> relics) {

		for (final Relic relic : relics) {
			for (final var entry : relic.granted_effects.entrySet()) {
				final int amplifier =
						Math.max(0, entry.getValue() - 1);

				entity.addEffect(
						entry.getKey(),
						240,
						amplifier,
						true,
						false
				);
			}
		}
	}

	// MyUtils.java
	public static void trueHurt(
			final MyLivingEntity attacker, final MyLivingEntity victim, final float amount) {

		final MyDamageSource source =
				new MyDamageSource(attacker, MyDamageSource.MyDamageKind.EXTRA_DAMAGE);

		victim.setHealth(victim.health() - amount);
		victim.setHurtMark(source);
	}
}
