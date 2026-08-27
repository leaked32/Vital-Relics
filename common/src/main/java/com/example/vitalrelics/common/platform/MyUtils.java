package com.example.vitalrelics.common.platform;

import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;

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

			if (RelicLoader.isImmuneToEffect(
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

}
