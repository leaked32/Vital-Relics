package com.example.vitalrelics.common;

import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MyUtils;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.common.relics.Relic;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MyEvents {
	private MyEvents() {}

	public static void onLivingEntityTick(
			MyLivingEntity myLivingEntity, final int currentTick, List<Relic> relics) {
		final Map<String, Relic.Ticks.Info> ticks =
				Loader.computeTicks(relics, currentTick);

		for (final var entry : ticks.entrySet()) {
			final Relic.Ticks.Info value = entry.getValue();

			switch (entry.getKey()) {
				case "heal" -> myLivingEntity.heal((float) (
						value.add + myLivingEntity.maxHealth() * value.ratio_add));
				case "feed" -> {
					final float amount = (float) (
							value.add + myLivingEntity.maxHealth() * value.ratio_add);
					myLivingEntity.feed(Math.round(amount), 1.0F);
				}
			}
		}

		// Scheduled to update on each half seconds
//		if (currentTick % 10 == 0) {
//		}

		// Scheduled to update on each second
		if (currentTick % 20 == 0) {
			// Effects
			MyUtils.removeImmuneEffects(myLivingEntity, relics, MyLivingEntity.MyEffectCategory.ALL);
			MyUtils.applyRelicEffects(myLivingEntity, relics);

			// Passive Skill: reality_severance

			final double reality_severance_level =
					Loader.levelOfSuchPassiveSkill(relics, Relic.PASSIVE_SKILL_REALITY_SEVERANCE);

			if (reality_severance_level > 0.0) {
				final float ratioDamage = (float) (reality_severance_level / 100.0);
				final float rangeDamage = ratioDamage * myLivingEntity.attackDamage();

				MyDamageInfo.directRangedAttack(
						myLivingEntity,
						rangeDamage,
						Math.round((float) reality_severance_level),
						1,
						Math.round((float) (reality_severance_level / 4.0))
				);
			}

			// Client HUD
			if (myLivingEntity.isServerPlayer()) {
				MySpellSystem.INSTANCE.syncSpellHud(myLivingEntity);
			}
		}

		// Scheduled to update on each 4 seconds
		if (currentTick % 80 == 0) {
			final double metalMendingLevel = Loader.levelOfSuchPassiveSkill(
					relics, Relic.PASSIVE_SKILL_METAL_MENDING
			);

			if (metalMendingLevel > 0.0) {
				myLivingEntity.mendEquipment(
						Math.max(1, (int) Math.round(metalMendingLevel))
				);
			}
		}

	}

	public static float onLivingDamage(
			final MyLivingEntity victim, final MyLivingEntity attacker, float amount, final int currentTick) {

		/*
		Offensive Attack
		 */
		double lingeringWoundLevel = 0.0;

		if (attacker != null) {
			final List<Relic> attackerRelics =
					MyRuntime.getRuntimeUtils().gatherRelics(attacker);
			lingeringWoundLevel = Loader.levelOfSuchPassiveSkill(
					attackerRelics, Relic.PASSIVE_SKILL_LINGERING_WOUND
			);

			amount = (float) Loader.applyCallback(
					attackerRelics, "damage_dealt", amount, victim.maxHealth()
			);

			victim.setInvulnerableTime(Math.round((float) Loader.applyCallback(
					attackerRelics, "invulnerable_time_dealt",
					victim.invulnerableTime(), 10.0
			)));

			final double lifestealLevel = Loader.levelOfSuchPassiveSkill(
					attackerRelics, Relic.PASSIVE_SKILL_LIFESTEAL
			);

			if (lifestealLevel > 0.0 && amount > 0.0F)
				attacker.heal((float) (amount * lifestealLevel));
		}

		/*
		Protection
		 */

		final List<Relic> victimRelics =
				MyRuntime.getRuntimeUtils().gatherRelics(victim);

		amount = (float) Loader.applyCallback(
				victimRelics, "damage_taken", amount, victim.maxHealth()
		);

		final int invulnerableTime = Math.round((float) Loader.applyCallback(
				victimRelics, "invulnerable_time_taken",
				victim.invulnerableTime(), 10.0
		));

		final double ironCurtainLevel =  Loader.levelOfSuchPassiveSkill(
				victimRelics, Relic.PASSIVE_SKILL_IRON_CURTAIN
		);
		if (ironCurtainLevel != 0.0) {
			if (victim.invulnerableTime() != invulnerableTime) {
				if (!Scheduler.INSTANCE().acquireProtection(
						victim.uuid(), currentTick, invulnerableTime
				)) {
					amount = 0.0F;
				}

				victim.setInvulnerableTime(invulnerableTime);
			}
		}

		if (attacker != null && lingeringWoundLevel > 0.0 && amount > 0.0F) {
			accumulateLingeringWound(
					victim, amount, lingeringWoundLevel, currentTick
			);

			applyLingeringWound(attacker, victim);
		}

		/*
		Footer
		 */

		// Thorns
		if (attacker != null && amount > 0.0F) {
			final double thornsLevel = Loader.levelOfSuchPassiveSkill(
					victimRelics, Relic.PASSIVE_SKILL_THORNS
			);

			if (thornsLevel > 0.0 && Scheduler.INSTANCE().acquireThorns(
					victim.uuid(), currentTick, 10
			)) {
				attacker.hurtThorns(victim, (float) (amount * thornsLevel));
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
				target.uuid(), currentTick, (float) (amount * level)
		);
	}

	public static void applyLingeringWound(
			final MyLivingEntity attacker,
			final MyLivingEntity target) {

		final float accumulatedDamage =
				Scheduler.INSTANCE().healingPrevention(target.uuid());

		final float allowedHealth =
				target.maxHealth() - accumulatedDamage;

		if (target.health() > allowedHealth) {
			MyUtils.trueHurt(
					attacker,
					target,
					target.health() - allowedHealth
			);
		}
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
