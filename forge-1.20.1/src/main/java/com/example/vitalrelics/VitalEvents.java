package com.example.vitalrelics;

import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import com.example.vitalrelics.common.Scheduler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.example.vitalrelics.Utils.*;

public final class VitalEvents {
	private static UUID modifierId(final String name) {
		return UUID.nameUUIDFromBytes((Manifest.MODID + ":" + name).getBytes(StandardCharsets.UTF_8));
	}

	private static final UUID ATTACK_DAMAGE_ADD_ID = modifierId("attack_damage_add");
	private static final UUID ATTACK_DAMAGE_MUL_ID = modifierId("attack_damage_mul_base");
	private static final UUID ATTACK_DAMAGE_MUL_TOTAL_ID = modifierId("attack_damage_mul_total");

	private static final UUID ATTACK_SPEED_ADD_ID = modifierId("attack_speed_add");
	private static final UUID ATTACK_SPEED_MUL_ID = modifierId("attack_speed_mul_base");
	private static final UUID ATTACK_SPEED_MUL_TOTAL_ID = modifierId("attack_speed_mul_total");

	private static final UUID ARMOR_ADD_ID = modifierId("armor_add");
	private static final UUID ARMOR_MUL_ID = modifierId("armor_mul_base");
	private static final UUID ARMOR_MUL_TOTAL_ID = modifierId("armor_mul_total");

	private static final UUID ARMOR_TOUGHNESS_ADD_ID = modifierId("armor_toughness_add");
	private static final UUID ARMOR_TOUGHNESS_MUL_ID = modifierId("armor_toughness_mul_base");
	private static final UUID ARMOR_TOUGHNESS_MUL_TOTAL_ID = modifierId("armor_toughness_mul_total");

	private static final UUID KNOCKBACK_RESISTANCE_ADD_ID = modifierId("knockback_resistance_add");
	private static final UUID KNOCKBACK_RESISTANCE_MUL_ID = modifierId("knockback_resistance_mul_base");
	private static final UUID KNOCKBACK_RESISTANCE_MUL_TOTAL_ID = modifierId("knockback_resistance_mul_total");

	private static final UUID MAX_HEALTH_ADD_ID = modifierId("max_health_add");
	private static final UUID MAX_HEALTH_MUL_ID = modifierId("max_health_mul_base");
	private static final UUID MAX_HEALTH_MUL_TOTAL_ID = modifierId("max_health_mul_total");

	private static final UUID BLOCK_REACH_ADD_ID = modifierId("block_interaction_range_add");
	private static final UUID BLOCK_REACH_MUL_ID = modifierId("block_interaction_range_mul_base");
	private static final UUID BLOCK_REACH_MUL_TOTAL_ID = modifierId("block_interaction_range_mul_total");

	private static final UUID ENTITY_REACH_ADD_ID = modifierId("entity_interaction_range_add");
	private static final UUID ENTITY_REACH_MUL_ID = modifierId("entity_interaction_range_mul_base");
	private static final UUID ENTITY_REACH_MUL_TOTAL_ID = modifierId("entity_interaction_range_mul_total");

	private record PropertyTarget(
			Attribute attribute,
			UUID addId,
			UUID mulBaseId,
			UUID mulTotalId) {}

	@FunctionalInterface
	private interface TickAction {
		void apply(LivingEntity entity, Relic.Ticks.Info value);
	}

	private static final Map<String, PropertyTarget> PROPERTY_TARGETS = Map.of(
			"attack_damage", new PropertyTarget(Attributes.ATTACK_DAMAGE,
					ATTACK_DAMAGE_ADD_ID, ATTACK_DAMAGE_MUL_ID, ATTACK_DAMAGE_MUL_TOTAL_ID),
			"attack_speed", new PropertyTarget(Attributes.ATTACK_SPEED,
					ATTACK_SPEED_ADD_ID, ATTACK_SPEED_MUL_ID, ATTACK_SPEED_MUL_TOTAL_ID),
			"armor", new PropertyTarget(Attributes.ARMOR,
					ARMOR_ADD_ID, ARMOR_MUL_ID, ARMOR_MUL_TOTAL_ID),
			"armor_toughness", new PropertyTarget(Attributes.ARMOR_TOUGHNESS,
					ARMOR_TOUGHNESS_ADD_ID, ARMOR_TOUGHNESS_MUL_ID, ARMOR_TOUGHNESS_MUL_TOTAL_ID),
			"knockback_resistance", new PropertyTarget(Attributes.KNOCKBACK_RESISTANCE,
					KNOCKBACK_RESISTANCE_ADD_ID, KNOCKBACK_RESISTANCE_MUL_ID, KNOCKBACK_RESISTANCE_MUL_TOTAL_ID),
			"max_health", new PropertyTarget(Attributes.MAX_HEALTH,
					MAX_HEALTH_ADD_ID, MAX_HEALTH_MUL_ID, MAX_HEALTH_MUL_TOTAL_ID),
			"block_interaction_range", new PropertyTarget(ForgeMod.BLOCK_REACH.get(),
					BLOCK_REACH_ADD_ID, BLOCK_REACH_MUL_ID, BLOCK_REACH_MUL_TOTAL_ID),
			"entity_interaction_range", new PropertyTarget(ForgeMod.ENTITY_REACH.get(),
					ENTITY_REACH_ADD_ID, ENTITY_REACH_MUL_ID, ENTITY_REACH_MUL_TOTAL_ID)
	);

	private static final Map<String, TickAction> TICK_ACTIONS = Map.of(
			"heal", (entity, value) -> entity.heal((float) (
					value.add + entity.getMaxHealth() * value.ratio_add)),
			"feed", (entity, value) -> {
				if (entity instanceof Player player) {
					final float feed = (float) (
							value.add + player.getMaxHealth() * value.ratio_add);
					player.getFoodData().eat(Math.round(feed), 1.0F);
				}
			}
	);

	private VitalEvents() {}

	@SubscribeEvent
	public static void onServerTick(final TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;

		final MinecraftServer server = event.getServer();
		final int currentTickCount = server.getTickCount();

		final Function<UUID, Boolean> isEntityValid = uuid -> {
			for (final ServerLevel level : server.getAllLevels()) {
				final Entity entity = level.getEntity(uuid);

				if (entity instanceof LivingEntity livingEntity &&
						!livingEntity.isRemoved() &&
						!livingEntity.isDeadOrDying()) {
					return true;
				}
			}

			return false;
		};

		Scheduler.INSTANCE().serverTick(currentTickCount, isEntityValid);
	}

	@SubscribeEvent
	public static void onLivingTick(final LivingEvent.LivingTickEvent event) {
		final LivingEntity livingEntity = event.getEntity();

		if (livingEntity.level().isClientSide()) {
			return;
		}

		final List<Relic> relics = gatherRelics(livingEntity);
		final int tick = livingEntity.getServer().getTickCount();

		final Map<String, Relic.Ticks.Info> ticks =
				RelicLoader.computeTicks(relics, tick);

		for (final var entry : ticks.entrySet()) {
			final TickAction action = TICK_ACTIONS.get(entry.getKey());
			if (action != null)
				action.apply(livingEntity, entry.getValue());
		}


		if (tick % 10 == 0) {
			removeImmuneEffects(livingEntity, relics);
			applyRelicEffects(livingEntity, relics);
		}

		if (tick % 20 == 0) {
			// Properties

			final Map<String, Relic.Properties.Info> properties =
					RelicLoader.computeProperties(relics);

			for (final var entry : PROPERTY_TARGETS.entrySet()) {
				final PropertyTarget target = entry.getValue();

				final Relic.Properties.Info property =
						properties.getOrDefault(
								entry.getKey(),
								Relic.Properties.Info.basic()
						);

				applyProperty(
						livingEntity.getAttribute(target.attribute()),
						property,
						target.addId(),
						target.mulBaseId(),
						target.mulTotalId()
				);
			}

			// Passive Skill: Flight

			if (livingEntity instanceof ServerPlayer player) {
				final double flight_level = RelicLoader.levelOfSuchPassiveSkill(
						relics, Relic.PASSIVE_SKILL_FLIGHT
				);
				if (flight_level > 0) {
					if (!player.getAbilities().mayfly) {
						player.getAbilities().mayfly = true;
						player.getAbilities().setFlyingSpeed((float) (0.05 * flight_level));
						player.onUpdateAbilities();
					}
				} else {
					GameType gameType = player.gameMode.getGameModeForPlayer();
					if (gameType != GameType.CREATIVE) {
						player.getAbilities().mayfly = false;
						player.getAbilities().flying = false;
						player.getAbilities().setFlyingSpeed(0.05F);
						player.onUpdateAbilities();
					}
				}
			}


			// Passive Skill: reality_severance

			final double reality_severance_level =
					RelicLoader.levelOfSuchPassiveSkill(relics, Relic.PASSIVE_SKILL_REALITY_SEVERANCE);

			if (reality_severance_level > 0.0) {
				final float ratioDamage = (float) (reality_severance_level / 100.0);
				final float rangeDamage =
						ratioDamage *
								(float) livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE);

				MyDamageInfo.directRangedAttack(
						livingEntity,
						rangeDamage,
						Math.round((float) reality_severance_level),
						1,
						Math.round((float) (reality_severance_level / 4.0))
				);
			}
		}

		if (tick % 80 == 0) {
			final double metalMendingLevel =
					RelicLoader.levelOfSuchPassiveSkill(relics, Relic.PASSIVE_SKILL_METAL_MENDING);

			if (metalMendingLevel > 0.0) {
				Utils.metalMending(livingEntity, Math.max(1, (int) Math.round(metalMendingLevel)));
			}
		}

	}

	private static void applyProperty(
			final AttributeInstance attr,
			final Relic.Properties.Info prop,
			final UUID addId,
			final UUID mulBaseId,
			final UUID mulTotalId) {

		if (attr == null || prop == null)
			return;

		replaceModifier(attr, addId, prop.add, AttributeModifier.Operation.ADDITION);
		replaceModifier(attr, mulBaseId, prop.mul_base, AttributeModifier.Operation.MULTIPLY_BASE);
		replaceModifier(attr, mulTotalId, prop.mul_total - 1.0,
				AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	private static void replaceModifier(
			final AttributeInstance attr,
			final UUID id,
			final double amount,
			final AttributeModifier.Operation operation) {

		attr.removeModifier(id);

		if (amount != 0.0)
			attr.addTransientModifier(new AttributeModifier(id, Manifest.MODID, amount, operation));
	}


	@SubscribeEvent
	public static void onLivingDamage(final LivingDamageEvent event) {
		final LivingEntity victim = event.getEntity();
		final Entity entity_criminal = event.getSource().getEntity();

		if (victim.level().isClientSide()) {
			return;
		}

		MinecraftServer victim_server = victim.getServer();
		if (victim_server == null) {
			return;
		}

		float amount = event.getAmount();

		/*
		Attack
		 */
		if (entity_criminal instanceof LivingEntity criminal) {
			final List<Relic> attackerRelics = gatherRelics(criminal);
			amount = (float) RelicLoader.applyCallback(
					attackerRelics, "damage_dealt", amount, victim.getMaxHealth());

			final double invulnerableTime = RelicLoader.applyCallback(
					attackerRelics, "invulnerable_time_dealt", victim.invulnerableTime, 10.0);
			victim.invulnerableTime = Math.round((float) invulnerableTime);


			// Lifesteal
			final double lifestealLevel = RelicLoader.levelOfSuchPassiveSkill(
					attackerRelics,
					Relic.PASSIVE_SKILL_LIFESTEAL
			);

			if (lifestealLevel > 0.0 && amount > 0.0F) {
				criminal.heal((float) (amount * lifestealLevel));
			}
		}


		/*
		Protection
		 */

		final List<Relic> victimRelics = gatherRelics(victim);
		amount = (float) RelicLoader.applyCallback(
				victimRelics, "damage_taken", amount, victim.getMaxHealth());
		final float invulnerable_time = (float) RelicLoader.applyCallback(
				victimRelics, "invulnerable_time_taken", victim.invulnerableTime, 10.0);

		// Only activate it when changes happen.
		if (victim.invulnerableTime != invulnerable_time) {
			if (!Scheduler.INSTANCE().acquireProtection(
					victim.getUUID(),
					victim_server.getTickCount(),
					Math.round(invulnerable_time)
			)) {
				amount = 0.0F;
			}
			victim.invulnerableTime = Math.round(invulnerable_time);
		}

		// Thorns

		if (entity_criminal instanceof LivingEntity criminal && amount > 0.0F) {
			final double thornsLevel = RelicLoader.levelOfSuchPassiveSkill(
					victimRelics,
					Relic.PASSIVE_SKILL_THORNS
			);

			if (thornsLevel > 0.0 &&
					Scheduler.INSTANCE().acquireThorns(
							victim.getUUID(),
							victim_server.getTickCount(),
							10
					)) {

				criminal.hurt(
						victim.damageSources().thorns(victim),
						(float) (amount * thornsLevel)
				);
			}
		}

		event.setAmount(amount);
	}

	@SubscribeEvent
	public static void onEffectApplicable(final MobEffectEvent.Applicable event) {
		final LivingEntity entity = event.getEntity();

		if (entity.level().isClientSide())
			return;

		final MobEffect effect = event.getEffectInstance().getEffect();
		final ResourceLocation id =
				net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect);

		if (id == null)
			return;

		final boolean negative = effect.getCategory() == MobEffectCategory.HARMFUL;

		if (RelicLoader.isImmuneToEffect(gatherRelics(entity), id.getPath(), negative))
			event.setResult(Event.Result.DENY);
	}


	@SubscribeEvent
	public static void onArrowImpact(final ProjectileImpactEvent event) {
		if (!(event.getProjectile() instanceof AbstractArrow arrow) ||
				arrow.level().isClientSide() ||
				!(event.getRayTraceResult() instanceof EntityHitResult hit) ||
				!(hit.getEntity() instanceof LivingEntity victim))
			return;

		final double sp_level =
				RelicLoader.levelOfSuchPassiveSkill(
						gatherRelics(victim),
						Relic.PASSIVE_SKILL_RETARGET_ARROW
				);

		if (sp_level > 0.0) {
			retargetArrow(arrow, victim, sp_level);
			// existing event cancellation unchanged
		}
	}


	@SubscribeEvent
	public static void onArrowLoose(final ArrowLooseEvent event) {
		final Player player = event.getEntity();

		if (player.level().isClientSide()) {
			return;
		}

		final double level = RelicLoader.levelOfSuchPassiveSkill(
				gatherRelics(player),
				Relic.PASSIVE_SKILL_EMPOWERED_ARROW
		);

		if (level <= 0.0)
			return;

		event.setCharge(Math.round((float) (event.getCharge() * level)));
	}


	@SubscribeEvent
	public static void onArrowShot(final EntityJoinLevelEvent event) {
		if (!(event.getEntity() instanceof AbstractArrow arrow) ||
				event.getLevel().isClientSide()) {
			return;
		}

		if (!(arrow.getOwner() instanceof LivingEntity owner)) {
			return;
		}

		final double level = RelicLoader.levelOfSuchPassiveSkill(
				gatherRelics(owner),
				Relic.PASSIVE_SKILL_EMPOWERED_ARROW
		);

		if (level <= 0.0)
			return;

		arrow.setDeltaMovement(
				arrow.getDeltaMovement().scale(level)
		);

		final double leastDamage =
				level * owner.getAttributeValue(Attributes.ATTACK_DAMAGE);

		arrow.setBaseDamage(
				Math.max(arrow.getBaseDamage() * level, leastDamage)
		);
	}
}