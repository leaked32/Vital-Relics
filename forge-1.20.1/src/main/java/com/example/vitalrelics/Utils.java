package com.example.vitalrelics;


import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import com.example.vitalrelics.common.RelicTranslations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
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

	public static void removeImmuneEffects(
			final LivingEntity livingEntity,
			final List<Relic> relics) {

		final List<MobEffect> remove = new ArrayList<>();

		for (final MobEffectInstance instance : livingEntity.getActiveEffects()) {
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
			livingEntity.removeEffect(effect);
	}

	public static void applyRelicEffects(
			final LivingEntity livingEntity,
			final List<Relic> relics) {

		for (final Relic relic : relics) {
			for (final var entry : relic.granted_effects.entrySet()) {
				final ResourceLocation id = new ResourceLocation("minecraft", entry.getKey());
				final MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);

				if (effect == null) continue;

				final int amplifier = Math.max(0, entry.getValue() - 1);

				livingEntity.addEffect(
						new MobEffectInstance(effect, 240, amplifier, true, false
						)
				);
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

				stack.setDamageValue(
						stack.getDamageValue() - repairAmount
				);

				remaining -= repairAmount;
			}
		}
	}

	public static void retargetArrow(AbstractArrow arrow, LivingEntity newOwner, final double damage_mul) {

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
	Add Effects
	 */

	public static void addEffect(LivingEntity target, MobEffect effect, int dura, int level) {

		if (target.isDeadOrDying() || !target.level().isLoaded(target.blockPosition())) {
			return;
		}

		try {
			// ambient visible
			MobEffectInstance effect_instance = new MobEffectInstance(effect, dura, level, true, true);
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


	public static void teleport(
			final LivingEntity entity,
			final ServerLevel level,
			final Vec3 destination) {

		if (entity instanceof ServerPlayer player) {
			player.teleportTo(
					level,
					destination.x, destination.y, destination.z,
					player.getYRot(), player.getXRot()
			);
		} else {
			entity.teleportTo(
					destination.x, destination.y, destination.z
			);
		}

		level.playSound(
				null,
				entity.getX(), entity.getY(), entity.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
				0.8F, 1.15F
		);
	}

	public static LivingEntity pointedLivingEntity(
			final LivingEntity caster,
			final ServerLevel level,
			final double range) {

		final Vec3 origin = caster.getEyePosition();
		final Vec3 direction = caster.getLookAngle().normalize();

		final BlockHitResult blockHit = level.clip(new ClipContext(
				origin,
				origin.add(direction.scale(range)),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				caster
		));

		final double visibleRange = blockHit.getType() == HitResult.Type.BLOCK
				? origin.distanceTo(blockHit.getLocation())
				: range;

		final Vec3 end = origin.add(direction.scale(visibleRange));
		final AABB searchBox = caster.getBoundingBox()
				.expandTowards(direction.scale(visibleRange))
				.inflate(1.0);

		LivingEntity selected = null;
		double selectedDistance = Double.MAX_VALUE;

		for (final LivingEntity candidate : level.getEntitiesOfClass(
				LivingEntity.class,
				searchBox,
				entity -> entity != caster && entity.isAlive()
		)) {
			final Vec3 hit = candidate.getBoundingBox()
					.inflate(candidate.getPickRadius())
					.clip(origin, end)
					.orElse(null);

			if (hit == null)
				continue;

			final double distance = origin.distanceToSqr(hit);

			if (distance < selectedDistance) {
				selected = candidate;
				selectedDistance = distance;
			}
		}

		return selected;
	}



	private static BlockHitResult clipTeleportRay(
			ServerLevel level,
			Entity caster,
			Vec3 start,
			Vec3 end
	) {
		final Vec3 direction = end.subtract(start).normalize();
		Vec3 current = start;

		for (int i = 0; i < 64; ++i) {
			final BlockHitResult hit = level.clip(
					new ClipContext(
							current, end,
							ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
							caster
					)
			);

			if (hit.getType() != HitResult.Type.BLOCK)
				return hit;

			final BlockPos pos = hit.getBlockPos();

			/*
			 * Snow layers have collision shapes, so COLLIDER normally stops here.
			 * For teleport targeting, they should be transparent.
			 */
			if (!level.getBlockState(pos).is(Blocks.SNOW))
				return hit;

			/*
			 * Advance past this entire voxel instead of merely adding an epsilon.
			 * Otherwise a multi-layer snow block can immediately be hit again.
			 */
			final Vec3 p = hit.getLocation();

			double advance = Double.POSITIVE_INFINITY;

			if (direction.x > 0.0)
				advance = Math.min(advance, (pos.getX() + 1.0 - p.x) / direction.x);
			else if (direction.x < 0.0)
				advance = Math.min(advance, (pos.getX() - p.x) / direction.x);

			if (direction.y > 0.0)
				advance = Math.min(advance, (pos.getY() + 1.0 - p.y) / direction.y);
			else if (direction.y < 0.0)
				advance = Math.min(advance, (pos.getY() - p.y) / direction.y);

			if (direction.z > 0.0)
				advance = Math.min(advance, (pos.getZ() + 1.0 - p.z) / direction.z);
			else if (direction.z < 0.0)
				advance = Math.min(advance, (pos.getZ() - p.z) / direction.z);

			if (!Double.isFinite(advance))
				break;

			current = p.add(direction.scale(advance + 1.0e-4));

			if (current.distanceToSqr(start) >= end.distanceToSqr(start))
				break;
		}

		return BlockHitResult.miss(
				end,
				Direction.getNearest(
						direction.x,
						direction.y,
						direction.z
				),
				BlockPos.containing(end)
		);
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
}
