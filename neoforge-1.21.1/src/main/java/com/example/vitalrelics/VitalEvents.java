package com.example.vitalrelics;

import com.example.vitalrelics.common.*;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MyUtils;
import com.example.vitalrelics.platform.NeoLivingEntity;
import net.minecraft.core.Holder;
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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.example.vitalrelics.Utils.*;

public final class VitalEvents {
	private record FlightState(boolean grantedByVitalRelics, double level) {}

	private static final Map<UUID, FlightState> FLIGHT_STATES = new HashMap<>();

	/*
	Properties
	 */

	private static final ResourceLocation ATTACK_DAMAGE_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "attack_damage_add");

	private static final ResourceLocation ATTACK_DAMAGE_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "attack_damage_mul_base");

	private static final ResourceLocation ATTACK_DAMAGE_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "attack_damage_mul_total");

	private static final ResourceLocation ATTACK_SPEED_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "attack_speed_add");

	private static final ResourceLocation ATTACK_SPEED_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "attack_speed_mul_base");

	private static final ResourceLocation ATTACK_SPEED_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "attack_speed_mul_total");

	private static final ResourceLocation ARMOR_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "armor_add");

	private static final ResourceLocation ARMOR_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "armor_mul_base");

	private static final ResourceLocation ARMOR_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "armor_mul_total");

	private static final ResourceLocation ARMOR_TOUGHNESS_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "armor_toughness_add");

	private static final ResourceLocation ARMOR_TOUGHNESS_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "armor_toughness_mul_base");

	private static final ResourceLocation ARMOR_TOUGHNESS_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "armor_toughness_mul_total");

	private static final ResourceLocation KNOCKBACK_RESISTANCE_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "knockback_resistance_add");

	private static final ResourceLocation KNOCKBACK_RESISTANCE_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "knockback_resistance_mul_base");

	private static final ResourceLocation KNOCKBACK_RESISTANCE_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "knockback_resistance_mul_total");

	private static final ResourceLocation MAX_HEALTH_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "max_health_add");

	private static final ResourceLocation MAX_HEALTH_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "max_health_mul_base");

	private static final ResourceLocation MAX_HEALTH_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "max_health_mul_total");

	private static final ResourceLocation BLOCK_INTERACTION_RANGE_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "block_interaction_range_add");

	private static final ResourceLocation BLOCK_INTERACTION_RANGE_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "block_interaction_range_mul_base");

	private static final ResourceLocation BLOCK_INTERACTION_RANGE_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "block_interaction_range_mul_total");

	private static final ResourceLocation ENTITY_INTERACTION_RANGE_ADD_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "entity_interaction_range_add");

	private static final ResourceLocation ENTITY_INTERACTION_RANGE_MUL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "entity_interaction_range_mul_base");

	private static final ResourceLocation ENTITY_INTERACTION_RANGE_MUL_TOTAL_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "entity_interaction_range_mul_total");

	private record PropertyTarget(
			Holder<Attribute> attribute,
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
			"armor", new PropertyTarget(Attributes.ARMOR,
					ARMOR_ADD_ID, ARMOR_MUL_ID, ARMOR_MUL_TOTAL_ID),
			"armor_toughness", new PropertyTarget(Attributes.ARMOR_TOUGHNESS,
					ARMOR_TOUGHNESS_ADD_ID, ARMOR_TOUGHNESS_MUL_ID, ARMOR_TOUGHNESS_MUL_TOTAL_ID),
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

	private static void updateFlight(
			final ServerPlayer player,
			final double flightLevel) {

		final UUID playerId = player.getUUID();
		final FlightState previous = FLIGHT_STATES.get(playerId);
		final boolean hasFlightRelic = flightLevel > 0.0;

		if (previous == null && hasFlightRelic) {
			final boolean grantedByVitalRelics = !player.getAbilities().mayfly;

			if (grantedByVitalRelics) {
				player.getAbilities().mayfly = true;
				player.getAbilities().setFlyingSpeed((float) (0.05 * flightLevel));
				player.onUpdateAbilities();
			}

			FLIGHT_STATES.put(playerId, new FlightState(grantedByVitalRelics, flightLevel));
			return;
		}

		if (previous != null && hasFlightRelic) {
			if (previous.grantedByVitalRelics() && Double.compare(previous.level(), flightLevel) != 0) {
				player.getAbilities().setFlyingSpeed((float) (0.05 * flightLevel));
				player.onUpdateAbilities();
				FLIGHT_STATES.put(playerId, new FlightState(true, flightLevel));
			}
			return;
		}

		if (previous == null)
			return;

		FLIGHT_STATES.remove(playerId);

		final GameType gameType = player.gameMode.getGameModeForPlayer();
		if (!previous.grantedByVitalRelics() || gameType == GameType.CREATIVE ||
				gameType == GameType.SPECTATOR)
			return;

		player.getAbilities().mayfly = false;
		player.getAbilities().flying = false;
		player.getAbilities().setFlyingSpeed(0.05F);
		player.onUpdateAbilities();
	}

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
		if (!(event.getEntity() instanceof LivingEntity livingEntity) || livingEntity.level().isClientSide()) {
			return;
		}

		final MinecraftServer server = livingEntity.getServer();
		if (server == null) {
			return;
		}

		final var relics = gatherRelics(livingEntity);
		final int currentTickCount = server.getTickCount();

		// Calls on each tick
		{
			final Map<String, Relic.Ticks.Info> ticks =
					RelicLoader.computeTicks(relics, currentTickCount);

			for (final var entry : ticks.entrySet()) {
				final TickAction action = TICK_ACTIONS.get(entry.getKey());
				if (action != null)
					action.apply(livingEntity, entry.getValue());
			}

			spawnEnemyRelicParticles(livingEntity, relics, currentTickCount);
		}

		// Scheduled to update on each half seconds
		// Scheduled to update on each half seconds
		if (currentTickCount % 10 == 0) {
			final MyLivingEntity entity =
					new NeoLivingEntity(livingEntity);

			MyUtils.removeImmuneEffects(entity, relics, MyLivingEntity.MyEffectCategory.ALL);

			MyUtils.applyRelicEffects(entity, relics);
		}

		// Scheduled to update on each second
		if (currentTickCount % 20 == 0) {
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
				updateFlight(player, flight_level);
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
						new NeoLivingEntity(livingEntity),
						rangeDamage,
						Math.round((float) reality_severance_level),
						1,
						Math.round((float) (reality_severance_level / 4.0))
				);
			}

			// Client HUD

			if (livingEntity instanceof ServerPlayer player) {
				MySpellSystem.INSTANCE.syncSpellHud(new NeoLivingEntity(player));
			}
		}

		// Scheduled to update on each 4 seconds
		if (currentTickCount % 80 == 0) {
			// Passive Skill: metal_mending

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

		/*
		Attack
		 */
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
	public static void onArrowImpact(final ProjectileImpactEvent event) {
		if (!(event.getEntity() instanceof AbstractArrow arrow)) {
			return;
		}

		if (arrow.level().isClientSide()) {
			return;
		}

		if (event.getRayTraceResult().getType() != HitResult.Type.ENTITY) {
			return;
		}

		final EntityHitResult entityResult =
				(EntityHitResult) event.getRayTraceResult();

		if (!(entityResult.getEntity() instanceof LivingEntity victim)) {
			return;
		}

		final List<Relic> relics = gatherRelics(victim);

		final double retargetLevel =
				RelicLoader.levelOfSuchPassiveSkill(
						relics,
						Relic.PASSIVE_SKILL_RETARGET_ARROW
				);

		if (retargetLevel > 0.0) {
			retargetArrow(arrow, victim, 1.0, 1.0, retargetLevel);

			event.setCanceled(true);
			return;
		}

		final double deflectionLevel =
				RelicLoader.levelOfSuchPassiveSkill(relics, Relic.PASSIVE_SKILL_ARROW_DEFLECTION);

		if (deflectionLevel <= 0.0) {
			return;
		}

		final MinecraftServer server = victim.getServer();
		if (server == null)
			return;

		final int cooldownTicks =
				Math.max(1, (int) Math.round(100.0 / deflectionLevel));

		if (!Scheduler.INSTANCE().acquireArrowDeflection(
				victim.getUUID(),
				server.getTickCount(),
				cooldownTicks
		))
			return;

		retargetArrow(arrow, victim, deflectionLevel, deflectionLevel, 0.0);

		event.setCanceled(true);
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

		if (arrow.getPersistentData().getBoolean("vitalrelics_empowered"))
			return;

		final double level = RelicLoader.levelOfSuchPassiveSkill(
				gatherRelics(owner),
				Relic.PASSIVE_SKILL_EMPOWERED_ARROW
		);

		if (level <= 0.0)
			return;

		arrow.getPersistentData().putBoolean("vitalrelics_empowered", true);

		arrow.setDeltaMovement(
				arrow.getDeltaMovement().scale(level)
		);

		final double leastDamage =
				level * owner.getAttributeValue(Attributes.ATTACK_DAMAGE);

		arrow.setBaseDamage(
				Math.max(arrow.getBaseDamage() * level, leastDamage)
		);
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(final EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide())
			return;

		if (!(event.getEntity() instanceof LivingEntity livingEntity))
			return;

		if (enemyRelicsRolled(livingEntity))
			return;

		/*
		 * Mark before rolling.
		 *
		 * Even entities which receive no relics must be marked, otherwise
		 * loading them again would give them another chance to roll.
		 */
		markEnemyRelicsRolled(livingEntity);

		final ResourceLocation entityId =
				BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());

		if (entityId == null)
			return;

		final List<Relic> relics =
				RelicLoader.INSTANCE.rollEnemyRelics(entityId.toString());

		if (relics.isEmpty())
			return;

		setEnemyRelics(livingEntity, relics);
	}
}
