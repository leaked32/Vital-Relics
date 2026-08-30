package com.example.vitalrelics.common;

import com.example.vitalrelics.common.platform.*;

import java.util.ArrayList;
import java.util.List;

public class MyDamageInfo
{
	/*
	Public convenient static functions
	 */
	public static void directRangedAttack(
			final MyLivingEntity attacker,
			final float amount, final int range, final int count, final int neg_leve) {

		if (count <= 0) {
			return;
		}

		final List<MyDamageInfo> damages = new ArrayList<>(count);

		for (int i = 0; i < count; i += 1) {
			damages.add(
					new MyDamageInfo(
							attacker, null, amount, MyDamageType.normal,
							null, range, MyRangeFilter.hostileTargeted, 20, neg_leve
					)
			);
		}

		someExtraDamages(attacker, damages);
	}

	public static void directAttack(
			final MyLivingEntity attacker,
			final MyLivingEntity victim,
			final float amount,
			final int count) {

		// float attrDamage = (float)attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
		if (count <= 0) {
			return;
		}

		final List<MyDamageInfo> damages = new ArrayList<>(count);

		for (int i = 0; i < count; ++i) {
			damages.add(
					new MyDamageInfo(
							attacker, victim, amount, MyDamageType.normal,
							null, 0.f, MyRangeFilter.none, 20, 5
					)
			);
		}

		someExtraDamages(attacker, damages);
	}

	public static void counterStrike(
			final MyDamageSource source,
			final MyLivingEntity revenger,
			final int count) {

		final MyLivingEntity target = source.attacker();
		if (target == null)
			return;

		// Anti dead-lock
		if (source.isExtraDamage()) {
			return;
		}

		float attrDamage = revenger.attackDamage();
		final float extra_damage_amount = Math.max(attrDamage * .25f, 4.f);

		if (count <= 0) {
			return;
		}

		final List<MyDamageInfo> damages = new ArrayList<>(count);

		for (int i = 0; i < count; ++i) {
			damages.add(
					new MyDamageInfo(
							revenger, target, extra_damage_amount, MyDamageType.normal,
							0.02f, 2.f, MyRangeFilter.nonallied, 20, 5
					)
			);
		}

		someExtraDamages(revenger, damages);
	}

	public static void someExtraDamages(
			final MyLivingEntity attacker,
			final List<MyDamageInfo> infos) {

		final int tick = attacker.serverTick();
		if (tick < 0) {
			return;
		}

		for (int i = 0; i < infos.size(); ++i) {
			final int index = i;

			Scheduler.INSTANCE().addDelayedTask(
					attacker.uuid(),
					new Scheduler.DelayTask(
							(i + 1) * 3,
							1,
							() -> {
								if (attacker.isClientSide()) {
									return;
								}
								infos.get(index).deal_damage();
							}
					),
					tick
			);
		}
	}

	private static void ascentWeaken(
			final MyLivingEntity target,
			final int duration,
			final int amplifier) {

		target.addEffect(MyEffect.MOVEMENT_SLOWDOWN, duration, amplifier);
		target.addEffect(MyEffect.WITHER, duration, amplifier);
		target.addEffect(MyEffect.DARKNESS, duration, 0);
	}

	/*
	Non-static Members
	 */

	public enum MyDamageType
	{
		normal,
		real_damage,
		my_penetrate,
		my_penetrate_if_normal_failed
	}

	public enum MyRangeFilter
	{
		none,
		all,
		nonallied,
		hostileTargeted
	}

	private final MyDamageType type;

	private final Float amount;
	private final Float ratioAmount;

	private final MyLivingEntity attacker;
	private final MyLivingEntity target;

	private final MyRangeFilter range_filter;
	private final float range;

	private final int weakenDura;
	private final int weakenStrength;

	/**
	 * @param amount Damage amount to apply
	 * @param type MyDamageType
	 * @param range All non-allied LivingEntity within range to apply. Specify null to disable ranged attack.
	 */
	public MyDamageInfo(
			final MyLivingEntity attacker,
			final MyLivingEntity target,
			final Float amount,
			final MyDamageType type,
			final Float ratioAmount,
			final float range,
			final MyRangeFilter range_filter,
			final int dura,
			final int strength) {

		if (attacker == null) {
			throw new RuntimeException(
					"`MyDamageInfo` constructor: null `attacker` is not acceptable"
			);
		}
		if (range_filter == null) {
			throw new RuntimeException(
					"`MyDamageInfo` constructor: null `range_filter` is not acceptable"
			);
		}

		this.attacker = attacker;
		this.target = target;
		this.amount = amount;
		this.ratioAmount = ratioAmount;
		this.type = type;
		this.range = range;
		this.range_filter = range_filter;

		this.weakenDura = dura;
		this.weakenStrength = strength;
	}

	public void deal_damage()
	{
		if (target != null) {
			dealToLivingEntity(target);
			dealToRange(target);
		}
		else if (range_filter != MyRangeFilter.none)  {
			dealToRange(attacker);
		}
		else {
			throw new RuntimeException(
					"`MyDamageInfo`: neither range nor target is specified."
			);
		}
	}

	private void dealToRange(final MyLivingEntity centralized) {
		if (range_filter == MyRangeFilter.none) {
			return;
		}

		final List<MyLivingEntity> all_nearby =
				centralized.livingEntitiesInRange(range);

		for (final MyLivingEntity nearby_target : all_nearby) {
			switch (range_filter) {
				case all:
					if (target != null && nearby_target.is(target)) {
						continue;
					}
					dealToLivingEntity(nearby_target);
					continue;

				case nonallied:
					if (attacker.isAllied(nearby_target)) {
						continue;
					}
					if (target != null && nearby_target.is(target)) {
						continue;
					}
					dealToLivingEntity(nearby_target);
					continue;

				case hostileTargeted:
					if (!attacker.isHostileTargeted(nearby_target)) {
						continue;
					}
					if (target != null && nearby_target.is(target)) {
						continue;
					}
					dealToLivingEntity(nearby_target);
					continue;

				case none:
					continue;
			}
		}
	}

	private void dealToLivingEntity(final MyLivingEntity target) {
		if (target.isDeadOrDying() || !target.isLoaded()) {
			return;
		}
		if (attacker.uuid().equals(target.uuid())) {
			return;
		}
		if (attacker.isAllied(target)) {
			return;
		}

		var dummy_type = type;
		if (_excluded_target(target)) {
			dummy_type = type == MyDamageType.my_penetrate
					? MyDamageType.real_damage
					: MyDamageType.normal;
		}

		final MyDamageSource new_source =
				new MyDamageSource(attacker, MyDamageKind.EXTRA_DAMAGE);

		float amount_to_apply = 0.f;
		if (amount != null) {
			amount_to_apply += amount;
		}
		if (ratioAmount != null) {
			amount_to_apply += target.maxHealth() * ratioAmount;
		}

		if (weakenStrength != 0 && weakenDura != 0) {
			ascentWeaken(target, weakenDura, weakenStrength);
		}

		switch (dummy_type) {
			case normal: {
				target.hurt(new_source, amount_to_apply);
			}
			break;

			case real_damage: {
				boolean suc = target.hurt(new_source, amount_to_apply);
				if (!suc) {
					myPenetrate(target, new_source, amount_to_apply, false);
				}
			}
			break;

			case my_penetrate_if_normal_failed: {
				boolean suc = target.hurt(new_source, amount_to_apply);

				if (!suc) {
//					attacker.logInfo(
//							"RealityPiercer deal_extra_damage unsuccessful hurt, " +
//							"try 'reality_piercer_penetrate'"
//					);
					realityPiercerPenetrate(target, new_source, amount_to_apply * 2.f, true);
				}
			}
			break;

			case my_penetrate: {
				realityPiercerPenetrate(target, new_source, amount_to_apply, true);
			}
			break;
		}
	}

	// Accumulated damage
	private static void realityPiercerPenetrate(
			final MyLivingEntity target,
			final MyDamageSource source,
			final float amount,
			final boolean allow_remove) {

		final int tick = target.serverTick();
		if (tick < 0)
			return;

		// LOGGER.info("Reality Piercer add_prevent_heal logging target: {}", target.toString());
		final float least_damage =
				Scheduler.INSTANCE().addHealingPrevention(
						target.uuid(),
						tick,
						amount
				);

		float allowed_max_health = target.maxHealth() - least_damage;

		if (allowed_max_health > 0.f) {
			// float delta_health = target.getHealth() - allowed_max_health;
			if (target.health() > allowed_max_health) {
				// Excessive Health
				// LOGGER.warn("`add_prevent_heal` abnormal health, target: {}, allowed_max_health: {}", target.toString(), allowed_max_health);
				myPenetrate(target, source, target.health() - allowed_max_health, allow_remove);
			} else {
				// LOGGER.debug("common case, target: {}, allowed_max_health: {}", target.toString(), allowed_max_health);
				myPenetrate(target, source, amount, allow_remove);
			}
		} else if (target.health() > 0.f && (!target.isDeadOrDying())) {
			// Kill
			// LOGGER.warn("`add_prevent_heal` it shall die: {}", target.toString());
			myPenetrate(target, source, amount, allow_remove);
		}
	}

	// This function deals normal damage
	private static void myPenetrate(
			final MyLivingEntity target,
			final MyDamageSource source,
			final float amount,
			final boolean allow_remove) {

		// float decided_amount = 0.f;
		target.resetInvulnerableTime();

		MyUtils.trueHurt(source.attacker(), target, amount);

		//		if (target instanceof IMyMixinUnique my_mixin_entity) {
		//			decided_amount = my_mixin_entity.my_mixin_penetrate(source, amount, allow_remove);
		//		}
		//		if (!target.isDeadOrDying()) {
		//			target.hurtDuration = 10;
		//			target.hurtTime = 10;
		//			target.hurtMarked = true;
		//
		//			target.gameEvent(GameEvent.ENTITY_DAMAGE);
		//
		//			// 3. Force the hurt sound ("cry")
		//			target.playSound(SoundEvents.PLAYER_HURT, 1.f, target.getVoicePitch());
		//		}
		//		target.invulnerableTime = 0;
	}

	/*
	Private static Utils
	 */
	private static final List<String> _penetration_excluded_entities = List.of(
			"entity.goety.apostle",
			"entity.dummmmmmy.target_dummy"
	);

	public static boolean _excluded_target(final MyLivingEntity target) {
		return _penetration_excluded_entities.contains(target.typeId());
	}
}
