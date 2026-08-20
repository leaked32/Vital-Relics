package com.example.vitalrelics;

import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.vitalrelics.Utils.*;
import static com.example.vitalrelics.VitalRelics.MODID;
import static com.example.vitalrelics.VitalRelics.loader;

public final class VitalEvents {
	private static UUID modifierId(final String name) {
		return UUID.nameUUIDFromBytes(
				(MODID + ":" + name).getBytes(StandardCharsets.UTF_8)
		);
	}

	private static final UUID ATTACK_DAMAGE_ADD_ID =
			modifierId("attack_damage_add");
	private static final UUID ATTACK_DAMAGE_MUL_ID =
			modifierId("attack_damage_mul_base");
	private static final UUID ATTACK_DAMAGE_MUL_TOTAL_ID =
			modifierId("attack_damage_mul_total");

	private static final UUID ATTACK_SPEED_ADD_ID =
			modifierId("attack_speed_add");
	private static final UUID ATTACK_SPEED_MUL_ID =
			modifierId("attack_speed_mul_base");
	private static final UUID ATTACK_SPEED_MUL_TOTAL_ID =
			modifierId("attack_speed_mul_total");

	private static final UUID KNOCKBACK_RESISTANCE_ADD_ID =
			modifierId("knockback_resistance_add");
	private static final UUID KNOCKBACK_RESISTANCE_MUL_ID =
			modifierId("knockback_resistance_mul_base");
	private static final UUID KNOCKBACK_RESISTANCE_MUL_TOTAL_ID =
			modifierId("knockback_resistance_mul_total");

	private static final UUID MAX_HEALTH_ADD_ID =
			modifierId("max_health_add");
	private static final UUID MAX_HEALTH_MUL_ID =
			modifierId("max_health_mul_base");
	private static final UUID MAX_HEALTH_MUL_TOTAL_ID =
			modifierId("max_health_mul_total");

	private static final UUID BLOCK_REACH_ADD_ID =
			modifierId("block_interaction_range_add");
	private static final UUID BLOCK_REACH_MUL_ID =
			modifierId("block_interaction_range_mul_base");
	private static final UUID BLOCK_REACH_MUL_TOTAL_ID =
			modifierId("block_interaction_range_mul_total");

	private static final UUID ENTITY_REACH_ADD_ID =
			modifierId("entity_interaction_range_add");
	private static final UUID ENTITY_REACH_MUL_ID =
			modifierId("entity_interaction_range_mul_base");
	private static final UUID ENTITY_REACH_MUL_TOTAL_ID =
			modifierId("entity_interaction_range_mul_total");

	private VitalEvents() {}


	@SubscribeEvent
	public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;

		final Player player = event.player;

		if (player.level().isClientSide())
			return;

		final List<Relic> relicList = gatherRelics(player);

		final int currentTick =
				player.getServer().getTickCount();

		if (currentTick % 10 == 0)
			removeImmuneEffects(player, relicList);

		final Relic.Ticks ticks =
				RelicLoader.computeTicks(relicList, currentTick);

		final float heal =
				(float)(ticks.heal.add +
						player.getMaxHealth() * ticks.heal.ratio_add);

		final float feed =
				(float)(ticks.feed.add +
						player.getMaxHealth() * ticks.feed.ratio_add);

		player.heal(heal);
		player.getFoodData().eat(Math.round(feed), 1.0F);

		final Relic.Properties prop =
				RelicLoader.computeProperties(relicList);

		applyProperty(
				player.getAttribute(Attributes.ATTACK_DAMAGE),
				prop.attack_damage,
				ATTACK_DAMAGE_ADD_ID,
				ATTACK_DAMAGE_MUL_ID,
				ATTACK_DAMAGE_MUL_TOTAL_ID
		);

		applyProperty(
				player.getAttribute(Attributes.ATTACK_SPEED),
				prop.attack_speed,
				ATTACK_SPEED_ADD_ID,
				ATTACK_SPEED_MUL_ID,
				ATTACK_SPEED_MUL_TOTAL_ID
		);

		applyProperty(
				player.getAttribute(Attributes.KNOCKBACK_RESISTANCE),
				prop.knockback_resistance,
				KNOCKBACK_RESISTANCE_ADD_ID,
				KNOCKBACK_RESISTANCE_MUL_ID,
				KNOCKBACK_RESISTANCE_MUL_TOTAL_ID
		);

		applyProperty(
				player.getAttribute(Attributes.MAX_HEALTH),
				prop.max_health,
				MAX_HEALTH_ADD_ID,
				MAX_HEALTH_MUL_ID,
				MAX_HEALTH_MUL_TOTAL_ID
		);

		/*
		 * 1.20.1 does not have:
		 *
		 *     Attributes.BLOCK_INTERACTION_RANGE
		 *     Attributes.ENTITY_INTERACTION_RANGE
		 *
		 * Forge provides reach attributes instead.
		 */
		applyProperty(
				player.getAttribute(ForgeMod.BLOCK_REACH.get()),
				prop.block_interaction_range,
				BLOCK_REACH_ADD_ID,
				BLOCK_REACH_MUL_ID,
				BLOCK_REACH_MUL_TOTAL_ID
		);

		applyProperty(
				player.getAttribute(ForgeMod.ENTITY_REACH.get()),
				prop.entity_interaction_range,
				ENTITY_REACH_ADD_ID,
				ENTITY_REACH_MUL_ID,
				ENTITY_REACH_MUL_TOTAL_ID
		);

		if (player.getHealth() > player.getMaxHealth())
			player.setHealth(player.getMaxHealth());
	}

	private static void applyProperty(
			final AttributeInstance attribute,
			final Relic.Properties.Info property,
			final UUID addId,
			final UUID mulBaseId,
			final UUID mulTotalId) {

		if (attribute == null || property == null)
			return;

		replaceModifier(
				attribute,
				addId,
				property.add,
				AttributeModifier.Operation.ADDITION
		);

		replaceModifier(
				attribute,
				mulBaseId,
				property.mul_base,
				AttributeModifier.Operation.MULTIPLY_BASE
		);

		replaceModifier(
				attribute,
				mulTotalId,
				property.mul_total - 1.0,
				AttributeModifier.Operation.MULTIPLY_TOTAL
		);
	}

	private static void replaceModifier(
			final AttributeInstance attribute,
			final UUID id,
			final double amount,
			final AttributeModifier.Operation operation) {

		attribute.removeModifier(id);

		if (amount != 0.0) {
			attribute.addTransientModifier(
					new AttributeModifier(
							id,
							MODID,
							amount,
							operation
					)
			);
		}
	}

	@SubscribeEvent
	public static void onLivingHurt(final LivingHurtEvent event) {
		final LivingEntity victim = event.getEntity();
		final Entity criminal = event.getSource().getEntity();

		if (victim.level().isClientSide())
			return;

		float amount = event.getAmount();

		if (criminal instanceof Player player) {
			final List<Relic> relicList = gatherRelics(player);
			float invulnerableTime = victim.invulnerableTime;

			for (final Relic relic : relicList) {
				if (relic.callbacks.damage_dealt != null) {
					amount = (float)relic.callbacks.damage_dealt.process(
							amount,
							victim.getMaxHealth()
					);
				}

				if (relic.callbacks.invulnerable_time_dealt != null) {
					invulnerableTime =
							(float)relic.callbacks.invulnerable_time_dealt.process(
									invulnerableTime,
									10.0
							);
				}
			}

			victim.invulnerableTime = Math.round(invulnerableTime);
		}

		if (victim instanceof Player player) {
			final List<Relic> relicList = gatherRelics(player);
			float invulnerableTime = victim.invulnerableTime;

			for (final Relic relic : relicList) {
				if (relic.callbacks.damage_taken != null) {
					amount = (float)relic.callbacks.damage_taken.process(
							amount,
							player.getMaxHealth()
					);
				}

				if (relic.callbacks.invulnerable_time_taken != null) {
					invulnerableTime =
							(float)relic.callbacks.invulnerable_time_taken.process(
									invulnerableTime,
									10.0
							);
				}
			}

			victim.invulnerableTime = Math.round(invulnerableTime);
		}

		event.setAmount(amount);
	}

	@SubscribeEvent
	public static void onEffectApplicable(final MobEffectEvent.Applicable event) {
		if (!(event.getEntity() instanceof Player player))
			return;

		if (player.level().isClientSide())
			return;

		final List<Relic> relics = gatherRelics(player);
		final MobEffect effect = event.getEffectInstance().getEffect();

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
			event.setResult(Event.Result.DENY);
		}
	}

	@SubscribeEvent
	public static void onArrowImpact(final ProjectileImpactEvent event) {
		if (!(event.getProjectile() instanceof AbstractArrow arrow))
			return;

		if (arrow.level().isClientSide())
			return;

		if (!(event.getRayTraceResult() instanceof EntityHitResult hit))
			return;

		if (!(hit.getEntity() instanceof Player victim))
			return;

		final List<Relic> relics = gatherRelics(victim);

		if (RelicLoader.hasSuchSpecialAbility(
				relics,
				"retarget_arrow"
		)) {
			retargetArrow(arrow, victim);
			event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
		}
	}
}