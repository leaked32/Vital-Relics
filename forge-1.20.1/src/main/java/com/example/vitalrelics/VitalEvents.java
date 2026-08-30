package com.example.vitalrelics;

import com.example.vitalrelics.common.*;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MyUtils;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.platform.ForgeLivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

	private VitalEvents() {}

	@SubscribeEvent
	public static void onServerTick(
			final TickEvent.ServerTickEvent event) {

		if (event.phase != TickEvent.Phase.END)
			return;

		Scheduler.INSTANCE().serverTick(
				event.getServer().getTickCount()
		);
	}

	@SubscribeEvent
	public static void onLivingTick(final LivingEvent.LivingTickEvent event) {
		final LivingEntity livingEntity = event.getEntity();

		if (livingEntity.level().isClientSide()) {
			return;
		}

		final MinecraftServer server = livingEntity.getServer();
		if (server == null)
			return;

		final List<Relic> relics = gatherRelics(livingEntity);
		final int currentTickCount = server.getTickCount();

		final MyLivingEntity entity = new ForgeLivingEntity(livingEntity);
		MyEvents.onLivingEntityTick(entity, currentTickCount, relics);
		spawnEnemyRelicParticles(livingEntity, relics, currentTickCount);

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
			final AttributeInstance attr, final UUID id, final double amount,
			final AttributeModifier.Operation operation) {

		attr.removeModifier(id);

		if (amount != 0.0)
			attr.addTransientModifier(new AttributeModifier(id, Manifest.MODID, amount, operation));
	}

	@SubscribeEvent
	public static void onLivingDamage(final LivingDamageEvent event) {
		final LivingEntity victim = event.getEntity();

		if (victim.level().isClientSide())
			return;

		final MinecraftServer server = victim.getServer();
		if (server == null)
			return;

		final Entity source = event.getSource().getEntity();
		final MyLivingEntity attacker = source instanceof LivingEntity livingSource
				? new ForgeLivingEntity(livingSource)
				: null;

		event.setAmount(MyEvents.onLivingDamage(
				new ForgeLivingEntity(victim), attacker,
				event.getAmount(), server.getTickCount()
		));
	}

	@SubscribeEvent
	public static void onEffectApplicable(final MobEffectEvent.Applicable event) {
		final LivingEntity entity = event.getEntity();

		if (entity.level().isClientSide())
			return;

		final MobEffect effect = event.getEffectInstance().getEffect();
		final ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);

		if (id == null)
			return;

		final boolean negative = effect.getCategory() == MobEffectCategory.HARMFUL;

		if (Loader.isImmuneToEffect(gatherRelics(entity), id.getPath(), negative))
			event.setResult(Event.Result.DENY);
	}

	@SubscribeEvent
	public static void onArrowImpact(final ProjectileImpactEvent event) {
		if (!(event.getProjectile() instanceof AbstractArrow arrow) ||
				arrow.level().isClientSide() ||
				!(event.getRayTraceResult() instanceof EntityHitResult hit) ||
				!(hit.getEntity() instanceof LivingEntity victim))
			return;

		final List<Relic> relics = gatherRelics(victim);

		final double retargetLevel =
				Loader.levelOfSuchPassiveSkill(
						relics,
						Relic.PASSIVE_SKILL_RETARGET_ARROW
				);

		if (retargetLevel > 0.0) {
			retargetArrow(arrow, victim, 1.0, 1.0, retargetLevel);

			event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
			return;
		}

		final double deflectionLevel =
				Loader.levelOfSuchPassiveSkill(
						relics,
						Relic.PASSIVE_SKILL_ARROW_DEFLECTION
				);

		if (deflectionLevel <= 0.0)
			return;

		final MinecraftServer server = victim.getServer();
		if (server == null)
			return;

		final int cooldownTicks =
				Math.max(1, (int) Math.round(100.0 / deflectionLevel));

		if (!Scheduler.INSTANCE().acquireArrowDeflection(
				victim.getUUID(), server.getTickCount(), cooldownTicks
		)) {
			return;
		}

		retargetArrow(arrow, victim, deflectionLevel, deflectionLevel, 0.0);

		event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
	}

//	@SubscribeEvent
//	public static void onArrowLoose(final ArrowLooseEvent event) {
//		final Player player = event.getEntity();
//
//		if (player.level().isClientSide()) {
//			return;
//		}
//
//		final double level = RelicLoader.levelOfSuchPassiveSkill(
//				gatherRelics(player),
//				Relic.PASSIVE_SKILL_EMPOWERED_ARROW
//		);
//
//		if (level <= 0.0)
//			return;
//
//		event.setCharge(Math.round((float) (event.getCharge() * level)));
//	}

	@SubscribeEvent
	public static void onArrowShot(final EntityJoinLevelEvent event) {
		if (!(event.getEntity() instanceof AbstractArrow arrow) ||
				event.getLevel().isClientSide()) {
			return;
		}

		if (!(arrow.getOwner() instanceof LivingEntity owner)) {
			return;
		}

		final double level = Loader.levelOfSuchPassiveSkill(
				gatherRelics(owner),
				Relic.PASSIVE_SKILL_EMPOWERED_ARROW
		);

		if (level <= 0.0)
			return;

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
				ForgeRegistries.ENTITY_TYPES.getKey(livingEntity.getType());

		if (entityId == null)
			return;

		final List<Relic> relics =
				Loader.get().rollEnemyRelics(entityId.toString());

		if (relics.isEmpty())
			return;

		setEnemyRelics(livingEntity, relics);
		// updateEnemyRelicName(livingEntity, relics);
	}

	/*
	Give the player the guide-book for the first time.
	 */

	private static final String GUIDE_BOOK_GIVEN = Manifest.MODID + ":guide_book_given";

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
