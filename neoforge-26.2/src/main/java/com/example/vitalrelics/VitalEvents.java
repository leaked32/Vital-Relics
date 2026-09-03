package com.example.vitalrelics;

import com.example.vitalrelics.common.*;
import com.example.vitalrelics.common.platform.MyDamageSource;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.platform.NeoAbstractArrow;
import com.example.vitalrelics.platform.NeoDamageSource;
import com.example.vitalrelics.platform.NeoLivingEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
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

import java.util.List;
import java.util.Map;

import static com.example.vitalrelics.Utils.*;

public final class VitalEvents {

	/*
	Properties
	 */
	private static Identifier makePropertyId(final String propertyName) {
		return Identifier.fromNamespaceAndPath(Manifest.MODID, propertyName);
	}

	private record PropertyTarget(
			Holder<Attribute> attribute,
			Identifier addId,
			Identifier mulBaseId,
			Identifier mulTotalId) {

		private static PropertyTarget of(
				final String propertyName,
				final Holder<Attribute> attribute) {
			return new PropertyTarget(
					attribute,
					makePropertyId(propertyName + "_add"),
					makePropertyId(propertyName + "_mul_base"),
					makePropertyId(propertyName + "_mul_total")
			);
		}
	}

	private static Map.Entry<String, PropertyTarget> propertyTarget(
			final String propertyName,
			final Holder<Attribute> attribute) {
		return Map.entry(propertyName, PropertyTarget.of(propertyName, attribute));
	}

	private static final Map<String, PropertyTarget> PROPERTY_TARGETS = Map.ofEntries(
			propertyTarget("attack_damage", Attributes.ATTACK_DAMAGE),
			propertyTarget("attack_speed", Attributes.ATTACK_SPEED),
			propertyTarget("armor", Attributes.ARMOR),
			propertyTarget("armor_toughness", Attributes.ARMOR_TOUGHNESS),
			propertyTarget("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE),
			propertyTarget("max_health", Attributes.MAX_HEALTH),
			propertyTarget("block_interaction_range", Attributes.BLOCK_INTERACTION_RANGE),
			propertyTarget("entity_interaction_range", Attributes.ENTITY_INTERACTION_RANGE)
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
			final Identifier addId,
			final Identifier mulBaseId,
			final Identifier mulTotalId) {

		if (attr == null || prop == null)
			return;

		replaceModifier(attr, addId, prop.add, AttributeModifier.Operation.ADD_VALUE);
		replaceModifier(attr, mulBaseId, prop.mul_base, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		replaceModifier(attr, mulTotalId, prop.mul_total - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void replaceModifier(
			final AttributeInstance attr,
			final Identifier id,
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

		final MyDamageSource myDamageSource =
				new NeoDamageSource(event.getSource());

		event.setNewDamage(MyEvents.onLivingDamage(
				new NeoLivingEntity(victim), myDamageSource,
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

		final Identifier id =
				BuiltInRegistries.MOB_EFFECT.getKey(effect);

		if (id == null)
			return;

		final boolean negative = effect.getCategory() == MobEffectCategory.HARMFUL;

		if (
				Loader.isImmuneToEffect(relics, id.getPath(), negative)
		) {
			event.setResult(
					MobEffectEvent.Applicable.Result.DO_NOT_APPLY
			);
		}
	}

	@SubscribeEvent
	public static void onArrowImpact(final ProjectileImpactEvent event) {
		if (!(event.getEntity() instanceof AbstractArrow arrow) ||
				arrow.level().isClientSide() ||
				!(event.getRayTraceResult() instanceof EntityHitResult hit) ||
				!(hit.getEntity() instanceof LivingEntity victim))
			return;

		final MinecraftServer server = victim.getServer();
		if (server == null)
			return;

		if (MyEvents.onArrowImpact(
				new NeoAbstractArrow(arrow),
				new NeoLivingEntity(victim),
				gatherRelics(victim),
				server.getTickCount()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onArrowShot(final EntityJoinLevelEvent event) {
		if (!(event.getEntity() instanceof AbstractArrow arrow) ||
				event.getLevel().isClientSide())
			return;

		if (!(arrow.getOwner() instanceof LivingEntity owner))
			return;

		MyEvents.onArrowShot(
				new NeoAbstractArrow(arrow),
				new NeoLivingEntity(owner),
				gatherRelics(owner));
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

		final Identifier entityId =
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

		if (data.getBoolean(GUIDE_BOOK_GIVEN).orElse(false))
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
