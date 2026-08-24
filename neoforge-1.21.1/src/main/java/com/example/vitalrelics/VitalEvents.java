package com.example.vitalrelics;

import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import com.example.vitalrelics.common.scheduled.Scheduler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.example.vitalrelics.Utils.*;
import static com.example.vitalrelics.VitalRelics.MODID;

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

	private record PropertyTarget(
			Attribute attribute,
			ResourceLocation addId,
			ResourceLocation mulBaseId,
			ResourceLocation mulTotalId) {}

	@FunctionalInterface
	private interface TickAction {
		void apply(LivingEntity entity, Relic.Ticks.Info value);
	}

	private static final Map<String, PropertyTarget> PROPERTY_TARGETS = Map.of(
			"attack_damage", new PropertyTarget(Attributes.ATTACK_DAMAGE,
					ATTACK_DAMAGE_ADD_ID, ATTACK_DAMAGE_MUL_ID, ATTACK_DAMAGE_MUL_TOTAL_ID),
			"attack_speed", new PropertyTarget(Attributes.ATTACK_SPEED,
					ATTACK_SPEED_ADD_ID, ATTACK_SPEED_MUL_ID, ATTACK_SPEED_MUL_TOTAL_ID),
			"knockback_resistance", new PropertyTarget(Attributes.KNOCKBACK_RESISTANCE,
					KNOCKBACK_RESISTANCE_ADD_ID, KNOCKBACK_RESISTANCE_MUL_ID, KNOCKBACK_RESISTANCE_MUL_TOTAL_ID),
			"max_health", new PropertyTarget(Attributes.MAX_HEALTH,
					MAX_HEALTH_ADD_ID, MAX_HEALTH_MUL_ID, MAX_HEALTH_MUL_TOTAL_ID),
			"block_interaction_range", new PropertyTarget(Attributes.BLOCK_INTERACTION_RANGE,
					BLOCK_INTERACTION_RANGE_ADD_ID, BLOCK_INTERACTION_RANGE_MUL_ID, BLOCK_INTERACTION_RANGE_MUL_TOTAL_ID),
			"entity_interaction_range", new PropertyTarget(Attributes.ENTITY_INTERACTION_RANGE,
					ENTITY_INTERACTION_RANGE_ADD_ID, ENTITY_INTERACTION_RANGE_MUL_ID, ENTITY_INTERACTION_RANGE_MUL_TOTAL_ID)
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
	public static void onServerTick(final ServerTickEvent.Post event) {
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
	public static void onEntityTick(final EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof LivingEntity livingEntity) || livingEntity.level().isClientSide())
			return;

		final MinecraftServer server = livingEntity.getServer();
		if (server == null) {
			return;
		}

		final var relics = gatherRelics(livingEntity);
		final int tick = server.getTickCount();

		if (tick % 10 == 0) {
			removeImmuneEffects(livingEntity, relics);
			applyRelicEffects(livingEntity, relics);

			if (livingEntity instanceof ServerPlayer player) {
				int flight_level = RelicLoader.levelOfSuchPassiveAbility(
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


		if (tick % 20 == 0) {
			final int reality_severance_level = RelicLoader.levelOfSuchPassiveAbility(
					relics, "reality_severance"
			);
			if (reality_severance_level > 0) {
				final float ratioDamage = reality_severance_level / 100.0f;
				final float rangeDamage = ratioDamage * (float)livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE);
				MyDamageInfo.directRangedAttack(livingEntity, rangeDamage, reality_severance_level, 1, Math.round(reality_severance_level / 4.0f));
			}
		}

		if (tick % 80 == 0) {
			final int metalMendingLevel = RelicLoader.levelOfSuchPassiveAbility(
					relics, "metal_mending"
			);

			if (metalMendingLevel > 0) {
				Utils.metalMending(livingEntity, metalMendingLevel);
			}
		}

		final Map<String, Relic.Ticks.Info> ticks =
				RelicLoader.computeTicks(relics, tick);

		for (final var entry : ticks.entrySet()) {
			final TickAction action = TICK_ACTIONS.get(entry.getKey());
			if (action != null)
				action.apply(livingEntity, entry.getValue());
		}

		final Map<String, Relic.Properties.Info> properties =
				RelicLoader.computeProperties(relics);

		for (final var entry : properties.entrySet()) {
			final PropertyTarget target = PROPERTY_TARGETS.get(entry.getKey());
			if (target != null) {
				applyProperty(livingEntity.getAttribute(target.attribute()), entry.getValue(),
						target.addId(), target.mulBaseId(), target.mulTotalId());
			}
		}

		if (livingEntity.getHealth() > livingEntity.getMaxHealth())
			livingEntity.setHealth(livingEntity.getMaxHealth());
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

		if (victim.level().isClientSide()) {
			return;
		}

		MinecraftServer victim_server = victim.getServer();
		if (victim_server == null) {
			return;
		}

		// Attack
		if (entity_criminal instanceof LivingEntity criminal) {
			final var attackerRelics = gatherRelics(criminal);
			final float amount = (float) RelicLoader.applyCallback(
					attackerRelics,
					"damage_dealt",
					event.getNewDamage(),
					victim.getMaxHealth()
			);
			final double invulnerableTime = RelicLoader.applyCallback(
					attackerRelics,
					"invulnerable_time_dealt",
					victim.invulnerableTime,
					10.0
			);

			victim.invulnerableTime = Math.round((float) invulnerableTime);
			event.setNewDamage(amount);
		}

		// Protection
		final var victimRelics = gatherRelics(victim);
		float amount = (float) RelicLoader.applyCallback(
				victimRelics,
				"damage_taken",
				event.getNewDamage(),
				victim.getMaxHealth()
		);
		final float invulnerable_time = (float) RelicLoader.applyCallback(
				victimRelics,
				"invulnerable_time_taken",
				victim.invulnerableTime,
				10.0
		);

		if (victim.invulnerableTime != invulnerable_time) {
			final UUID victimUUID = victim.getUUID();
			if (Scheduler.INSTANCE().PROTECTED_PLAYER_LIST.getOrDefault(victimUUID, 0) == 0) {
				Scheduler.INSTANCE().PROTECTED_PLAYER_LIST.put(
						victimUUID,
						victim_server.getTickCount(),
						Math.round(invulnerable_time)
				);
			} else {
				amount = 0.0F;
			}
		}

		victim.invulnerableTime = Math.round(invulnerable_time);
		event.setNewDamage(amount);

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
				final int sp_level = RelicLoader.levelOfSuchPassiveAbility(relics, "retarget_arrow");
				if (sp_level > 0) {
					retargetArrow(arrow, victim, sp_level);
					event.setCanceled(true);
				}
			}
		}
	}
}
