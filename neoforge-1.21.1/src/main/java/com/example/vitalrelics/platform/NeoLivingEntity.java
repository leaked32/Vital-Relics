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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;
import java.util.Objects;

public final class NeoLivingEntity extends NeoForgeEntity implements MyLivingEntity {
	private final LivingEntity livingEntity;

	public NeoLivingEntity(final LivingEntity entity) {
		super(entity);
		this.livingEntity = entity;
	}

	@Override
	public LivingEntity nativeEntity() {
		return livingEntity;
	}

	@Override
	public void teleport(final double x, final double y, final double z) {
		if (!(livingEntity.level() instanceof ServerLevel level))
			return;

		if (livingEntity instanceof ServerPlayer player) {
			player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
		} else {
			livingEntity.teleportTo(x, y, z);
		}
	}

	@Override
	public void playSound(final MySound sound) {
		if (!(livingEntity.level() instanceof ServerLevel level))
			return;

		final var event = switch (sound) {
			case TELEPORT -> SoundEvents.ENDERMAN_TELEPORT;
			case ILLUSIONER_CAST -> SoundEvents.ILLUSIONER_CAST_SPELL;
			case PLAYER_LEVELUP -> SoundEvents.PLAYER_LEVELUP;
			case AMETHYST_CHIME -> SoundEvents.AMETHYST_BLOCK_CHIME;
			case DRAGON_FLAP -> SoundEvents.ENDER_DRAGON_FLAP;
			case EVOKER_CAST -> SoundEvents.EVOKER_CAST_SPELL;
			case GENERIC_EXPLODE -> SoundEvents.GENERIC_EXPLODE;
			case BEACON_ACTIVATE -> SoundEvents.BEACON_ACTIVATE;
			case ENCHANTMENT_TABLE_USE -> SoundEvents.ENCHANTMENT_TABLE_USE;
		};
		level.playSound(null, x(), y(), z(), event, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	@Override
	public MyDamageSource extraDamageSource() {
		return new NeoDamageSource(_extraDamageSource(livingEntity));
	}

	@Override
	public boolean hurt(final MyDamageSource source, final float amount) {
		if (!(source instanceof NeoDamageSource neoSource))
			throw new IllegalArgumentException("Expected NeoDamageSource");
		return livingEntity.hurt(neoSource.nativeSource(), amount);
	}

	@Override
	public boolean hurtThorns(final MyLivingEntity source, final float amount) {
		if (!(source instanceof NeoLivingEntity neoSource))
			throw new IllegalArgumentException("Expected NeoLivingEntity source");
		return livingEntity.hurt(livingEntity.damageSources().thorns(neoSource.livingEntity), amount);
	}

	@Override public void resetInvulnerableTime() { livingEntity.invulnerableTime = 0; }
	@Override public int invulnerableTime() { return livingEntity.invulnerableTime; }
	@Override public void setInvulnerableTime(final int ticks) { livingEntity.invulnerableTime = ticks; }
	@Override public void setHealth(final float health) { livingEntity.setHealth(health); }

	@Override
	public void setHurtMark(final MyDamageSource source) {
		if (!(source instanceof NeoDamageSource neoSource))
			throw new IllegalArgumentException("Expected NeoDamageSource");
		final DamageSource damageSource = neoSource.nativeSource();
		if (livingEntity.getHealth() <= 0.0F) {
			livingEntity.die(damageSource);
			return;
		}
		if (!livingEntity.isAlive())
			return;
		livingEntity.hurtDuration = 10;
		livingEntity.hurtTime = 10;
		livingEntity.hurtMarked = true;
		livingEntity.gameEvent(GameEvent.ENTITY_DAMAGE);
		livingEntity.playSound(SoundEvents.PLAYER_HURT, 1.0F, livingEntity.getVoicePitch());
	}

	@Override public float health() { return livingEntity.getHealth(); }
	@Override public float maxHealth() { return livingEntity.getMaxHealth(); }
	@Override public float attackDamage() { return (float) livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE); }
	@Override public void push(final double x, final double y, final double z) { livingEntity.push(x, y, z); }
	@Override public void markMovementChanged() { livingEntity.hurtMarked = true; }
	@Override public boolean isDeadOrDying() { return livingEntity.isDeadOrDying(); }
	@Override public boolean isServerPlayer() { return livingEntity instanceof ServerPlayer; }

	@Override
	public boolean is(final MyLivingEntity other) {
		return other instanceof NeoLivingEntity neo && livingEntity.is(neo.livingEntity);
	}

	@Override
	public boolean isAllied(final MyLivingEntity other) {
		return other instanceof NeoLivingEntity neo && Utils.isAllied(livingEntity, neo.livingEntity);
	}

	@Override
	public boolean isHostileTargeted(final MyLivingEntity other) {
		return other instanceof NeoLivingEntity neo && Utils.hostileTargeted(livingEntity, neo.livingEntity);
	}

	@Override
	public List<MyLivingEntity> livingEntitiesInRange(final double radius) {
		final var box = livingEntity.getBoundingBox().inflate(radius, radius, radius);
		return livingEntity.level().getEntitiesOfClass(LivingEntity.class, box,
				target -> target != livingEntity && target.isAlive()).stream()
				.map(NeoLivingEntity::new).map(MyLivingEntity.class::cast).toList();
	}

	@Override
	public void addEffect(final MyEffect effect, final int duration, final int amplifier) {
		final var nativeEffect = switch (effect) {
			case MOVEMENT_SLOWDOWN -> MobEffects.MOVEMENT_SLOWDOWN;
			case WITHER -> MobEffects.WITHER;
			case DARKNESS -> MobEffects.DARKNESS;
			case ABSORPTION -> MobEffects.ABSORPTION;
		};
		_addEffect(livingEntity, nativeEffect, duration, amplifier);
	}

	@Override
	public String typeId() {
		return BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType()).toString();
	}

	@Override
	public int serverTick() {
		final var server = livingEntity.getServer();
		return server == null ? -1 : server.getTickCount();
	}

	@Override public void heal(final float amount) { livingEntity.heal(amount); }
	@Override public void feed(final int nutrition, final float saturation) {
		if (livingEntity instanceof Player player) player.getFoodData().eat(nutrition, saturation);
	}
	@Override public void mendEquipment(final int level) { Utils.metalMending(livingEntity, level); }
	@Override public double horizontalLookX() { return livingEntity.getLookAngle().x; }
	@Override public double horizontalLookZ() { return livingEntity.getLookAngle().z; }
	@Override public double width() { return livingEntity.getBbWidth(); }

	@Override
	public List<MyEffectInstance> activeEffects() {
		return livingEntity.getActiveEffects().stream().map(instance -> {
			final var effect = instance.getEffect().value();
			final ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
			if (id == null) return null;
			final MyEffectCategory category = switch (effect.getCategory()) {
				case BENEFICIAL -> MyEffectCategory.POSITIVE;
				case HARMFUL -> MyEffectCategory.NEGATIVE;
				case NEUTRAL -> MyEffectCategory.NEUTRAL;
			};
			return new MyEffectInstance(id.getPath(), category);
		}).filter(Objects::nonNull).toList();
	}

	@Override
	public void removeEffect(final String id) {
		final ResourceLocation resource = ResourceLocation.fromNamespaceAndPath("minecraft", id);
		final var effect = BuiltInRegistries.MOB_EFFECT.get(resource);
		if (effect != null) livingEntity.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
	}

	@Override
	public void addEffect(final String id, final int duration, final int amplifier,
			final boolean ambient, final boolean visible) {
		final ResourceLocation resource = ResourceLocation.fromNamespaceAndPath("minecraft", id);
		final var effect = BuiltInRegistries.MOB_EFFECT.get(resource);
		if (effect == null) return;
		livingEntity.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
				duration, amplifier, ambient, visible));
	}

	public static void _addEffect(final LivingEntity target, final Holder<MobEffect> effect,
			final int duration, final int amplifier) {
		if (target.isDeadOrDying() || !target.level().isLoaded(target.blockPosition())) return;
		target.forceAddEffect(new MobEffectInstance(effect, duration, amplifier, true, true), null);
	}

	private DamageSource _extraDamageSource(final LivingEntity attacker) {
		final HolderLookup.RegistryLookup<DamageType> damageTypeLookup = livingEntity.level().registryAccess()
				.lookupOrThrow(Registries.DAMAGE_TYPE);
		final Holder<DamageType> baseHolder = damageTypeLookup.getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,
				ResourceLocation.fromNamespaceAndPath("minecraft", "generic")));
		return new DamageSource(baseHolder, attacker) {
			@Override
			public boolean is(final TagKey<DamageType> tag) {
				if (tag == DamageTypeTags.BYPASSES_COOLDOWN) return true;
				return super.is(tag);
			}
		};
	}

	@Override public boolean isOnFire() { return livingEntity.isOnFire(); }
	@Override public void clearFire() { livingEntity.clearFire(); }
}
