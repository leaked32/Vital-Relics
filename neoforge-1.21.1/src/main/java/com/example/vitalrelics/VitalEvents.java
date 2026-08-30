package com.example.vitalrelics;

import com.example.vitalrelics.common.*;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MyUtils;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Loader;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
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

	private VitalEvents() {}

	@SubscribeEvent
	public static void onServerTick(final ServerTickEvent.Post event) {

		Scheduler.INSTANCE().serverTick(
				event.getServer().getTickCount()
		);
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

		final MyLivingEntity entity = new NeoLivingEntity(livingEntity);
		MyEvents.onLivingEntityTick(entity, currentTickCount, relics);
		spawnEnemyRelicParticles(livingEntity, relics, currentTickCount);

		// Scheduled to update on each second
		if (currentTickCount % 20 == 0) {
			// Properties

			final Map<String, Relic.Properties.Info> properties =
					Loader.computeProperties(relics);

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
				final double flightLevel = Loader.levelOfSuchPassiveSkill(
						relics, Relic.PASSIVE_SKILL_FLIGHT
				);

				final Scheduler.FlightUpdate update =
						Scheduler.INSTANCE().updateFlight(
								player.getUUID(),
								currentTickCount,
								flightLevel,
								player.getAbilities().mayfly
						);

				switch (update.action()) {
					case GRANT -> {
						player.getAbilities().mayfly = true;

						if (Math.abs(update.level() - 1.0) > 1.0E-9) {
							player.getAbilities().setFlyingSpeed(
									(float) (0.05 * update.level())
							);
						}

						player.onUpdateAbilities();
					}

					case UPDATE -> {
						if (Math.abs(update.level() - 1.0) > 1.0E-9) {
							player.getAbilities().setFlyingSpeed(
									(float) (0.05 * update.level())
							);
						} else {
							player.getAbilities().setFlyingSpeed(0.05F);
						}

						player.onUpdateAbilities();
					}

					case REMOVE -> {
						if (!player.isCreative() && !player.isSpectator()) {
							player.getAbilities().mayfly = false;
							player.getAbilities().flying = false;

							if (Math.abs(update.level() - 1.0) > 1.0E-9) {
								player.getAbilities().setFlyingSpeed(0.05F);
							}

							player.onUpdateAbilities();
						}
					}

					case NONE -> {}
				}
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

		if (victim.level().isClientSide())
			return;

		final MinecraftServer server = victim.getServer();
		if (server == null)
			return;

		final Entity source = event.getSource().getEntity();
		final MyLivingEntity attacker = source instanceof LivingEntity livingSource
				? new NeoLivingEntity(livingSource)
				: null;

		event.setNewDamage(MyEvents.onLivingDamage(
				new NeoLivingEntity(victim), attacker,
				event.getNewDamage(), server.getTickCount()
		));
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

		if (Loader.isImmuneToEffect(
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
				Loader.levelOfSuchPassiveSkill(relics, Relic.PASSIVE_SKILL_RETARGET_ARROW);

		if (retargetLevel > 0.0) {
			retargetArrow(arrow, victim, 1.0, 1.0, retargetLevel);

			event.setCanceled(true);
			return;
		}

		final double deflectionLevel =
				Loader.levelOfSuchPassiveSkill(relics, Relic.PASSIVE_SKILL_ARROW_DEFLECTION);

		if (deflectionLevel <= 0.0) {
			return;
		}

		final MinecraftServer server = victim.getServer();
		if (server == null)
			return;

		final int cooldownTicks =
				Math.max(1, (int) Math.round(100.0 / deflectionLevel));

		if (!Scheduler.INSTANCE().acquireArrowDeflection(
				victim.getUUID(), server.getTickCount(), cooldownTicks
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

		final double level = Loader.levelOfSuchPassiveSkill(
				gatherRelics(owner),
				Relic.PASSIVE_SKILL_EMPOWERED_ARROW
		);

		if (level <= 0.0)
			return;

		arrow.getPersistentData().putBoolean("vitalrelics_empowered", true);

		arrow.setDeltaMovement(
				arrow.getDeltaMovement().scale(level)
		);

		arrow.setBaseDamage(arrow.getBaseDamage() * level);

//		final double leastDamage =
//				level * owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
//
//		arrow.setBaseDamage(
//				Math.max(arrow.getBaseDamage() * level, leastDamage)
//		);
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
				Loader.get().rollEnemyRelics(entityId.toString());

		if (relics.isEmpty())
			return;

		setEnemyRelics(livingEntity, relics);
	}


	/*
	Give the player the guide-book for the first time.
	 */

	private static final String GUIDE_BOOK_GIVEN =
			Manifest.MODID + ":guide_book_given";

	@SubscribeEvent
	public static void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		final var data = player.getPersistentData();

		if (data.getBoolean(GUIDE_BOOK_GIVEN))
			return;

		final var book = VitalRelics.GUIDE_BOOK.get().getDefaultInstance();

		if (!player.getInventory().add(book))
			player.drop(book, false);

		data.putBoolean(GUIDE_BOOK_GIVEN, true);
	}


	/*
	Clean Up
	 */

	@SubscribeEvent
	public static void onPlayerLoggedOut(
			final PlayerEvent.PlayerLoggedOutEvent event) {

		if (event.getEntity().level().isClientSide())
			return;

		MyEvents.onPlayerLoggedOut(
				event.getEntity().getUUID()
		);
	}

	@SubscribeEvent
	public static void onServerStopping(
			final ServerStoppingEvent event) {

		MyEvents.onServerStopping();
	}
}
