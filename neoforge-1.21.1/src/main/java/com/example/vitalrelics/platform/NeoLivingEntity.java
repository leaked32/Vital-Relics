package com.example.vitalrelics.platform;

import com.example.vitalrelics.Utils;
import com.example.vitalrelics.common.platform.*;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.example.vitalrelics.VitalRelics.LOGGER;

public final class NeoLivingEntity implements MyLivingEntity {
	final LivingEntity entity;

	public NeoLivingEntity(final LivingEntity entity) {
		this.entity = entity;
	}

	public LivingEntity nativeEntity() {
		return entity;
	}

	@Override
	public void teleport(
			final double x,
			final double y,
			final double z) {

		if (!(entity.level() instanceof ServerLevel level))
			return;

		if (entity instanceof ServerPlayer player) {
			player.teleportTo(
					level,
					x, y, z,
					player.getYRot(), player.getXRot()
			);
		} else {
			entity.teleportTo(x, y, z);
		}
	}

	@Override
	public void playSound(final MySound sound) {
		if (!(entity.level() instanceof ServerLevel level))
			return;

		switch (sound) {
			case TELEPORT -> level.playSound(
					null,
					entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.ENDERMAN_TELEPORT,
					SoundSource.PLAYERS,
					0.8F, 1.15F
			);
			case ILLUSIONER_CAST -> level.playSound(
					null,
					entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.ILLUSIONER_CAST_SPELL,
					SoundSource.PLAYERS,
					1.0F, 1.0F
			);
			case PLAYER_LEVELUP -> level.playSound(
					null,
					entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.PLAYER_LEVELUP,
					SoundSource.PLAYERS,
					1.0F, 1.0F
			);
			case AMETHYST_CHIME -> level.playSound(
					null,
					entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.AMETHYST_BLOCK_CHIME,
					SoundSource.PLAYERS,
					1.0F, 1.0F
			);
			case DRAGON_FLAP -> level.playSound(
					null,
					entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.ENDER_DRAGON_FLAP,
					SoundSource.PLAYERS,
					1.0F, 1.0F
			);
			case EVOKER_CAST -> level.playSound(
					null,
					entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.EVOKER_CAST_SPELL,
					SoundSource.PLAYERS,
					1.0F, 1.0F
			);
			case GENERIC_EXPLODE -> level.playSound(
					null,
					entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.GENERIC_EXPLODE,
					SoundSource.PLAYERS,
					1.0F, 1.0F
			);
			case BEACON_ACTIVATE -> level.playSound(
					null,
					entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.BEACON_ACTIVATE,
					SoundSource.PLAYERS,
					1.0F, 1.0F
			);
			case ENCHANTMENT_TABLE_USE -> level.playSound(
					null,
					entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.ENCHANTMENT_TABLE_USE,
					SoundSource.PLAYERS,
					1.0F, 1.0F
			);
		}
	}

	@Override
	public boolean hurt(
			final MyDamageSource source,
			final float amount) {

		if (!(source.attacker() instanceof NeoLivingEntity neoAttacker)) {
			throw new IllegalArgumentException(
					"Expected NeoLivingEntity attacker"
			);
		}

		return switch (source.kind()) {
			case EXTRA_DAMAGE ->
					entity.hurt(
							_extraDamageSource(neoAttacker.entity),
							amount
					);

			case OTHER -> throw new IllegalArgumentException(
					"OTHER damage cannot be created by MyDamageInfo"
			);
		};
	}

	@Override
	public void resetInvulnerableTime() {
		entity.invulnerableTime = 0;
	}

	@Override
	public float health() {
		return entity.getHealth();
	}

	@Override
	public float maxHealth() {
		return entity.getMaxHealth();
	}

	@Override
	public float attackDamage() {
		return (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
	}

	@Override
	public double x() {
		return entity.getX();
	}

	@Override
	public double z() {
		return entity.getZ();
	}

	@Override
	public void push(
			final double x,
			final double y,
			final double z) {

		entity.push(x, y, z);
	}

	@Override
	public void markMovementChanged() {
		entity.hurtMarked = true;
	}

	@Override
	public boolean isDeadOrDying() {
		return entity.isDeadOrDying();
	}

	@Override
	public boolean isLoaded() {
		return entity.level().isLoaded(entity.blockPosition());
	}

	@Override
	public boolean isClientSide() {
		return entity.level().isClientSide();
	}

	@Override
	public boolean is(final MyLivingEntity other) {
		return other instanceof NeoLivingEntity neo &&
				entity.is(neo.entity);
	}

	@Override
	public boolean isAllied(final MyLivingEntity other) {
		if (!(other instanceof NeoLivingEntity neo)) {
			return false;
		}
		return Utils.isAllied(entity, neo.entity);
	}

	@Override
	public boolean hostileTargeted(final MyLivingEntity other) {
		if (!(other instanceof NeoLivingEntity neo)) {
			return false;
		}
		return Utils.hostileTargeted(entity, neo.entity);
	}

	@Override
	public List<MyLivingEntity> livingEntitiesInRange(final double radius) {
		final var box =
				entity.getBoundingBox().inflate(radius, radius, radius);

		return entity.level()
				.getEntitiesOfClass(
						LivingEntity.class,
						box,
						target -> target != entity && target.isAlive()
				)
				.stream()
				.map(NeoLivingEntity::new)
				.map(MyLivingEntity.class::cast)
				.toList();
	}

	@Override
	public void addEffect(
			final MyEffect effect,
			final int duration,
			final int amplifier) {

		final var nativeEffect = switch (effect) {
			case MOVEMENT_SLOWDOWN -> MobEffects.MOVEMENT_SLOWDOWN;
			case WITHER -> MobEffects.WITHER;
			case DARKNESS -> MobEffects.DARKNESS;
			case ABSORPTION -> MobEffects.ABSORPTION;
		};

		_addEffect(entity, nativeEffect, duration, amplifier);
	}

	@Override
	public UUID uuid() {
		return entity.getUUID();
	}

	@Override
	public String typeId() {
		return BuiltInRegistries.ENTITY_TYPE
				.getKey(entity.getType())
				.toString();
	}

	@Override
	public int serverTick() {
		final var server = entity.getServer();
		return server == null ? -1 : server.getTickCount();
	}

	@Override
	public void logInfo(final String message) {
		LOGGER.info(message);
	}

	@Override
	public void heal(final float amount) {
		entity.heal(amount);
	}


	@Override
	public double y() {
		return entity.getY();
	}


	@Override
	public double horizontalLookX() {
		return entity.getLookAngle().x;
	}

	@Override
	public double horizontalLookZ() {
		return entity.getLookAngle().z;
	}

	@Override
	public void setVelocity(
			final double x,
			final double y,
			final double z) {

		entity.setDeltaMovement(x, y, z);
	}

	@Override
	public double width() {
		return entity.getBbWidth();
	}

	@Override
	public List<MyEffectInstance> activeEffects() {
		return entity.getActiveEffects()
				.stream()
				.map(instance -> {
					final var effect = instance.getEffect().value();

					final ResourceLocation id =
							BuiltInRegistries.MOB_EFFECT.getKey(effect);

					if (id == null)
						return null;

					final MyEffectCategory category =
							switch (effect.getCategory()) {
								case BENEFICIAL -> MyEffectCategory.POSITIVE;
								case HARMFUL -> MyEffectCategory.NEGATIVE;
								case NEUTRAL -> MyEffectCategory.NEUTRAL;
							};

					return new MyEffectInstance(
							id.getPath(),
							category
					);
				})
				.filter(Objects::nonNull)
				.toList();
	}


	@Override
	public void removeEffect(final String id) {
		final ResourceLocation resource =
				ResourceLocation.fromNamespaceAndPath("minecraft", id);

		final var effect =
				BuiltInRegistries.MOB_EFFECT.get(resource);

		if (effect == null)
			return;

		entity.removeEffect(
				BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)
		);
	}

	@Override
	public void addEffect(
			final String id,
			final int duration,
			final int amplifier,
			final boolean ambient,
			final boolean visible) {

		final ResourceLocation resource =
				ResourceLocation.fromNamespaceAndPath("minecraft", id);

		final var effect =
				BuiltInRegistries.MOB_EFFECT.get(resource);

		if (effect == null)
			return;

		entity.addEffect(
				new MobEffectInstance(
						BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
						duration,
						amplifier,
						ambient,
						visible
				)
		);
	}

	/*
	Private functions
	 */

	public static void _addEffect(
			final LivingEntity target, final Holder<MobEffect> effect,
			final int duration, final int amplifier) {

		if (target.isDeadOrDying() ||
				!target.level().isLoaded(target.blockPosition())) {
			return;
		}

		final MobEffectInstance instance =
				new MobEffectInstance(effect, duration, amplifier, true, true);

		target.forceAddEffect(instance, null);
	}

	private DamageSource _extraDamageSource(final LivingEntity attacker) {
		final HolderLookup.RegistryLookup<DamageType> damageTypeLookup =
				entity.level()
						.registryAccess()
						.lookupOrThrow(Registries.DAMAGE_TYPE);

		final Holder<DamageType> baseHolder =
				damageTypeLookup.getOrThrow(
						ResourceKey.create(
								Registries.DAMAGE_TYPE,
								ResourceLocation.fromNamespaceAndPath(
										"minecraft",
										"generic"
								)
						)
				);

		return new DamageSource(baseHolder, attacker) {
			@Override
			public boolean is(final TagKey<DamageType> tag) {
				if (tag == DamageTypeTags.BYPASSES_COOLDOWN) {
					return true;
				}
				return super.is(tag);
			}

			@Override
			public String toString() {
				return "DamageSource (vitalrelics.extra_damage)";
			}
		};
	}


}
