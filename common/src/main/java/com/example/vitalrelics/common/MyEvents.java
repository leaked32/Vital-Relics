package com.example.vitalrelics.common;

import com.example.vitalrelics.common.platform.*;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.common.relics.Relic;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MyEvents {
	public static int onExperienceGain(
			final MyLivingEntity player, final int amount,
			final int experienceNeededForNextLevel
	) {
		if (amount <= 0 || experienceNeededForNextLevel <= 0)
			return amount;

		final double level = Loader.levelOfSuchPassiveSkill(
				MyRuntime.getRuntimeUtils().gatherRelics(player),
				Relic.PASSIVE_SKILL_EXPERIENCE_CONVERGENCE
		);
		if (level <= 0.0)
			return amount;

		final double multiplier = 1.0 + level * (experienceNeededForNextLevel / 7.0 - 1.0);
		return (int) Math.min(Integer.MAX_VALUE, Math.max(amount, Math.round(amount * multiplier)));
	}

	private MyEvents() {
	}

	public static void onLivingEntityTick(
			MyLivingEntity myLivingEntity, final int currentTick, List<Relic> relics) {
		spawnEnemyRelicParticles(myLivingEntity, relics, currentTick);

		final Map<String, Relic.Ticks.Info> ticks = Loader.computeTicks(relics, currentTick);
		final double healingAuraLevel = Loader.levelOfSuchPassiveSkill(
				relics, Relic.PASSIVE_SKILL_HEALING_AURA);

		for (final var entry : ticks.entrySet()) {
			final Relic.Ticks.Info value = entry.getValue();

			switch (entry.getKey()) {
				case "heal" -> {
					myLivingEntity.heal((float) (
							value.add + myLivingEntity.maxHealth() * value.ratio_add));

					if (healingAuraLevel > 0.0) {
						final double range = healingAuraLevel;
						for (final MyLivingEntity target : myLivingEntity.livingEntitiesInRange(range)) {
							if (myLivingEntity.isAllied(target)) {
								target.heal((float) (
										value.add + target.maxHealth() * value.ratio_add));
							}
						}
					}
				}
				case "feed" -> {
					final float amount = (float) (
							value.add + myLivingEntity.maxHealth() * value.ratio_add);
					myLivingEntity.feed(Math.round(amount), 1.0F);
				}
			}
		}

		// Scheduled to update on each half seconds
		if (currentTick % 10 == 0) {
			// Effects
			MyUtils.removeImmuneEffects(myLivingEntity, relics, MyLivingEntity.MyEffectCategory.ALL);
			MyUtils.applyRelicEffects(myLivingEntity, relics);

			// Passive Skill: Reality Severance
			final double reality_severance_level =
					Loader.levelOfSuchPassiveSkill(relics, Relic.PASSIVE_SKILL_REALITY_SEVERANCE);
			if (reality_severance_level > 0.0) {
				final float ratioDamage = (float) (reality_severance_level / 100.0);
				final float rangeDamage = ratioDamage * myLivingEntity.attackDamage();

				MyExtraDamageInfo.directRangedAttack(
						myLivingEntity, rangeDamage, Math.round((float) reality_severance_level), 1,
						Math.round((float) (reality_severance_level / 4.0))
				);
			}

			// Passive Skill: Fire Resistance
			final double fireResistanceLevel = Loader.levelOfSuchPassiveSkill(
					relics, Relic.PASSIVE_SKILL_FIRE_RESISTANCE);

			if (fireResistanceLevel > 0.0 && myLivingEntity.isOnFire()) {
				myLivingEntity.clearFire();
			}

			// Passive Skill: Grave Dominion
			final double graveDominionLevel = Loader.levelOfSuchPassiveSkill(
					relics, Relic.PASSIVE_SKILL_GRAVE_DOMINION);

			if (graveDominionLevel > 0.0) {
				for (final MyEntity entity : myLivingEntity.entitiesInRange(graveDominionLevel)) {
					entity.moveTo(entity.x(), entity.y() - entity.height(), entity.z());
				}
			}
		}

		// Scheduled to update on each second
		if (currentTick % 20 == 0) {
			// Client HUD
			if (myLivingEntity.isServerPlayer()) {
				MySpellSystem.INSTANCE.syncSpellHud(myLivingEntity);
			}
		}

		// Scheduled to update on each 4 seconds
		if (currentTick % 80 == 0) {
			final double metalMendingLevel = Loader.levelOfSuchPassiveSkill(
					relics, Relic.PASSIVE_SKILL_METAL_MENDING);

			if (metalMendingLevel > 0.0) {
				myLivingEntity.mendEquipment(Math.max(1, (int) Math.round(metalMendingLevel)));
			}
		}
	}

	public static void spawnEnemyRelicParticles(
			final MyLivingEntity entity, final List<Relic> relics, final int currentTick) {
		final MyRuntimeUtils runtime = MyRuntime.getRuntimeUtils();
		if (!runtime.hasEnemyRelics(entity) || relics.isEmpty() || currentTick % 2 != 0)
			return;

		double score = 0.0;
		for (final Relic relic : relics) {
			score += switch (relic.rarity) {
				case "common" -> 0.10;
				case "uncommon" -> 0.20;
				case "rare" -> 0.40;
				case "epic" -> 1.00;
				default -> 0.10;
			};
		}

		final int particleCount = 8;
		final double radius = Math.max(0.6, entity.width() * 0.75);
		final double y = entity.y() + entity.height() + 0.35;
		final double rotation = currentTick * 0.12;

		for (int i = 0; i < particleCount; ++i) {
			final double angle = rotation + Math.PI * 2.0 * i / particleCount;
			final double x = entity.x() + Math.cos(angle) * radius;
			final double z = entity.z() + Math.sin(angle) * radius;
			final int color;

			if (score >= 1.0) {
				color = (i & 1) == 0
						? rgb(0.45F, 0.01F, 0.01F)
						: rgb(0.10F, 0.005F, 0.005F);
			} else {
				final float t = (float) Math.max(0.0, Math.min(1.0, score));
				color = rgb(1.0F - 0.55F * t, 0.65F * (1.0F - t), 0.75F * (1.0F - t));
			}

			runtime.spawnDustParticle(entity, color, x, y, z);
		}
	}

	private static int rgb(final float red, final float green, final float blue) {
		final int r = Math.round(red * 255.0F);
		final int g = Math.round(green * 255.0F);
		final int b = Math.round(blue * 255.0F);
		return r << 16 | g << 8 | b;
	}

	public static float onLivingDamage(
			final MyLivingEntity victim, final MyDamageSource source, float amount, final int currentTick) {

		final MyLivingEntity attacker = source.attacker();

		/*
		Offensive Attack
		 */
		double lingeringWoundLevel = 0.0;

		if (attacker != null) {
			final List<Relic> attackerRelics = MyRuntime.getRuntimeUtils().gatherRelics(attacker);
			lingeringWoundLevel = Loader.levelOfSuchPassiveSkill(
					attackerRelics, Relic.PASSIVE_SKILL_LINGERING_WOUND);

			amount = (float) Loader.applyCallback(
					attackerRelics, "damage_dealt", amount, victim.maxHealth());

			victim.setInvulnerableTime(Math.round((float) Loader.applyCallback(
					attackerRelics, "invulnerable_time_dealt", victim.invulnerableTime(), 10.0)));

			final double lifestealLevel = Loader.levelOfSuchPassiveSkill(
					attackerRelics, Relic.PASSIVE_SKILL_LIFESTEAL);

			if (lifestealLevel > 0.0 && amount > 0.0F)
				attacker.heal((float) (amount * lifestealLevel));
		}

		/*
		Protection
		 */

		final List<Relic> victimRelics = MyRuntime.getRuntimeUtils().gatherRelics(victim);

		amount = (float) Loader.applyCallback(
				victimRelics, "damage_taken", amount, victim.maxHealth());

		final int invulnerableTime = Math.round((float) Loader.applyCallback(
				victimRelics, "invulnerable_time_taken", victim.invulnerableTime(), 10.0));

		final double ironCurtainLevel = Loader.levelOfSuchPassiveSkill(
				victimRelics, Relic.PASSIVE_SKILL_IRON_CURTAIN);
		if (ironCurtainLevel != 0.0) {
			if (victim.invulnerableTime() != invulnerableTime) {
				if (!Scheduler.INSTANCE().acquireProtection(victim.uuid(), currentTick, invulnerableTime)) {
					amount = 0.0F;
				}

				victim.setInvulnerableTime(invulnerableTime);
			}
		}

		/*
		Footer
		 */

		// Passive Skill: Lingering Wound
		if (attacker != null && lingeringWoundLevel > 0.0 && amount > 0.0F) {
			if (!source.isExtraDamage()) {
				accumulateLingeringWound(victim, amount, lingeringWoundLevel, currentTick);
			}

			Scheduler.INSTANCE().addDelayedTask(
					victim.uuid(),
					new Scheduler.DelayTask(
							1, 1, () -> {
						if (!victim.isLoaded() || victim.isDeadOrDying())
							return;

						applyLingeringWound(attacker, victim);
					}
					),
					currentTick
			);
		}

		// Passive Skill: Thorns
		if (attacker != null && amount > 0.0F) {
			final double thornsLevel = Loader.levelOfSuchPassiveSkill(
					victimRelics, Relic.PASSIVE_SKILL_THORNS);

			if (thornsLevel > 0.0 && Scheduler.INSTANCE().acquireThorns(
					victim.uuid(), currentTick, 10)) {

				final float thornsDamage = (float) (amount * thornsLevel);

				Scheduler.INSTANCE().addDelayedTask(
						attacker.uuid(),
						new Scheduler.DelayTask(
								1, 1, () -> {
							if (!attacker.isLoaded() || attacker.isDeadOrDying())
								return;

							attacker.hurtThorns(victim, thornsDamage);
						}
						),
						currentTick
				);
			}
		}

		return amount;
	}

	/*
	Lingering Wound
	 */
	public static void accumulateLingeringWound(
			final MyLivingEntity target, final float amount, final double level, final int currentTick) {

		if (level <= 0.0)
			return;

		Scheduler.INSTANCE().addHealingPrevention(
				target.uuid(), currentTick, (float) (amount * level));
	}

	public static void applyLingeringWound(
			final MyLivingEntity attacker, final MyLivingEntity target) {

		final float accumulatedDamage = Scheduler.INSTANCE().healingPrevention(target.uuid());
		final float allowedHealth = target.maxHealth() - accumulatedDamage;

		if (target.health() > allowedHealth) {
			MyUtils.trueHurt(attacker, target, target.health() - allowedHealth);
		}
	}

	public static void onArrowShot(
			final MyAbstractArrow arrow, final MyLivingEntity owner, final List<Relic> relics) {

		final double level = Loader.levelOfSuchPassiveSkill(
				relics, Relic.PASSIVE_SKILL_EMPOWERED_ARROW);

		if (level <= 0.0)
			return;

		arrow.setVelocity(
				arrow.velocityX() * level, arrow.velocityY() * level, arrow.velocityZ() * level);

		arrow.setBaseDamage(arrow.baseDamage() * level);
	}

	public static boolean onArrowImpact(
			final MyAbstractArrow arrow, final MyLivingEntity victim,
			final List<Relic> relics, final int currentTick
	) {

		final double retargetLevel = Loader.levelOfSuchPassiveSkill(
				relics, Relic.PASSIVE_SKILL_RETARGET_ARROW);

		if (retargetLevel > 0.0) {
			arrow.retarget(victim, 1.0, 1.0, retargetLevel);
			return true;
		}

		final double deflectionLevel = Loader.levelOfSuchPassiveSkill(
				relics, Relic.PASSIVE_SKILL_ARROW_DEFLECTION);

		if (deflectionLevel <= 0.0)
			return false;

		final int cooldownTicks = Math.max(1, (int) Math.round(100.0 / deflectionLevel));

		if (!Scheduler.INSTANCE().acquireArrowDeflection(victim.uuid(), currentTick, cooldownTicks))
			return false;

		arrow.retarget(victim, deflectionLevel, deflectionLevel, 0.0);
		return true;
	}

	/*
	Clean Up
	 */

	public static void onPlayerLoggedOut(final UUID uuid) {
		Scheduler.INSTANCE().clearEntity(uuid);
	}

	public static void onServerStopping() {
		Scheduler.INSTANCE().clear();
	}
}
