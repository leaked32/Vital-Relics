package com.example.vitalrelics.platform;

import com.example.vitalrelics.common.MyDamageInfo;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicTranslations;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MyRuntimeUtils;
import com.example.vitalrelics.network.SelectedSpellPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

import static com.example.vitalrelics.Utils.message;

public final class NeoRuntimeUtils implements MyRuntimeUtils {
	public static final NeoRuntimeUtils INSTANCE = new NeoRuntimeUtils();

	private NeoRuntimeUtils() {}

	private static NeoLivingEntity neo(final MyLivingEntity entity) {
		if (!(entity instanceof NeoLivingEntity neo))
			throw new IllegalArgumentException("Expected NeoLivingEntity");

		return neo;
	}

	private static LivingEntity nativeEntity(final MyLivingEntity entity) {
		return neo(entity).nativeEntity();
	}

	private static ServerPlayer player(final MyLivingEntity entity) {
		final LivingEntity nativeEntity = nativeEntity(entity);
		return nativeEntity instanceof ServerPlayer player ? player : null;
	}

	private static String spellName(final String id) {
		return RelicTranslations.INSTANCE.translate(
				"relic.vitalrelics.spell." + id,
				Relic.itemDisplayName(id)
		);
	}

	@Override
	public List<Relic> gatherRelics(final MyLivingEntity caster) {
		return com.example.vitalrelics.Utils.gatherRelics(nativeEntity(caster));
	}

	@Override
	public void syncSpellHud(
			final MyLivingEntity caster,
			final String spellId,
			final int cooldownTicks) {

		final ServerPlayer player = player(caster);
		if (player == null)
			return;

		PacketDistributor.sendToPlayer(
				player,
				new SelectedSpellPayload(spellId, cooldownTicks)
		);
	}

	@Override
	public void clearSpellHud(final MyLivingEntity caster) {
		final ServerPlayer player = player(caster);
		if (player == null)
			return;

		PacketDistributor.sendToPlayer(
				player,
				new SelectedSpellPayload("", 0)
		);
	}

	@Override
	public void showSelectedSpell(
			final MyLivingEntity caster,
			final String spellId) {

		final ServerPlayer player = player(caster);
		if (player == null)
			return;

		player.displayClientMessage(
				message(
						"message.vitalrelics.selected_spell",
						"Selected spell: %s",
						spellName(spellId)
				),
				true
		);
	}

	@Override
	public void showSpellCooldown(
			final MyLivingEntity caster,
			final String spellId,
			final int remainingTicks) {

		final ServerPlayer player = player(caster);
		if (player == null)
			return;

		player.displayClientMessage(
				message(
						"message.vitalrelics.spell_cooldown",
						"%s cooldown: %s",
						spellName(spellId),
						String.format(
								Locale.ROOT,
								"%.1fs",
								remainingTicks / 20.0
						)
				),
				true
		);
	}

	@Override
	public void showCurseRequiresTarget(final MyLivingEntity caster) {
		final ServerPlayer player = player(caster);
		if (player == null)
			return;

		player.displayClientMessage(
				message(
						"message.vitalrelics.curse_requires_target",
						"Curse requires a target."
				),
				true
		);
	}

	@Override
	public boolean teleportAlongLook(
			final MyLivingEntity caster,
			final double distance) {

		final LivingEntity entity = nativeEntity(caster);
		if (!(entity.level() instanceof ServerLevel level))
			return false;

		final Vec3 origin = entity.position();
		final Vec3 rayOrigin = entity.getEyePosition();
		final Vec3 direction = entity.getLookAngle().normalize();
		final Vec3 rayEnd = rayOrigin.add(direction.scale(distance));

		/*
		 * Raycast while treating snow as transparent.
		 */
		Vec3 clipStart = rayOrigin;
		BlockHitResult hit;

		while (true) {
			hit = level.clip(new ClipContext(
					clipStart,
					rayEnd,
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					entity
			));

			if (hit.getType() != HitResult.Type.BLOCK)
				break;

			if (!level.getBlockState(hit.getBlockPos()).is(Blocks.SNOW))
				break;

			/*
			 * Move slightly beyond the snow collision and continue the ray.
			 */
			clipStart = hit.getLocation().add(direction.scale(0.01));

			if (clipStart.distanceToSqr(rayOrigin) >= rayEnd.distanceToSqr(rayOrigin)) {
				hit = BlockHitResult.miss(
						rayEnd,
						Direction.getNearest(direction.x, direction.y, direction.z),
						BlockPos.containing(rayEnd)
				);
				break;
			}
		}

		/*
		 * Sky / no block:
		 * teleport as far along the look direction as possible.
		 */
		if (hit.getType() == HitResult.Type.MISS) {
			for (double travelled = distance; travelled >= 0.5; travelled -= 0.25) {
				final Vec3 candidate = origin.add(direction.scale(travelled));
				final AABB targetBox =
						entity.getBoundingBox().move(candidate.subtract(origin));

				if (!level.noCollision(entity, targetBox))
					continue;

				caster.teleport(candidate.x, candidate.y, candidate.z);
				return true;
			}
			return false;
		}

		final BlockPos target = hit.getBlockPos();
		final BlockState state = level.getBlockState(target);

		/*
		 * Thin blocks:
		 * teleport into the center of their block cell.
		 */
		final boolean centerTarget =
				state.getBlock() instanceof DoorBlock ||
				state.getBlock() instanceof TrapDoorBlock ||
				state.getBlock() instanceof IronBarsBlock ||
				state.getBlock() instanceof StainedGlassPaneBlock;

		if (centerTarget) {
			caster.teleport(
					target.getX() + 0.5,
					target.getY(),
					target.getZ() + 0.5
			);
			return true;
		}

		/*
		 * Normal block:
		 * try standing on its actual collision surface.
		 */
		final VoxelShape shape = state.getCollisionShape(level, target);

		if (!shape.isEmpty()) {
			final double topY = target.getY() + shape.max(Direction.Axis.Y);
			final Vec3 above = new Vec3(
					target.getX() + 0.5,
					topY + 1.0e-4,
					target.getZ() + 0.5
			);
			final AABB targetBox =
					entity.getBoundingBox().move(above.subtract(origin));

			if (level.noCollision(entity, targetBox)) {
				caster.teleport(above.x, above.y, above.z);
				return true;
			}
		}

		/*
		 * No room above:
		 * teleport BEFORE the target block.
		 *
		 * hit.getDirection() points toward the side from which the ray
		 * entered the target block, so this is the caster-facing side.
		 */
		final Direction beforeDirection = hit.getDirection();
		final BlockPos before = target.relative(beforeDirection);

		Vec3 candidate = new Vec3(
				before.getX() + 0.5,
				before.getY(),
				before.getZ() + 0.5
		);

		/*
		 * Move a tiny amount farther away from the target block to avoid
		 * floating-point boundary overlap with its collision shape.
		 */
		candidate = candidate.add(
				beforeDirection.getStepX() * 1.0e-4,
				beforeDirection.getStepY() * 1.0e-4,
				beforeDirection.getStepZ() * 1.0e-4
		);

		final AABB targetBox =
				entity.getBoundingBox().move(candidate.subtract(origin));

		if (!level.noCollision(entity, targetBox))
			return false;

		caster.teleport(candidate.x, candidate.y, candidate.z);
		return true;
	}

	@Override
	public MyLivingEntity pointedLivingEntity(
			final MyLivingEntity caster,
			final double range) {

		final LivingEntity nativeCaster = nativeEntity(caster);
		if (!(nativeCaster.level() instanceof ServerLevel level))
			return null;

		final Vec3 origin = nativeCaster.getEyePosition();
		final Vec3 direction = nativeCaster.getLookAngle().normalize();

		final BlockHitResult blockHit = level.clip(new ClipContext(
				origin,
				origin.add(direction.scale(range)),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				nativeCaster
		));

		final double visibleRange =
				blockHit.getType() == HitResult.Type.BLOCK
						? origin.distanceTo(blockHit.getLocation())
						: range;

		final Vec3 end = origin.add(direction.scale(visibleRange));
		final AABB searchBox =
				nativeCaster.getBoundingBox()
						.expandTowards(direction.scale(visibleRange))
						.inflate(1.0);

		LivingEntity selected = null;
		double selectedDistance = Double.MAX_VALUE;

		for (final LivingEntity candidate :
				level.getEntitiesOfClass(
						LivingEntity.class,
						searchBox,
						entity -> entity != nativeCaster && entity.isAlive()
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

		return selected == null ? null : new NeoLivingEntity(selected);
	}


	@Override
	public boolean shadowExchange(
			final MyLivingEntity caster,
			final double range) {

		final LivingEntity nativeCaster = nativeEntity(caster);
		if (!(nativeCaster.level() instanceof ServerLevel level))
			return false;

		final MyLivingEntity target = pointedLivingEntity(caster, range);

		if (target == null || caster.isAllied(target))
			return false;

		final LivingEntity nativeTarget = nativeEntity(target);
		final Vec3 casterPosition = nativeCaster.position();
		final Vec3 targetPosition = nativeTarget.position();

		caster.teleport(targetPosition.x, targetPosition.y, targetPosition.z);
		target.teleport(casterPosition.x, casterPosition.y, casterPosition.z);

		level.playSound(
				null,
				casterPosition.x, casterPosition.y, casterPosition.z,
				SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS,
				0.8F, 0.8F
		);

		level.playSound(
				null,
				targetPosition.x, targetPosition.y, targetPosition.z,
				SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS,
				0.8F, 1.2F
		);

		return true;
	}

	@Override
	public boolean phantomStep(
			final MyLivingEntity caster,
			final double range,
			final float intensity) {

		final LivingEntity nativeCaster = nativeEntity(caster);
		if (!(nativeCaster.level() instanceof ServerLevel level))
			return false;

		final Vec3 look = nativeCaster.getLookAngle();
		final Vec3 horizontal = new Vec3(look.x, 0.0, look.z);

		if (horizontal.lengthSqr() <= 1.0e-8)
			return false;

		final Vec3 direction = horizontal.normalize();
		final Vec3 origin = nativeCaster.position();

		/*
		 * Stop the step at the first solid block.
		 */
		final Vec3 rayOrigin = nativeCaster.getEyePosition();
		final Vec3 rayEnd = rayOrigin.add(direction.scale(range));

		final BlockHitResult hit = level.clip(new ClipContext(
				rayOrigin,
				rayEnd,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				nativeCaster
		));

		double travel = range;

		if (hit.getType() == HitResult.Type.BLOCK) {
			travel = Math.max(
					0.0,
					rayOrigin.distanceTo(hit.getLocation()) -
							nativeCaster.getBbWidth() * 0.5 -
							0.05
			);
		}

		if (travel <= 0.0)
			return false;

		/*
		 * Find the farthest collision-free destination before the obstacle.
		 */
		Vec3 destination = null;

		for (double distance = travel; distance >= 0.25; distance -= 0.25) {
			final Vec3 candidate = origin.add(direction.scale(distance));
			final AABB targetBox =
					nativeCaster.getBoundingBox().move(candidate.subtract(origin));

			if (level.noCollision(nativeCaster, targetBox)) {
				destination = candidate;
				break;
			}
		}

		if (destination == null)
			return false;

		final float damage = caster.attackDamage() * intensity;

		/*
		 * Damage only hostile entities whose hitboxes intersect the actual
		 * movement line.
		 */
		final AABB sweptArea =
				nativeCaster.getBoundingBox()
						.expandTowards(destination.subtract(origin))
						.inflate(1.0);

		for (final LivingEntity target :
				level.getEntitiesOfClass(LivingEntity.class, sweptArea)) {

			if (target == nativeCaster)
				continue;

			final MyLivingEntity myTarget = new NeoLivingEntity(target);

			if (!caster.hostileTargeted(myTarget))
				continue;

			final AABB hitbox =
					target.getBoundingBox().inflate(nativeCaster.getBbWidth() * 0.5);

			if (hitbox.clip(origin, destination).isEmpty())
				continue;

			MyDamageInfo.directAttack(caster, myTarget, damage, 1);
		}

		caster.teleport(destination.x, destination.y, destination.z);

		level.playSound(
				null,
				origin.x, origin.y, origin.z,
				SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS,
				0.6F, 1.5F
		);

		return true;
	}

	@Override
	public boolean upgradeEnchantedBook(
			final MyLivingEntity caster,
			final int experienceCost) {

		final ServerPlayer player = player(caster);
		if (player == null)
			return false;

		final ItemStack stack = player.getMainHandItem();

		if (!stack.is(Items.ENCHANTED_BOOK))
			return false;

		if (!player.isCreative() && player.experienceLevel < experienceCost) {
			player.displayClientMessage(
					message(
							"message.vitalrelics.enchant_upgrade_insufficient_experience",
							"Not enough experience. Required level: %s",
							experienceCost
					),
					true
			);
			return false;
		}

		final ItemEnchantments enchantments =
				stack.getOrDefault(
						DataComponents.STORED_ENCHANTMENTS,
						ItemEnchantments.EMPTY
				);

		final ItemEnchantments.Mutable mutable =
				new ItemEnchantments.Mutable(enchantments);

		int upgradedLevel = 0;

		for (final var enchantment : enchantments.keySet()) {
			final int level = enchantments.getLevel(enchantment);
			final int maximum = enchantment.value().getMaxLevel();

			if (level >= maximum)
				continue;

			upgradedLevel = level + 1;
			mutable.set(enchantment, upgradedLevel);
			break;
		}

		if (upgradedLevel == 0)
			return false;

		stack.set(
				DataComponents.STORED_ENCHANTMENTS,
				mutable.toImmutable()
		);

		if (!player.isCreative())
			player.giveExperienceLevels(-experienceCost);

		player.displayClientMessage(
				message(
						"message.vitalrelics.enchant_upgrade_success",
						"Enchantment upgraded to level %s.",
						upgradedLevel
				),
				true
		);

		player.level().playSound(
				null,
				player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENCHANTMENT_TABLE_USE,
				SoundSource.PLAYERS,
				1.0F, 1.0F
		);

		return true;
	}
}
