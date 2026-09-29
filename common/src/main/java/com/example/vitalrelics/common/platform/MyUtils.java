package com.example.vitalrelics.common.platform;

import com.example.vitalrelics.common.MyRuntime;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Loader;

import java.util.List;
import java.util.Objects;

public class MyUtils {


	public static void requireLoaded(final MyEntity entity) {
		Objects.requireNonNull(entity, "entity is null");

		if (!entity.isLoaded()) {
			throw new IllegalStateException("entity were not loaded");
		}
	}


	public static void removeImmuneEffects(
			final MyLivingEntity entity, final List<Relic> relics,
			final MyLivingEntity.MyEffectCategory category
	) {

		requireLoaded(entity);
		Objects.requireNonNull(relics, "relics");
		Objects.requireNonNull(category, "category");

		for (final MyLivingEntity.MyEffectInstance effect : entity.activeEffects()) {
			if (category != MyLivingEntity.MyEffectCategory.ALL && effect.category() != category) {
				continue;
			}

			final boolean negative = effect.category() == MyLivingEntity.MyEffectCategory.NEGATIVE;

			if (Loader.isImmuneToEffect(relics, effect.id(), negative)) {
				entity.removeEffect(effect.id());
			}
		}
	}

	public static boolean cleanseEffects(
			final MyLivingEntity entity,
			final MyLivingEntity.MyEffectCategory category
	) {

		requireLoaded(entity);
		Objects.requireNonNull(category, "category");

		boolean removed = false;

		for (final MyLivingEntity.MyEffectInstance effect : entity.activeEffects()) {
			if (category != MyLivingEntity.MyEffectCategory.ALL && effect.category() != category) {
				continue;
			}

			entity.removeEffect(effect.id());
			removed = true;
		}

		return removed;
	}

	public static void applyRelicEffects(final MyLivingEntity entity, final List<Relic> relics) {

		Objects.requireNonNull(entity, "entity");
		Objects.requireNonNull(relics, "relics");

		for (final Relic relic : relics) {
			for (final var entry : relic.granted_effects.entrySet()) {
				final int amplifier = Math.max(0, entry.getValue() - 1);

				entity.addEffect(entry.getKey(), 240, amplifier, true, false);
			}
		}
	}

	// MyUtils.java
	public static void trueHurt(
			final MyLivingEntity attacker, final MyLivingEntity victim, final float amount) {

		Objects.requireNonNull(attacker, "attacker");
		Objects.requireNonNull(victim, "victim");

		victim.resetInvulnerable();

		final MyDamageSource source = MyRuntime.getRuntimeUtils().extraDamageSource(attacker);

		victim.setHealth(victim.health() - amount);
		victim.setHurtMark(source);
	}

	public static double distanceBetween(final MyEntity entity0, final MyEntity entity1) {

		Objects.requireNonNull(entity0, "entity0");
		Objects.requireNonNull(entity1, "entity1");

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

		requireLoaded(self);
		requireLoaded(target);

		// Passive Skill: Suffocation Zone
		final List<Relic> targetRelics = MyRuntime.getRuntimeUtils().gatherRelics(target);
		final double suffocationZoneLevel = Loader.levelOfSuchPassiveSkill(
				targetRelics, Relic.PASSIVE_SKILL_SUFFOCATION_ZONE);

		if (suffocationZoneLevel > 0.0) {
			final double distance = MyUtils.distanceBetween(self, target);
			if (distance <= suffocationZoneLevel) {
				return true;
			}
			return false;
		}
		return false;
	}
}
