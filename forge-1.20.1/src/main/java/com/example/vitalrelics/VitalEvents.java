package com.example.vitalrelics;

import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import com.example.vitalrelics.common.scheduled.Scheduler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.example.vitalrelics.Utils.*;
import static com.example.vitalrelics.VitalRelics.MODID;

public final class VitalEvents {
	private static UUID modifierId(final String name) {
		return UUID.nameUUIDFromBytes((MODID + ":" + name).getBytes(StandardCharsets.UTF_8));
	}

	private static final UUID ATTACK_DAMAGE_ADD_ID = modifierId("attack_damage_add");
	private static final UUID ATTACK_DAMAGE_MUL_ID = modifierId("attack_damage_mul_base");
	private static final UUID ATTACK_DAMAGE_MUL_TOTAL_ID = modifierId("attack_damage_mul_total");

	private static final UUID ATTACK_SPEED_ADD_ID = modifierId("attack_speed_add");
	private static final UUID ATTACK_SPEED_MUL_ID = modifierId("attack_speed_mul_base");
	private static final UUID ATTACK_SPEED_MUL_TOTAL_ID = modifierId("attack_speed_mul_total");

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

		if (tick % 10 == 0) {
			removeImmuneEffects(livingEntity, relics);
			applyRelicEffects(livingEntity, relics);

			if (livingEntity instanceof ServerPlayer player) {
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

		if (tick % 20 == 0) {
			int reality_severance_level = RelicLoader.hasSuchSpecialAbility(
					relics, "reality_severance"
			);
			if (reality_severance_level > 0) {
				final float ratioDamage = reality_severance_level / 100.0f;
				final float rangeDamage = ratioDamage * (float)livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE);
				MyDamageInfo.directRangedAttack(livingEntity, rangeDamage, reality_severance_level, 1, Math.round(reality_severance_level / 4.0f));
			}
		}

		final Relic.Ticks ticks = RelicLoader.computeTicks(relics, tick);

		livingEntity.heal((float)(ticks.heal.add + livingEntity.getMaxHealth() * ticks.heal.ratio_add));

		if (livingEntity instanceof Player player) {
			final float feed =
					(float)(ticks.feed.add + player.getMaxHealth() * ticks.feed.ratio_add);

			player.getFoodData().eat(Math.round(feed), 1.0F);
		}

		final Relic.Properties prop = RelicLoader.computeProperties(relics);

		applyProperty(livingEntity.getAttribute(Attributes.ATTACK_DAMAGE), prop.attack_damage,
				ATTACK_DAMAGE_ADD_ID, ATTACK_DAMAGE_MUL_ID, ATTACK_DAMAGE_MUL_TOTAL_ID);

		applyProperty(livingEntity.getAttribute(Attributes.ATTACK_SPEED), prop.attack_speed,
				ATTACK_SPEED_ADD_ID, ATTACK_SPEED_MUL_ID, ATTACK_SPEED_MUL_TOTAL_ID);

		applyProperty(livingEntity.getAttribute(Attributes.KNOCKBACK_RESISTANCE), prop.knockback_resistance,
				KNOCKBACK_RESISTANCE_ADD_ID, KNOCKBACK_RESISTANCE_MUL_ID,
				KNOCKBACK_RESISTANCE_MUL_TOTAL_ID);

		applyProperty(livingEntity.getAttribute(Attributes.MAX_HEALTH), prop.max_health,
				MAX_HEALTH_ADD_ID, MAX_HEALTH_MUL_ID, MAX_HEALTH_MUL_TOTAL_ID);

		applyProperty(livingEntity.getAttribute(ForgeMod.BLOCK_REACH.get()), prop.block_interaction_range,
				BLOCK_REACH_ADD_ID, BLOCK_REACH_MUL_ID, BLOCK_REACH_MUL_TOTAL_ID);

		applyProperty(livingEntity.getAttribute(ForgeMod.ENTITY_REACH.get()), prop.entity_interaction_range,
				ENTITY_REACH_ADD_ID, ENTITY_REACH_MUL_ID, ENTITY_REACH_MUL_TOTAL_ID);

		if (livingEntity.getHealth() > livingEntity.getMaxHealth())
			livingEntity.setHealth(livingEntity.getMaxHealth());
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
			attr.addTransientModifier(new AttributeModifier(id, MODID, amount, operation));
	}


	@SubscribeEvent
	public static void onLivingDamage(final LivingDamageEvent event) {
		final LivingEntity victim = event.getEntity();
		final Entity attacker = event.getSource().getEntity();

		if (victim.level().isClientSide()) {
			return;
		}

		MinecraftServer victim_server = victim.getServer();
		if (victim_server == null) {
			return;
		}

		float amount = event.getAmount();

		if (attacker instanceof LivingEntity livingAttacker) {
			float invulnerableTime = victim.invulnerableTime;

			for (final Relic relic : gatherRelics(livingAttacker)) {
				if (relic.callbacks.damage_dealt != null)
					amount = (float)relic.callbacks.damage_dealt.process(
							amount, victim.getMaxHealth());

				if (relic.callbacks.invulnerable_time_dealt != null)
					invulnerableTime = (float)relic.callbacks.invulnerable_time_dealt.process(
							invulnerableTime, 10.0);
			}

			victim.invulnerableTime = Math.round(invulnerableTime);
		}

		float invulnerable_time = victim.invulnerableTime;

		for (final Relic relic : gatherRelics(victim)) {
			if (relic.callbacks.damage_taken != null)
				amount = (float)relic.callbacks.damage_taken.process(
						amount, victim.getMaxHealth());

			if (relic.callbacks.invulnerable_time_taken != null)
				invulnerable_time = (float)relic.callbacks.invulnerable_time_taken.process(
						invulnerable_time, 10.0);
		}

		if (victim.invulnerableTime != invulnerable_time) {
			// Modified by protection
			UUID victimUUID = victim.getUUID();
			if (Scheduler.INSTANCE().PROTECTED_PLAYER_LIST.getOrDefault(victimUUID, 0) == 0) {
				Scheduler.INSTANCE().PROTECTED_PLAYER_LIST.put(victimUUID, victim_server.getTickCount(), Math.round(invulnerable_time));
			} else {
				amount = 0.f;
			}
		}

		victim.invulnerableTime = Math.round(invulnerable_time);
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

		final int level =
				RelicLoader.hasSuchSpecialAbility(gatherRelics(victim), "retarget_arrow");

		if (level != 0) {
			retargetArrow(arrow, victim);
			event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
		}
	}
}