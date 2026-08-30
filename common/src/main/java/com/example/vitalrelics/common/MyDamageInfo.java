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
							attacker, null, amount,
							null, range, MyRangeFilter.hostileTargeted, 20, neg_leve
					)
			);
		}

		someExtraDamages(attacker, damages);
	}

	public static void directAttack(
			final MyLivingEntity attacker, final MyLivingEntity victim,
			final float amount, final int count) {

		// float attrDamage = (float)attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
		if (count <= 0) {
			return;
		}

		final List<MyDamageInfo> damages = new ArrayList<>(count);

		for (int i = 0; i < count; ++i) {
			damages.add(
					new MyDamageInfo(
							attacker, victim, amount,
							null, 0.f, MyRangeFilter.none, 20, 5
					)
			);
		}

		someExtraDamages(attacker, damages);
	}

	public static void counterStrike(
			final MyDamageSource source, final MyLivingEntity revenger, final int count) {

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
							revenger, target, extra_damage_amount,
							0.02f, 2.f, MyRangeFilter.nonallied, 20, 5
					)
			);
		}

		someExtraDamages(revenger, damages);
	}

	public static void someExtraDamages(
			final MyLivingEntity attacker, final List<MyDamageInfo> infos) {

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
	public enum MyRangeFilter
	{
		none,
		all,
		nonallied,
		hostileTargeted
	}

	// private final MyDamageType type;
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
	 * @param range All non-allied LivingEntity within range to apply. Specify null to disable ranged attack.
	 */
	public MyDamageInfo(
			final MyLivingEntity attacker,
			final MyLivingEntity target,
			final Float amount,
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


		target.hurt(new_source, amount_to_apply);
	}
}
