package com.example.vitalrelics.platform;

import com.example.vitalrelics.Utils;
import com.example.vitalrelics.common.platform.*;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ForgeLivingEntity implements MyLivingEntity {
	final LivingEntity entity;

	public ForgeLivingEntity(final LivingEntity entity) {
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

		final SoundEvent nativeSound;
		final float volume;
		final float pitch;

		switch (sound) {
			case TELEPORT -> {
				nativeSound = SoundEvents.ENDERMAN_TELEPORT;
				volume = 0.8F;
				pitch = 1.15F;
			}
			case ILLUSIONER_CAST -> {
				nativeSound = SoundEvents.ILLUSIONER_CAST_SPELL;
				volume = 0.8F;
				pitch = 1.15F;
			}
			case PLAYER_LEVELUP -> {
				nativeSound = SoundEvents.PLAYER_LEVELUP;
				volume = 0.5F;
				pitch = 1.4F;
			}
			case AMETHYST_CHIME -> {
				nativeSound = SoundEvents.AMETHYST_BLOCK_CHIME;
				volume = 0.8F;
				pitch = 1.3F;
			}
			case DRAGON_FLAP -> {
				nativeSound = SoundEvents.ENDER_DRAGON_FLAP;
				volume = 0.65F;
				pitch = 0.75F;
			}
			case EVOKER_CAST -> {
				nativeSound = SoundEvents.EVOKER_CAST_SPELL;
				volume = 0.8F;
				pitch = 0.9F;
			}
			case GENERIC_EXPLODE -> {
				nativeSound = SoundEvents.GENERIC_EXPLODE;
				volume = 0.5F;
				pitch = 1.4F;
			}
			case BEACON_ACTIVATE -> {
				nativeSound = SoundEvents.BEACON_ACTIVATE;
				volume = 0.6F;
				pitch = 1.25F;
			}
			case ENCHANTMENT_TABLE_USE -> {
				nativeSound = SoundEvents.ENCHANTMENT_TABLE_USE;
				volume = 1.0F;
				pitch = 1.0F;
			}
			default -> throw new IllegalArgumentException("Unsupported sound: " + sound);
		}

		level.playSound(
				null,
				entity.getX(), entity.getY(), entity.getZ(),
				nativeSound,
				SoundSource.PLAYERS,
				volume,
				pitch
		);
	}

	@Override
	public MyDamageSource extraDamageSource() {
		return new ForgeDamageSource(_extraDamageSource(entity));
	}

	@Override
	public boolean hurt(
			final MyDamageSource source,
			final float amount) {

		if (!(source instanceof ForgeDamageSource forgeSource))
			throw new IllegalArgumentException("Expected ForgeDamageSource");

		return entity.hurt(forgeSource.nativeSource(), amount);
	}

	@Override
	public boolean hurtThorns(final MyLivingEntity source, final float amount) {
		if (!(source instanceof ForgeLivingEntity forgeSource))
			throw new IllegalArgumentException("Expected ForgeLivingEntity source");

		return entity.hurt(entity.damageSources().thorns(forgeSource.entity), amount);
	}

	@Override
	public void resetInvulnerable() {
		if (entity.isInvulnerable()) {
			entity.setInvulnerable(false);
		}
		resetInvulnerableTime();
	}

	@Override
	public void resetInvulnerableTime() {
		entity.invulnerableTime = 0;
	}

	@Override
	public int invulnerableTime() {
		return entity.invulnerableTime;
	}

	@Override
	public void setInvulnerableTime(final int ticks) {
		entity.invulnerableTime = ticks;
	}

	@Override
	public void setHealth(final float health) {
		entity.setHealth(health);
	}

	@Override
	public void setHurtMark(final MyDamageSource source) {
		if (!(source instanceof ForgeDamageSource forgeSource))
			throw new IllegalArgumentException("Expected ForgeDamageSource");

		final DamageSource damageSource = forgeSource.nativeSource();

		if (entity.getHealth() <= 0.0F) {
			entity.die(damageSource);
			return;
		}

		if (!entity.isAlive()) {
			return;
		}

		entity.hurtDuration = 10;
		entity.hurtTime = 10;
		entity.hurtMarked = true;
		entity.gameEvent(GameEvent.ENTITY_DAMAGE);
		entity.playSound(SoundEvents.PLAYER_HURT, 1.0F, entity.getVoicePitch());
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
	public void heal(final float amount) {
		entity.heal(amount);
	}

	@Override
	public void feed(final int nutrition, final float saturation) {
		if (entity instanceof Player player)
			player.getFoodData().eat(nutrition, saturation);
	}

	@Override
	public void mendEquipment(final int level) {
		Utils.metalMending(entity, level);
	}

	@Override
	public double x() {
		return entity.getX();
	}

	@Override
	public double y() {
		return entity.getY();
	}

	@Override
	public double z() {
		return entity.getZ();
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
	public boolean isServerPlayer() {
		if (entity instanceof ServerPlayer) {
			return true;
		}
		return false;
	}


	@Override
	public boolean is(final MyLivingEntity other) {
		return other instanceof ForgeLivingEntity forge &&
				entity.is(forge.entity);
	}

	@Override
	public boolean isAllied(final MyLivingEntity other) {
		if (!(other instanceof ForgeLivingEntity forge))
			return false;

		return Utils.isAllied(entity, forge.entity);
	}

	@Override
	public boolean isHostileTargeted(final MyLivingEntity other) {
		if (!(other instanceof ForgeLivingEntity forge))
			return false;

		return Utils.hostileTargeted(entity, forge.entity);
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
				.map(ForgeLivingEntity::new)
				.map(MyLivingEntity.class::cast)
				.toList();
	}

	@Override
	public UUID uuid() {
		return entity.getUUID();
	}

	@Override
	public String typeId() {
		final ResourceLocation id =
				ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

		return id == null ? "" : id.toString();
	}

	@Override
	public int serverTick() {
		final var server = entity.getServer();
		return server == null ? -1 : server.getTickCount();
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
					final MobEffect effect = instance.getEffect();

					final ResourceLocation id =
							ForgeRegistries.MOB_EFFECTS.getKey(effect);

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
		final MobEffect effect =
				ForgeRegistries.MOB_EFFECTS.getValue(
						new ResourceLocation("minecraft", id)
				);

		if (effect != null)
			entity.removeEffect(effect);
	}

	@Override
	public void addEffect(
			final String id,
			final int duration,
			final int amplifier,
			final boolean ambient,
			final boolean visible) {

		final MobEffect effect =
				ForgeRegistries.MOB_EFFECTS.getValue(
						new ResourceLocation("minecraft", id)
				);

		if (effect == null)
			return;

		entity.addEffect(
				new MobEffectInstance(
						effect,
						duration,
						amplifier,
						ambient,
						visible
				)
		);
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
								new ResourceLocation("minecraft", "generic")
						)
				);

		return new DamageSource(baseHolder, attacker) {
			@Override
			public boolean is(final TagKey<DamageType> tag) {
				if (tag == DamageTypeTags.BYPASSES_COOLDOWN)
					return true;

				return super.is(tag);
			}

			@Override
			public String toString() {
				return "DamageSource (vitalrelics.extra_damage)";
			}
		};
	}


	@Override
	public double height() {
		return entity.getBbHeight();
	}

	@Override
	public void moveTo(final double x, final double y, final double z) {
		entity.setPos(x, y, z);
	}


	@Override
	public List<MyEntity> entitiesInRange(final double radius) {
		return entity.level().getEntities(entity, entity.getBoundingBox().inflate(radius)).stream()
				.map(ForgeEntity::new)
				.map(MyEntity.class::cast)
				.toList();
	}

	@Override
	public boolean isOnFire() {
		return entity.isOnFire();
	}

	@Override
	public void clearFire() {
		entity.clearFire();
	}
}
