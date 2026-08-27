package com.example.vitalrelics;

import com.example.vitalrelics.common.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.vitalrelics.VitalRelics.loader;
import static com.example.vitalrelics.compat.TouhouMaidCompat.gatherMaidRelics;

public class Utils {

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

		// Relics assigned directly to spawned enemies.
		gatherEnemyRelics(entity, out);

		return out;
	}

	private static boolean effectiveAt(final Relic relic, final String location) {
		if (relic.effective_slots.isEmpty()) {
			return location.equals("in_curios_api_slots") ||
					location.equals("in_touhou_little_maid_curios_slots");
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
		if (!id.getNamespace().equals(Manifest.MODID))
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

				stack.setDamageValue(stack.getDamageValue() - repairAmount);
				remaining -= repairAmount;
			}
		}
	}

	public static void retargetArrow(
			final AbstractArrow arrow,
			final LivingEntity newOwner,
			final double damage_mul) {

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
		arrow.setBaseDamage(Math.max(
				arrow.getBaseDamage(),
				newOwner.getAttributeValue(Attributes.ATTACK_DAMAGE) * damage_mul
		));

		// Allow the arrow to continue flying after bounce
	}

	/*
	Add Effects
	 */

	public static void addEffect(
			final LivingEntity target,
			final MobEffect effect,
			final int dura,
			final int level) {

		if (target.isDeadOrDying() || !target.level().isLoaded(target.blockPosition())) {
			return;
		}

		try {
			// ambient visible
			MobEffectInstance effect_instance =
					new MobEffectInstance(effect, dura, level, true, true);
			target.forceAddEffect(effect_instance, null);
		} catch (ArrayIndexOutOfBoundsException e) {

		} catch (Exception e) {

		}
	}

	public static boolean hostileTargeted(
			final LivingEntity self,
			final LivingEntity other) {

		if (isAllied(self, other))
			return false;

		/*
		 * self is actively targeting other.
		 *
		 * This is important for relic-bearing hostile mobs:
		 * a zombie targeting a player must regard that player as hostile.
		 */
		if (self instanceof net.minecraft.world.entity.Mob mob) {
			final LivingEntity target = mob.getTarget();

			if (target != null && target.is(other))
				return true;
		}

		/*
		 * other is actively targeting self.
		 *
		 * This preserves the original behavior for players and other
		 * non-Mob relic bearers.
		 */
		if (other instanceof net.minecraft.world.entity.Mob mob) {
			final LivingEntity target = mob.getTarget();

			if (target != null && target.is(self))
				return true;

			// If it's a tamable animal, check its owner as well.
			if (other instanceof TamableAnimal tamable && tamable.isTame()) {
				final LivingEntity owner = tamable.getOwner();

				if (owner != null && hostileTargeted(self, owner))
					return true;
			}
		}

		return false;
	}

	public static boolean isAllied(
			final LivingEntity live0,
			final LivingEntity live1) {

		if (live0 == null || live1 == null || live0 == live1 ||
				live0.getUUID() == live1.getUUID()) {
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

	public static Component message(
			final String key,
			final String fallback,
			final Object... arguments) {

		final String pattern =
				RelicTranslations.INSTANCE.translate(key, fallback);

		return Component.literal(
				String.format(Locale.ROOT, pattern, arguments)
		);
	}

	/*
	Make relics used by enemies as well.
	 */

	public static void setEnemyRelics(
			final LivingEntity entity,
			final List<Relic> relics) {

		final var tag = entity.getPersistentData();
		final var list = new net.minecraft.nbt.ListTag();

		for (final Relic relic : relics)
			list.add(net.minecraft.nbt.StringTag.valueOf(relic.id));

		tag.put(Manifest.ENEMY_RELICS_TAG, list);
	}

	public static void gatherEnemyRelics(
			final LivingEntity entity,
			final List<Relic> out) {

		final var tag = entity.getPersistentData();

		if (!tag.contains(Manifest.ENEMY_RELICS_TAG))
			return;

		final var list = tag.getList(
				Manifest.ENEMY_RELICS_TAG,
				net.minecraft.nbt.Tag.TAG_STRING
		);

		for (int i = 0; i < list.size(); ++i) {
			final Relic relic = loader.find(list.getString(i));

			if (relic != null)
				out.add(relic);
		}
	}

	public static boolean enemyRelicsRolled(final LivingEntity entity) {
		return entity.getPersistentData()
				.getBoolean(Manifest.ENEMY_RELICS_ROLLED_TAG);
	}

	public static void markEnemyRelicsRolled(final LivingEntity entity) {
		entity.getPersistentData()
				.putBoolean(Manifest.ENEMY_RELICS_ROLLED_TAG, true);
	}

	// Visual effects for enemies with relics in this mod.
	private static double relicEnemyScore(final Relic relic) {
		return switch (relic.rarity) {
			case "common" -> 0.10;
			case "uncommon" -> 0.20;
			case "rare" -> 0.40;
			case "epic" -> 1.00;
			default -> 0.10;
		};
	}

	private static double enemyRelicScore(final List<Relic> relics) {
		double score = 0.0;

		for (final Relic relic : relics)
			score += relicEnemyScore(relic);

		return score;
	}

	public static boolean hasEnemyRelics(final LivingEntity entity) {
		final var tag = entity.getPersistentData();

		if (!tag.contains(Manifest.ENEMY_RELICS_TAG))
			return false;

		return !tag.getList(
				Manifest.ENEMY_RELICS_TAG,
				net.minecraft.nbt.Tag.TAG_STRING
		).isEmpty();
	}

	public static void spawnEnemyRelicParticles(
			final LivingEntity entity,
			final List<Relic> relics,
			final int currentTick) {

		if (!hasEnemyRelics(entity) || relics.isEmpty())
			return;

		if (!(entity.level() instanceof ServerLevel level))
			return;

		if (currentTick % 2 != 0)
			return;

		final double score = enemyRelicScore(relics);

		final int particleCount = 8;
		final double radius = Math.max(0.6, entity.getBbWidth() * 0.75);
		final double y = entity.getY() + entity.getBbHeight() + 0.35;
		final double rotation = currentTick * 0.12;

		for (int i = 0; i < particleCount; ++i) {
			final double angle =
					rotation +
							Math.PI * 2.0 * i / particleCount;

			final double x =
					entity.getX() + Math.cos(angle) * radius;

			final double z =
					entity.getZ() + Math.sin(angle) * radius;

			final org.joml.Vector3f color;

			if (score >= 1.0) {
				/*
				 * Extremely dangerous:
				 * alternate bloody red with almost-black crimson.
				 */
				color = (i & 1) == 0
						? new org.joml.Vector3f(0.45F, 0.01F, 0.01F)
						: new org.joml.Vector3f(0.10F, 0.005F, 0.005F);
			} else {
				/*
				 * 0.0 -> light pink
				 * ~0.3 -> light red
				 * ~0.7 -> darker red
				 * 1.0 -> bloody red
				 */
				final float t = (float) Math.max(0.0, Math.min(1.0, score));

				final float red = 1.0F - 0.55F * t;
				final float green = 0.65F * (1.0F - t);
				final float blue = 0.75F * (1.0F - t);

				color = new org.joml.Vector3f(
						red,
						green,
						blue
				);
			}

			level.sendParticles(
					new net.minecraft.core.particles.DustParticleOptions(color, 1.0F),
					x, y, z, 1, 0.0, 0.0, 0.0, 0.0
			);
		}
	}
}
