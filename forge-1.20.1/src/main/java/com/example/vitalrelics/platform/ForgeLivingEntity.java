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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ForgeLivingEntity extends ForgeEntity implements MyLivingEntity {
	final LivingEntity livingEntity;

	public ForgeLivingEntity(final LivingEntity livingEntity) {
		super(livingEntity);
		this.livingEntity = livingEntity;
	}

	public LivingEntity nativeEntity() {
		return livingEntity;
	}

	@Override
	public void teleport(
			final double x,
			final double y,
			final double z) {

		if (!(livingEntity.level() instanceof ServerLevel level))
			return;

		if (livingEntity instanceof ServerPlayer player) {
			player.teleportTo(
					level,
					x, y, z,
					player.getYRot(), player.getXRot()
			);
		} else {
			livingEntity.teleportTo(x, y, z);
		}
	}

	@Override
	public void playSound(final MySound sound) {
		if (!(livingEntity.level() instanceof ServerLevel level))
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
				livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
				nativeSound,
				SoundSource.PLAYERS,
				volume,
				pitch
		);
	}

	@Override
	public MyDamageSource extraDamageSource() {
		return new ForgeDamageSource(_extraDamageSource(livingEntity));
	}

	@Override
	public boolean hurt(
			final MyDamageSource source,
			final float amount) {

		if (!(source instanceof ForgeDamageSource forgeSource))
			throw new IllegalArgumentException("Expected ForgeDamageSource");

		return livingEntity.hurt(forgeSource.nativeSource(), amount);
	}

	@Override
	public boolean hurtThorns(final MyLivingEntity source, final float amount) {
		if (!(source instanceof ForgeLivingEntity forgeSource))
			throw new IllegalArgumentException("Expected ForgeLivingEntity source");

		return livingEntity.hurt(livingEntity.damageSources().thorns(forgeSource.livingEntity), amount);
	}

	@Override
	public void resetInvulnerable() {
		if (livingEntity.isInvulnerable()) {
			livingEntity.setInvulnerable(false);
		}
		resetInvulnerableTime();
	}

	@Override
	public void resetInvulnerableTime() {
		livingEntity.invulnerableTime = 0;
	}

	@Override
	public int invulnerableTime() {
		return livingEntity.invulnerableTime;
	}

	@Override
	public void setInvulnerableTime(final int ticks) {
		livingEntity.invulnerableTime = ticks;
	}

	@Override
	public void setHealth(final float health) {
		livingEntity.setHealth(health);
	}

	@Override
	public void setHurtMark(final MyDamageSource source) {
		if (!(source instanceof ForgeDamageSource forgeSource))
			throw new IllegalArgumentException("Expected ForgeDamageSource");

		final DamageSource damageSource = forgeSource.nativeSource();

		if (livingEntity.getHealth() <= 0.0F) {
			livingEntity.die(damageSource);
			return;
		}

		if (!livingEntity.isAlive()) {
			return;
		}

		livingEntity.hurtDuration = 10;
		livingEntity.hurtTime = 10;
		livingEntity.hurtMarked = true;
		livingEntity.gameEvent(GameEvent.ENTITY_DAMAGE);
		livingEntity.playSound(SoundEvents.PLAYER_HURT, 1.0F, livingEntity.getVoicePitch());
	}

	@Override
	public float health() {
		return livingEntity.getHealth();
	}

	@Override
	public float maxHealth() {
		return livingEntity.getMaxHealth();
	}

	@Override
	public float attackDamage() {
		return (float) livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE);
	}

	@Override
	public void heal(final float amount) {
		livingEntity.heal(amount);
	}

	@Override
	public void feed(final int nutrition, final float saturation) {
		if (livingEntity instanceof Player player)
			player.getFoodData().eat(nutrition, saturation);
	}

	@Override
	public void mendEquipment(final int level) {
		Utils.metalMending(livingEntity, level);
	}

	@Override
	public double x() {
		return livingEntity.getX();
	}

	@Override
	public double y() {
		return livingEntity.getY();
	}

	@Override
	public double z() {
		return livingEntity.getZ();
	}

	@Override
	public double horizontalLookX() {
		return livingEntity.getLookAngle().x;
	}

	@Override
	public double horizontalLookZ() {
		return livingEntity.getLookAngle().z;
	}

	@Override
	public void setVelocity(
			final double x,
			final double y,
			final double z) {

		livingEntity.setDeltaMovement(x, y, z);
	}

	@Override
	public void push(
			final double x,
			final double y,
			final double z) {

		livingEntity.push(x, y, z);
	}

	@Override
	public void markMovementChanged() {
		livingEntity.hurtMarked = true;
	}

	@Override
	public boolean isDeadOrDying() {
		return livingEntity.isDeadOrDying();
	}

	@Override
	public boolean isLoaded() {
		return livingEntity.level().isLoaded(livingEntity.blockPosition());
	}

	@Override
	public boolean isClientSide() {
		return livingEntity.level().isClientSide();
	}


	@Override
	public boolean isServerPlayer() {
		if (livingEntity instanceof ServerPlayer) {
			return true;
		}
		return false;
	}


	@Override
	public boolean is(final MyLivingEntity other) {
		return other instanceof ForgeLivingEntity forge &&
				livingEntity.is(forge.livingEntity);
	}

	@Override
	public boolean isAllied(final MyLivingEntity other) {
		if (!(other instanceof ForgeLivingEntity forge))
			return false;

		return Utils.isAllied(livingEntity, forge.livingEntity);
	}

	@Override
	public boolean isHostile(final MyLivingEntity other) {
		if (!(other instanceof ForgeLivingEntity forge))
			return false;

		return Utils.isHostile(livingEntity, forge.livingEntity);
	}

	@Override
	public List<MyLivingEntity> livingEntitiesInRange(final double radius) {
		final var box =
				livingEntity.getBoundingBox().inflate(radius, radius, radius);

		return livingEntity.level()
				.getEntitiesOfClass(
						LivingEntity.class,
						box,
						target -> target != livingEntity && target.isAlive()
				)
				.stream()
				.map(ForgeLivingEntity::new)
				.map(MyLivingEntity.class::cast)
				.toList();
	}

	@Override
	public void resetTarget() {
		if (livingEntity instanceof Mob mob) {
			Utils.clearTarget(mob);
		}
	}


	@Override
	public MyLivingEntity getTarget() {
		if (livingEntity instanceof Mob mob) {
			if (mob.getTarget() != null) {
				return new ForgeLivingEntity(mob.getTarget());
			}
			else {
				return null;
			}
		}
		return null;
	}


	@Override
	public UUID uuid() {
		return livingEntity.getUUID();
	}

	@Override
	public String typeId() {
		final ResourceLocation id =
				ForgeRegistries.ENTITY_TYPES.getKey(livingEntity.getType());

		return id == null ? "" : id.toString();
	}

	@Override
	public int serverTick() {
		final var server = livingEntity.getServer();
		return server == null ? -1 : server.getTickCount();
	}

	@Override
	public double width() {
		return livingEntity.getBbWidth();
	}

	@Override
	public List<MyEffectInstance> activeEffects() {
		return livingEntity.getActiveEffects()
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
			livingEntity.removeEffect(effect);
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

		livingEntity.addEffect(
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
				livingEntity.level()
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
		return livingEntity.getBbHeight();
	}

	@Override
	public void moveTo(final double x, final double y, final double z) {
		livingEntity.setPos(x, y, z);
	}


	@Override
	public List<MyEntity> entitiesInRange(final double radius) {
		return livingEntity.level().getEntities(livingEntity, livingEntity.getBoundingBox().inflate(radius)).stream()
				.map(ForgeEntity::new)
				.map(MyEntity.class::cast)
				.toList();
	}

	@Override
	public boolean isOnFire() {
		return livingEntity.isOnFire();
	}

	@Override
	public void clearFire() {
		livingEntity.clearFire();
	}

	@Override
	public void igniteForSeconds(final int seconds) {
		livingEntity.setSecondsOnFire(seconds);
	}
}
