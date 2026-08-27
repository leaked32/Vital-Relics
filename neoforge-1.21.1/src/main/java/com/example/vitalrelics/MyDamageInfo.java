package com.example.vitalrelics;

import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.Scheduler;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.vitalrelics.Utils.*;
import static com.example.vitalrelics.VitalRelics.LOGGER;

public class MyDamageInfo
{
	/*
	Public convenient static functions
	 */
	public static void directRangedAttack(final LivingEntity attacker, final float amount, final int range, final int count, final int neg_leve) {
		if (count <= 0) {
			return;
		}

		final List<MyDamageInfo> damages = new ArrayList<>(count);

		for (int i = 0; i < count; i += 1) {
			damages.add(
					new MyDamageInfo(
							attacker, null, amount, MyDamageType.normal,
							null, range,  MyRangeFilter.hostileTargeted, 20, neg_leve
					)
			);
		}

		someExtraDamages(attacker, damages);
	}

	public static void directAttack(LivingEntity attacker, LivingEntity victim, float amount, final int count) {
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

	public static void counterStrike(DamageSource source, LivingEntity revenger, final int count) {
		if (!(source.getEntity() instanceof LivingEntity target)) return;

		// Anti dead-lock
		if (MyDamageInfo.MyDamageTypes.isExtraDamage(source)) {
			return;
		}

		float attrDamage = (float)revenger.getAttributeValue(Attributes.ATTACK_DAMAGE);
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

	public static void someExtraDamages(LivingEntity attacker, List<MyDamageInfo> infos) {
		MinecraftServer server = attacker.getServer();
		if (server == null) {
			return;
		}

		for (int i = 0; i < infos.size(); ++i) {
			final int index = i;

			Scheduler.INSTANCE().addDelayedTask(
					attacker.getUUID(),
					new Scheduler.DelayTask(
							(i + 1) * 3,
							1,
							() -> {
								if (attacker.level().isClientSide) {
									return;
								}
								infos.get(index).deal_damage();
							}
					),
					server.getTickCount()
			);
		}
	}

	private static void ascentWeaken(
			final LivingEntity target,
			final int duration,
			final int amplifier) {

		addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier);
		addEffect(target, MobEffects.WITHER, duration, amplifier);
		addEffect(target, MobEffects.DARKNESS, duration, 0);
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

	private final @Nullable Float amount;
	private final @Nullable Float ratioAmount;

	private final LivingEntity attacker;
	private final @Nullable LivingEntity target;

	private final MyRangeFilter range_filter;
	private final float range;

	private final int weakenDura;
	private final int weakenStrength;

	/**
	 * @param amount Damage amount to apply
	 * @param type MyDamageType
	 * @param range All non-allied LivingEntity within range to apply. Specify null to disable ranged attack.
	 */
	public MyDamageInfo(LivingEntity attacker, @Nullable LivingEntity target, @Nullable Float amount, MyDamageType type, @Nullable Float ratioAmount, float range, MyRangeFilter range_filter, int dura, int strength) {
		if (attacker == null) {
			throw new RuntimeException("`MyDamageInfo` constructor: null `attacker` is not acceptable");
		}
		if (range_filter == null) {
			throw new RuntimeException("`MyDamageInfo` constructor: null `range_filter` is not acceptable");
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
			_deal_to_range(target);
		}
		else if (range_filter != MyRangeFilter.none)  {
			_deal_to_range(attacker);
		}
		else {
			throw new RuntimeException("`MyDamageInfo`: neither range nor target is specified.");
		}
	}


	private void _deal_to_range(LivingEntity centralized) {
		if (range_filter == MyRangeFilter.none) {
			return;
		}

		List<LivingEntity> all_nearby = getLivingEntitiesInRange(centralized, range);
		for (LivingEntity nearby_target : all_nearby) {
			switch (range_filter) {
				case nonallied:
					if (isAllied(attacker, nearby_target)) {
						continue;
					}
					if (target != null && nearby_target.is(target)) {
						continue;
					}
					dealToLivingEntity(nearby_target);
					continue;
				case hostileTargeted:
					if (!hostileTargeted(attacker, nearby_target)) {
						continue;
					}
					if (target != null && nearby_target.is(target)) {
						continue;
					}
					dealToLivingEntity(nearby_target);
					continue;
			}
		}
	}


	private void dealToLivingEntity(LivingEntity target) {
		if (target.isDeadOrDying() || !target.level().isLoaded(target.blockPosition())) {
			return;
		}
		if (attacker.getUUID().equals(target.getUUID())) {
			return;
		}
		if (isAllied(attacker, target)) {
			return;
		}

		var dummy_type = type;
		if (_excluded_target(target)) {
			dummy_type = type == MyDamageType.my_penetrate ? MyDamageType.real_damage : MyDamageType.normal;
		}

		DamageSource new_source = MyDamageTypes.myDamageSource(target.level(), attacker);

		float amount_to_apply = 0.f;
		if (amount != null) {
			amount_to_apply += amount;
		}
		if (ratioAmount != null) {
			amount_to_apply += target.getMaxHealth() * ratioAmount;
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
					LOGGER.info("RealityPiercer deal_extra_damage unsuccessful hurt, try 'reality_piercer_penetrate'");
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

	public static List<LivingEntity> getLivingEntitiesInRange(Entity center, double radius) {
		Level level = center.level();
		AABB box = center.getBoundingBox().inflate(radius, radius, radius);

		return level.getEntitiesOfClass(LivingEntity.class, box, entity ->
				entity != center && entity.isAlive()
		);
	}

	// Accumulated damage
	private static void realityPiercerPenetrate(LivingEntity target, DamageSource source, float amount, boolean allow_remove) {

		UUID uuid = target.getUUID();
		MinecraftServer server = target.level().getServer();
		if (server == null) return;
		// LOGGER.info("Reality Piercer add_prevent_heal logging target: {}", target.toString());
		final float least_damage =
				Scheduler.INSTANCE().addHealingPrevention(
						target.getUUID(),
						server.getTickCount(),
						amount
				);

		float allowed_max_health = target.getMaxHealth() - least_damage;

		if (allowed_max_health > 0.f) {
			// float delta_health = target.getHealth() - allowed_max_health;
			if (target.getHealth() > allowed_max_health) {
				// Excessive Health
				// LOGGER.warn("`add_prevent_heal` abnormal health, target: {}, allowed_max_health: {}", target.toString(), allowed_max_health);
				myPenetrate(target, source, target.getHealth() - allowed_max_health, allow_remove);
			} else {
				// LOGGER.debug("common case, target: {}, allowed_max_health: {}", target.toString(), allowed_max_health);
				myPenetrate(target, source, amount, allow_remove);
			}
		} else if (target.getHealth() > 0.f && (!target.isDeadOrDying())) {
			// Kill
			// LOGGER.warn("`add_prevent_heal` it shall die: {}", target.toString());
			myPenetrate(target, source, amount, allow_remove);
		}
	}

	// This function deals normal damage
	private static void myPenetrate(final LivingEntity target, DamageSource source, final float amount, final boolean allow_remove) {
		float decided_amount = 0.f;
		target.invulnerableTime = 0;

		target.hurt(source, amount);

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

	public static boolean _excluded_target(final LivingEntity target) {
		final ResourceLocation id =
				BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());

		return _penetration_excluded_entities.contains(id.toString());
	}


	public static class MyDamageTypes {
		public static final String DAMAGE_TAG = "extra_damage";
		public static final String DAMAGE_STR = "DamageSource (" + Manifest.MODID + "." + DAMAGE_TAG + ")";

		public static DamageSource myDamageSource(Level level, Entity entity) {
			HolderLookup.RegistryLookup<DamageType> damageTypeLookup = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);

			Holder<DamageType> baseHolder = damageTypeLookup.getOrThrow(
					ResourceKey.create(
							Registries.DAMAGE_TYPE,
							ResourceLocation.fromNamespaceAndPath("minecraft", "generic")
					)
			);


			DamageSource extraDamageSource = new DamageSource(baseHolder, entity) {
				@Override
				public boolean is(TagKey<DamageType> tag) {
					if (tag == DamageTypeTags.BYPASSES_COOLDOWN) {
						return true;
					}
					return super.is(tag);
				}

				@Override
				public String toString() {
					return DAMAGE_STR;
				}
			};
			return extraDamageSource;
		}

		public static boolean isExtraDamage(DamageSource source) {
			String message_id = source.toString().trim();
			LOGGER.info("Damage Type is: " + message_id);
			return message_id.equals(DAMAGE_STR) || message_id.equals("DamageSource (revelationfix.fe_power.0)");
			// return source.typeHolder().is(APOSTLE_EXTRA_DAMAGE_KEY);
		}
	}
}
