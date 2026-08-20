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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

import static com.example.vitalrelics.VitalRelics.MODID;
import static com.example.vitalrelics.VitalRelics.loader;

public class Utils {

	public static List<Relic> gatherRelics(final Player player) {
		final List<Relic> relicList = new ArrayList<>();

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

	public static void removeImmuneEffects(
			final Player player,
			final List<Relic> relics) {

		final List<MobEffect> remove = new ArrayList<>();

		for (final MobEffectInstance instance : player.getActiveEffects()) {
			final MobEffect effect = instance.getEffect();

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
				remove.add(effect);
			}
		}

		for (final MobEffect effect : remove)
			player.removeEffect(effect);
	}

	public static void applyRelicEffects(
			final Player player,
			final List<Relic> relics) {

		for (final Relic relic : relics) {
			for (final var entry : relic.add_effects.entrySet()) {
				final ResourceLocation id = new ResourceLocation("minecraft", entry.getKey());
				final MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);

				if (effect == null) continue;

				final int amplifier = Math.max(0, entry.getValue() - 1);

				player.addEffect(
						new MobEffectInstance(effect, 240, amplifier, true, false
						)
				);
			}
		}
	}

	public static void retargetArrow(AbstractArrow arrow, LivingEntity newOwner) {

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
				newOwner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F));

		// Allow the arrow to continue flying after bounce


	}

}
