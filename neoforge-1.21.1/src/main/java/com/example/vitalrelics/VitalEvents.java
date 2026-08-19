package com.example.vitalrelics;

import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.ArrayList;
import java.util.List;

import static com.example.vitalrelics.Utils.retargetArrow;
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

	public static List<Relic> gatherRelics(Player player) {

		List<Relic> relicList = new ArrayList<>();
		for (final ItemStack stack : player.getInventory().items) {
			if (stack.isEmpty())
				continue;

			final ResourceLocation id =
					BuiltInRegistries.ITEM.getKey(stack.getItem());

			if (!id.getNamespace().equals(MODID))
				continue;

			final Relic relic = loader.find(id.getPath());

			if (relic == null || relic.properties == null)
				continue;

			relicList.add(relic);
		}
		return relicList;
	}

	private static void removeImmuneEffects(
			final Player player,
			final List<Relic> relics) {

		final List<ResourceLocation> remove = new ArrayList<>();

		for (final MobEffectInstance instance : player.getActiveEffects()) {
			final var effect = instance.getEffect().value();

			final ResourceLocation id =
					BuiltInRegistries.MOB_EFFECT.getKey(effect);

			if (id == null)
				continue;

			final boolean negative =
					effect.getCategory() == MobEffectCategory.HARMFUL;

			if (RelicLoader.isImmuneToEffect(
					relics,
					id.getPath(),
					negative
			)) {
				remove.add(id);
			}
		}

		for (final ResourceLocation id : remove) {
			final var effect =
					BuiltInRegistries.MOB_EFFECT.get(id);

			player.removeEffect(
					BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)
			);
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(final PlayerTickEvent.Post event) {
		final Player player = event.getEntity();

		// Gameplay state should be authoritative on the server

		if (player.level().isClientSide())
			return;
		var relicList = gatherRelics(player);

		// Ticks

		final int currentTick = event.getEntity().getServer().getTickCount();

		if (currentTick % 10 == 0) {
			removeImmuneEffects(player, relicList);
		}

		var ticks = RelicLoader.computeTicks(relicList, currentTick);

		float heal = (float)(ticks.heal.add + player.getMaxHealth() * ticks.heal.ratio_add);
		float feed = (float)(ticks.feed.add + player.getMaxHealth() * ticks.feed.ratio_add);
		player.heal(heal);
		player.getFoodData().eat(Math.round(feed), 1.f);
		// LOGGER.info("onPlayerTick heal={} feed={}", heal, feed);

		// Properties

		var prop = RelicLoader.computeProperties(relicList);

		final AttributeInstance attrDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
		final AttributeInstance attrMaxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		final AttributeInstance attrKnockBackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		final AttributeInstance attrAttackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
		final AttributeInstance attrBlockInteractionRange = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
		final AttributeInstance attrEntityInteractionRange = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);

		if (attrDamage != null) {
			replaceModifier(attrDamage, ATTACK_DAMAGE_ADD_ID,
					prop.attack_damage.add, AttributeModifier.Operation.ADD_VALUE);

			replaceModifier(attrDamage, ATTACK_DAMAGE_MUL_ID,
					prop.attack_damage.mul_base, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

			replaceModifier(attrDamage, ATTACK_DAMAGE_MUL_TOTAL_ID,
					prop.attack_damage.mul_total - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}

		if (attrAttackSpeed != null) {
			replaceModifier(attrAttackSpeed, ATTACK_SPEED_ADD_ID,
					prop.attack_speed.add, AttributeModifier.Operation.ADD_VALUE);

			replaceModifier(attrAttackSpeed, ATTACK_SPEED_MUL_ID,
					prop.attack_speed.mul_base, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

			replaceModifier(attrAttackSpeed, ATTACK_SPEED_MUL_TOTAL_ID,
					prop.attack_speed.mul_total - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}
		if (attrKnockBackResistance != null) {
			replaceModifier(attrKnockBackResistance, KNOCKBACK_RESISTANCE_ADD_ID,
					prop.knockback_resistance.add, AttributeModifier.Operation.ADD_VALUE);

			replaceModifier(attrKnockBackResistance, KNOCKBACK_RESISTANCE_MUL_ID,
					prop.knockback_resistance.mul_base, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

			replaceModifier(attrKnockBackResistance, KNOCKBACK_RESISTANCE_MUL_TOTAL_ID,
					prop.knockback_resistance.mul_total - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

		}
		if (attrMaxHealth != null) {
			replaceModifier(attrMaxHealth, MAX_HEALTH_ADD_ID,
					prop.max_health.add, AttributeModifier.Operation.ADD_VALUE);

			replaceModifier(attrMaxHealth, MAX_HEALTH_MUL_ID,
					prop.max_health.mul_base, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

			replaceModifier(attrMaxHealth, MAX_HEALTH_MUL_TOTAL_ID,
					prop.max_health.mul_total - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

		}
		if (attrBlockInteractionRange != null) {
			replaceModifier(attrBlockInteractionRange, BLOCK_INTERACTION_RANGE_ADD_ID,
					prop.block_interaction_range.add, AttributeModifier.Operation.ADD_VALUE);

			replaceModifier(attrBlockInteractionRange, BLOCK_INTERACTION_RANGE_MUL_ID,
					prop.block_interaction_range.mul_base, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

			replaceModifier(attrBlockInteractionRange, BLOCK_INTERACTION_RANGE_MUL_TOTAL_ID,
					prop.block_interaction_range.mul_total - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}
		if (attrEntityInteractionRange != null) {
			replaceModifier(attrEntityInteractionRange, ENTITY_INTERACTION_RANGE_ADD_ID,
					prop.entity_interaction_range.add, AttributeModifier.Operation.ADD_VALUE);

			replaceModifier(attrEntityInteractionRange, ENTITY_INTERACTION_RANGE_MUL_ID,
					prop.entity_interaction_range.mul_base, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

			replaceModifier(attrEntityInteractionRange, ENTITY_INTERACTION_RANGE_MUL_TOTAL_ID,
					prop.entity_interaction_range.mul_total - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}
		if (player.getHealth() > player.getMaxHealth())
			player.setHealth(player.getMaxHealth());
	}

	private static void replaceModifier(
			final AttributeInstance attribute,
			final ResourceLocation id,
			final double amount,
			final AttributeModifier.Operation operation) {
		attribute.removeModifier(id);

		if (amount != 0.0) {
			attribute.addTransientModifier(
					new AttributeModifier(id, amount, operation)
			);
		}
	}

	@SubscribeEvent
	public static void onLivingDamage(final LivingDamageEvent.Pre event) {
		final LivingEntity victim = event.getEntity();
		final Entity criminal = event.getSource().getEntity();

		if (victim.level().isClientSide())
			return;

		if (criminal instanceof Player player) {

			var relicList = gatherRelics(player);
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


		if (victim instanceof Player player) {
			var relicList = gatherRelics(player);
			float amount = event.getNewDamage();
			float invulnerable_time = victim.invulnerableTime;

			for (final var relic : relicList) {
				if (relic.callbacks.damage_taken != null) {
					amount = (float)relic.callbacks.damage_taken.process(amount, player.getMaxHealth());
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
		if (!(event.getEntity() instanceof Player player))
			return;

		if (player.level().isClientSide())
			return;

		final var relics = gatherRelics(player);
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
			if (entityResult.getEntity() instanceof Player victim) {

				final var relics = gatherRelics(victim);
				if (RelicLoader.hasSuchSpecialAbility(relics, "retarget_arrow")) {
					retargetArrow(arrow, victim);
					event.setCanceled(true);
				}
			}
		}
	}
}