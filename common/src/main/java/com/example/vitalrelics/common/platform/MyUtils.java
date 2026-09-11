package com.example.vitalrelics.common.platform;

import com.example.vitalrelics.common.MyRuntime;
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

		victim.resetInvulnerable();

		final MyDamageSource source =
				MyRuntime.getRuntimeUtils().extraDamageSource(attacker);

		victim.setHealth(victim.health() - amount);
		victim.setHurtMark(source);
	}

	public static double distanceBetween(
			final MyEntity entity0, final MyEntity entity1) {
		if (entity0 == null || entity1 == null) {
			throw new IllegalArgumentException("null input for distanceBetween");
		}

		if (!entity0.isLoaded() || !entity1.isLoaded()) {
			throw new IllegalStateException("entities were not loaded");
		}

		double dx = entity0.x() - entity1.x();
		double dy = entity0.y() - entity1.y();
		double dz = entity0.z() - entity1.z();

		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public static boolean blockedBySuffocationZone(
			final MyLivingEntity self, final MyLivingEntity target) {

		// Passive Skill: Suffocation Zone
		final List<Relic> targetRelics = MyRuntime.getRuntimeUtils().gatherRelics(target);
		final double suffocationZoneLevel = Loader.levelOfSuchPassiveSkill(
				targetRelics, Relic.PASSIVE_SKILL_SUFFOCATION_ZONE);

		if (suffocationZoneLevel > 0.0) {
			final double distance = MyUtils.distanceBetween(self, target);
			if (distance <= suffocationZoneLevel) {
				// Prevent the event.
				// self.resetTarget();
				// MyRuntime.getRuntimeUtils().log("blockedBySuffocationZone: Prevent selecting " +
				// 		"target");
				return true;
			}
			// MyRuntime.getRuntimeUtils().log(String.format("blockedBySuffocationZone: Too " +
			// 		"faraway {} {}", suffocationZoneLevel, distance));
			return false;
		}

		// MyRuntime.getRuntimeUtils().log("blockedBySuffocationZone: No Such passive skill");
		return false;
	}
}
