package com.example.vitalrelics;


import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

import static com.example.vitalrelics.VitalRelics.MODID;
import static com.example.vitalrelics.VitalRelics.loader;
import static com.example.vitalrelics.compat.TouhouMaidCompat.gatherMaidRelics;

public class Utils {

	/*
	What does it have?
	 */

	public static List<Relic> gatherRelics(final LivingEntity entity) {
		final List<Relic> out = new ArrayList<>();

		// Player inventory / hotbar.
		if (entity instanceof Player player) {
			for (int i = 0; i < player.getInventory().items.size(); ++i) {
				final ItemStack stack = player.getInventory().items.get(i);
				final boolean hotbar = i < 9;

				addRelic(out, stack,
						hotbar ? "in_hotbar" : "in_inventory",
						"in_inventory");
			}
		}

		// Curios works on LivingEntity, not only Player.
		CuriosApi.getCuriosInventory(entity).ifPresent(inv -> {
			for (final var slot : inv.findCurios(stack -> true))
				addRelic(out, slot.stack(), "in_curios_api_slots");
		});

		// Optional Touhou Little Maid integration.
		if (ModList.get().isLoaded("touhou_little_maid")) {
			gatherMaidRelics(entity, out);
		}

		return out;
	}

	private static boolean effectiveAt(final Relic relic, final String location) {
		if (relic.effective_slots.isEmpty()) {
			return location.equals("in_curios_api_slots") || location.equals("in_touhou_little_maid_curios_slots");
		}

		return relic.effective_slots.contains(location);
	}

	public static void addRelic(
			final List<Relic> out,
			final ItemStack stack,
			final String... locations) {

		if (stack.isEmpty())
			return;

		final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!id.getNamespace().equals(MODID))
			return;

		final Relic relic = loader.find(id.getPath());
		if (relic == null)
			return;

		for (final String location : locations) {
			if (effectiveAt(relic, location)) {
				out.add(relic);
				return;
			}
		}
	}

	public static void applyRelicEffects(
			final LivingEntity livingEntity,
			final List<Relic> relics) {

		for (final Relic relic : relics) {
			for (final var entry : relic.add_effects.entrySet()) {
				final ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", entry.getKey());

				final var effect = BuiltInRegistries.MOB_EFFECT.get(id);

				if (effect == null)
					continue;

				final int amplifier = Math.max(0, entry.getValue() - 1);

				livingEntity.addEffect(new MobEffectInstance(
						BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), 240, amplifier, true, false
				));
			}
		}
	}

	/*
	Effects
	 */

	public static void removeImmuneEffects(
			final LivingEntity livingEntity,
			final List<Relic> relics) {

		final List<ResourceLocation> remove = new ArrayList<>();

		for (final MobEffectInstance instance : livingEntity.getActiveEffects()) {
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

			livingEntity.removeEffect(
					BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)
			);
		}
	}

	public static void addEffect(
			final LivingEntity target, final Holder<MobEffect> effect,
			final int duration, final int amplifier) {

		if (target.isDeadOrDying() ||
				!target.level().isLoaded(target.blockPosition())) {
			return;
		}

		final MobEffectInstance instance =
				new MobEffectInstance(effect, duration, amplifier, true, true);

		target.forceAddEffect(instance, null);
	}


	/*
	Special Abilities
	 */

	public static void metalMending(final LivingEntity entity, final int level) {

		if (level <= 0)
			return;

		int remaining = level;

		for (final ItemStack stack : entity.getAllSlots()) {
			if (remaining <= 0)
				break;

			if (!stack.isDamageableItem() || !stack.isDamaged())
				continue;

			final int repairAmount = Math.min(remaining, stack.getDamageValue());
			stack.setDamageValue(stack.getDamageValue() - repairAmount);
			remaining -= repairAmount;
		}

		if (entity instanceof Player player) {
			for (final ItemStack stack : player.getInventory().items) {
				if (remaining <= 0)
					break;

				if (!stack.isDamageableItem() || !stack.isDamaged())
					continue;

				final int repairAmount =
						Math.min(remaining, stack.getDamageValue());

				stack.setDamageValue(
						stack.getDamageValue() - repairAmount
				);

				remaining -= repairAmount;
			}
		}
	}

	public static void retargetArrow(AbstractArrow arrow, LivingEntity newOwner, final int damage_mul) {

		Entity owner = arrow.getOwner();

		// === Always bounce back, even if owner is null ===
		Vec3 currentPos = arrow.position();
		Vec3 direction;
		if (owner != null) {
			double distance = currentPos.distanceTo(owner.position());

			// Dynamic aim height - higher when target is farther away
			double baseHeight = owner.getEyeHeight();
			double extraHeight = Math.min(distance * 0.02, 4.0);   // increases with distance

			// Aim at eye level
			Vec3 ownerPos = owner.position().add(0, baseHeight + extraHeight, 0);
			direction = ownerPos.subtract(currentPos).normalize();
		} else {
			// No owner → just reverse current direction
			direction = arrow.getDeltaMovement().normalize().scale(-1.0);
		}

		// Vec3 direction = targetPos.subtract(currentPos).normalize();

		double minSpeed = 5f;
		double newSpeed = Math.max(arrow.getDeltaMovement().length(), minSpeed);

		Vec3 newVelocity = direction.scale(newSpeed);

		arrow.setDeltaMovement(newVelocity);
		arrow.hasImpulse = true;


		arrow.setOwner(newOwner);
		arrow.setBaseDamage(Math.max(arrow.getBaseDamage(),
				newOwner.getAttributeValue(Attributes.ATTACK_DAMAGE) * damage_mul));

		// Allow the arrow to continue flying after bounce


	}

	/*
	Allies or Enemies
	 */

	public static boolean hostileTargeted(LivingEntity self, LivingEntity other) {
		if (isAllied(self, other)) {
			return false;
		}

		if (other instanceof net.minecraft.world.entity.Mob mob) {
			var rtn = mob.getTarget();
			if (rtn == null) {
				return false;
			}
			return rtn.is(self);
		}

		return false;
	}


	public static boolean isAllied(LivingEntity live0, LivingEntity live1) {
		if (live0 == null || live1 == null || live0 == live1 || live0.getUUID() == live1.getUUID()) {
			return false;
		}

		// 1. Use the built-in isAlliedTo (covers scoreboard teams + some vanilla behaviors)
		if (live0.isAlliedTo(live1)) {
			return true;
		}

		// 2. Check if target is a tamed animal owned by base
		if (live1 instanceof TamableAnimal tamable && tamable.isTame()) {
			LivingEntity owner = tamable.getOwner();   // This is safe and preferred

			if (owner != null && owner.is(live0)) {     // owner.is(base) is better than ==
				return true;
			}
		}

		// Optional: Also check the other way around (if base is the pet and target is the owner)
		if (live0 instanceof TamableAnimal tamableBase && tamableBase.isTame()) {
			LivingEntity owner = tamableBase.getOwner();
			if (owner != null && owner.is(live1)) {
				return true;
			}
		}

		return false;
	}

}
