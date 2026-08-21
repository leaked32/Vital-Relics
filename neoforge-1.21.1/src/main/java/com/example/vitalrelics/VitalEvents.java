package com.example.vitalrelics;

import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.ArrayList;
import java.util.List;

import static com.example.vitalrelics.Utils.*;
import static com.example.vitalrelics.VitalRelics.MODID;
import static com.example.vitalrelics.VitalRelics.LOGGER;
import static com.example.vitalrelics.VitalRelics.loader;

public final class VitalEvents {
	private static final ResourceLocation ATTACK_DAMAGE_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "attack_damage_add");

	private static final ResourceLocation ATTACK_DAMAGE_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "attack_damage_mul_base");

	private static final ResourceLocation ATTACK_DAMAGE_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "attack_damage_mul_total");

	private static final ResourceLocation ATTACK_SPEED_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "attack_speed_add");

	private static final ResourceLocation ATTACK_SPEED_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "attack_speed_mul_base");

	private static final ResourceLocation ATTACK_SPEED_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "attack_speed_mul_total");


	private static final ResourceLocation KNOCKBACK_RESISTANCE_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "knockback_resistance_add");

	private static final ResourceLocation KNOCKBACK_RESISTANCE_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "knockback_resistance_mul_base");

	private static final ResourceLocation KNOCKBACK_RESISTANCE_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "knockback_resistance_mul_total");

	private static final ResourceLocation MAX_HEALTH_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "max_health_add");

	private static final ResourceLocation MAX_HEALTH_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "max_health_mul_base");

	private static final ResourceLocation MAX_HEALTH_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "max_health_mul_total");

	private static final ResourceLocation BLOCK_INTERACTION_RANGE_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "block_interaction_range_add");

	private static final ResourceLocation BLOCK_INTERACTION_RANGE_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "block_interaction_range_mul_base");

	private static final ResourceLocation BLOCK_INTERACTION_RANGE_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "block_interaction_range_mul_total");

	private static final ResourceLocation ENTITY_INTERACTION_RANGE_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "entity_interaction_range_add");

	private static final ResourceLocation ENTITY_INTERACTION_RANGE_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "entity_interaction_range_mul_base");

	private static final ResourceLocation ENTITY_INTERACTION_RANGE_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(MODID, "entity_interaction_range_mul_total");

	private VitalEvents() {}


	@SubscribeEvent
	public static void onEntityTick(final EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide())
			return;

		final var relics = gatherRelics(entity);
		final int tick = entity.getServer().getTickCount();

		if (tick % 10 == 0) {
			removeImmuneEffects(entity, relics);
			applyRelicEffects(entity, relics);

			if (entity instanceof ServerPlayer player) {
				int flight_level = RelicLoader.hasSuchSpecialAbility(
						relics, "flight"
				);
				if (flight_level > 0) {
					if (!player.getAbilities().mayfly) {
						player.getAbilities().mayfly = true;
						player.onUpdateAbilities();
					}
				} else {
					GameType gameType = player.gameMode.getGameModeForPlayer();
					if (gameType != GameType.CREATIVE) {
						player.getAbilities().mayfly = false;
						player.getAbilities().flying = false;
						player.onUpdateAbilities();
					}
				}
			}

		}

		final var ticks = RelicLoader.computeTicks(relics, tick);

		entity.heal((float)(ticks.heal.add + entity.getMaxHealth() * ticks.heal.ratio_add));

		if (entity instanceof Player player) {
			final float feed = (float)(ticks.feed.add + player.getMaxHealth() * ticks.feed.ratio_add);
			player.getFoodData().eat(Math.round(feed), 1.0F);
		}

		final var prop = RelicLoader.computeProperties(relics);

		applyProperty(entity.getAttribute(Attributes.ATTACK_DAMAGE), prop.attack_damage,
				ATTACK_DAMAGE_ADD_ID, ATTACK_DAMAGE_MUL_ID, ATTACK_DAMAGE_MUL_TOTAL_ID);

		applyProperty(entity.getAttribute(Attributes.ATTACK_SPEED), prop.attack_speed,
				ATTACK_SPEED_ADD_ID, ATTACK_SPEED_MUL_ID, ATTACK_SPEED_MUL_TOTAL_ID);

		applyProperty(entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE), prop.knockback_resistance,
				KNOCKBACK_RESISTANCE_ADD_ID, KNOCKBACK_RESISTANCE_MUL_ID, KNOCKBACK_RESISTANCE_MUL_TOTAL_ID);

		applyProperty(entity.getAttribute(Attributes.MAX_HEALTH), prop.max_health,
				MAX_HEALTH_ADD_ID, MAX_HEALTH_MUL_ID, MAX_HEALTH_MUL_TOTAL_ID);

		applyProperty(entity.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), prop.block_interaction_range,
				BLOCK_INTERACTION_RANGE_ADD_ID, BLOCK_INTERACTION_RANGE_MUL_ID, BLOCK_INTERACTION_RANGE_MUL_TOTAL_ID);

		applyProperty(entity.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), prop.entity_interaction_range,
				ENTITY_INTERACTION_RANGE_ADD_ID, ENTITY_INTERACTION_RANGE_MUL_ID, ENTITY_INTERACTION_RANGE_MUL_TOTAL_ID);

		if (entity.getHealth() > entity.getMaxHealth())
			entity.setHealth(entity.getMaxHealth());
	}

	private static void applyProperty(
			final AttributeInstance attr,
			final Relic.Properties.Info prop,
			final ResourceLocation addId,
			final ResourceLocation mulBaseId,
			final ResourceLocation mulTotalId) {

		if (attr == null || prop == null)
			return;

		replaceModifier(attr, addId, prop.add, AttributeModifier.Operation.ADD_VALUE);
		replaceModifier(attr, mulBaseId, prop.mul_base, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		replaceModifier(attr, mulTotalId, prop.mul_total - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void replaceModifier(
			final AttributeInstance attr,
			final ResourceLocation id,
			final double amount,
			final AttributeModifier.Operation op) {

		attr.removeModifier(id);

		if (amount != 0.0)
			attr.addTransientModifier(new AttributeModifier(id, amount, op));
	}

	@SubscribeEvent
	public static void onLivingDamage(final LivingDamageEvent.Pre event) {
		final LivingEntity victim = event.getEntity();
		final Entity entity_criminal = event.getSource().getEntity();

		if (victim.level().isClientSide())
			return;

		if (entity_criminal instanceof LivingEntity criminal) {

			var relicList = gatherRelics(criminal);
			float amount = event.getNewDamage();
			float invulnerable_time = victim.invulnerableTime;

			for (final var relic : relicList) {
				if (relic.callbacks.damage_dealt != null) {
					amount = (float)relic.callbacks.damage_dealt.process(amount, victim.getMaxHealth());
				}
				if (relic.callbacks.invulnerable_time_dealt != null) {
					invulnerable_time = (float)relic.callbacks.invulnerable_time_dealt.process(invulnerable_time, 10.0);
				}
			}

			victim.invulnerableTime = Math.round(invulnerable_time);
			event.setNewDamage(amount);
		}


		if (victim instanceof LivingEntity) {
			var relicList = gatherRelics(victim);
			float amount = event.getNewDamage();
			float invulnerable_time = victim.invulnerableTime;

			for (final var relic : relicList) {
				if (relic.callbacks.damage_taken != null) {
					amount = (float)relic.callbacks.damage_taken.process(amount, victim.getMaxHealth());
				}
				if (relic.callbacks.invulnerable_time_taken != null) {
					invulnerable_time = (float)relic.callbacks.invulnerable_time_taken.process(invulnerable_time, 10.0);
				}
			}
			victim.invulnerableTime = Math.round(invulnerable_time);
			event.setNewDamage(amount);
		}

		// inspect DamageSource / attacker / victim relics here
	}

	@SubscribeEvent
	public static void onEffectApplicable(final MobEffectEvent.Applicable event) {
		LivingEntity livingEntity = event.getEntity();

		if (livingEntity.level().isClientSide())
			return;

		final var relics = gatherRelics(livingEntity);
		final var effect = event.getEffectInstance().getEffect().value();

		final ResourceLocation id =
				BuiltInRegistries.MOB_EFFECT.getKey(effect);

		if (id == null)
			return;

		final boolean negative =
				effect.getCategory() == MobEffectCategory.HARMFUL;

		if (RelicLoader.isImmuneToEffect(
				relics,
				id.getPath(),
				negative
		)) {
			event.setResult(
					MobEffectEvent.Applicable.Result.DO_NOT_APPLY
			);
		}
	}


	@SubscribeEvent
	public static void onArrowImpact(ProjectileImpactEvent event) {
		// Only care about arrows
		if (!(event.getEntity() instanceof AbstractArrow arrow)) {
			return;
		}

		if (arrow.level().isClientSide()) {
			return;
		}

		// Check if it's about to hit a LivingEntity
		if (event.getRayTraceResult().getType() == HitResult.Type.ENTITY) {
			EntityHitResult entityResult = (EntityHitResult) event.getRayTraceResult();

			// Protection
			if (entityResult.getEntity() instanceof LivingEntity victim) {

				final var relics = gatherRelics(victim);
				if (RelicLoader.hasSuchSpecialAbility(relics, "retarget_arrow") != 0) {
					retargetArrow(arrow, victim);
					event.setCanceled(true);
				}
			}
		}
	}
}