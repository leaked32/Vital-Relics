package com.example.vitalrelics.platform;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Translations;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MyRuntimeUtils;
import com.example.vitalrelics.common.utils.MyVec3;
import com.example.vitalrelics.network.ForgeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.example.vitalrelics.Utils.message;

public final class ForgeRuntimeUtils implements MyRuntimeUtils {
	public static final ForgeRuntimeUtils INSTANCE = new ForgeRuntimeUtils();

	private ForgeRuntimeUtils() {}

	@Override
	public void log(final String message) {
		VitalRelics.LOGGER.info(message);
	}

	private static ForgeLivingEntity forge(final MyLivingEntity entity) {
		if (!(entity instanceof ForgeLivingEntity forge))
			throw new IllegalArgumentException("Expected ForgeLivingEntity");

		return forge;
	}

	private static LivingEntity nativeEntity(final MyLivingEntity entity) {
		return forge(entity).nativeEntity();
	}

	private static ServerPlayer player(final MyLivingEntity entity) {
		final LivingEntity nativeEntity = nativeEntity(entity);

		if (!(nativeEntity instanceof ServerPlayer player))
			return null;

		return player;
	}

	private static String spellName(final String id) {
		return Translations.get().translate(
				"relic.vitalrelics.spell." + id,
				Relic.itemDisplayName(id)
		);
	}

	private static MyVec3 wrap(final Vec3 value) {
		return new MyVec3(value.x, value.y, value.z);
	}

	@Override
	public List<Relic> gatherRelics(final MyLivingEntity entity) {
		return com.example.vitalrelics.Utils.gatherRelics(
				nativeEntity(entity)
		);
	}

	@Override
	public MyLivingEntity pointedLivingEntity(
			final MyLivingEntity source,
			final double range) {

		final LivingEntity entity = nativeEntity(source);

		if (!(entity.level() instanceof ServerLevel level))
			return null;

		final Vec3 origin = entity.getEyePosition();
		final Vec3 direction = entity.getLookAngle().normalize();

		final BlockHitResult blockHit = level.clip(new ClipContext(
				origin,
				origin.add(direction.scale(range)),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				entity
		));

		final double visibleRange =
				blockHit.getType() == HitResult.Type.BLOCK
						? origin.distanceTo(blockHit.getLocation())
						: range;

		final Vec3 end = origin.add(direction.scale(visibleRange));
		final AABB searchBox = entity.getBoundingBox()
				.expandTowards(direction.scale(visibleRange))
				.inflate(1.0);

		LivingEntity selected = null;
		double selectedDistance = Double.MAX_VALUE;

		for (final LivingEntity candidate : level.getEntitiesOfClass(
				LivingEntity.class,
				searchBox,
				target -> target != entity && target.isAlive()
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

		return selected == null
				? null
				: new ForgeLivingEntity(selected);
	}

	@Override
	public MyVec3 safeDestinationAlongLook(
			final MyLivingEntity abstractEntity,
			final double range) {

		final LivingEntity entity = nativeEntity(abstractEntity);

		if (!(entity.level() instanceof ServerLevel level))
			return null;

		final Vec3 origin = entity.position();
		final Vec3 rayOrigin = entity.getEyePosition();
		final Vec3 direction = entity.getLookAngle().normalize();
		final Vec3 rayEnd = rayOrigin.add(direction.scale(range));

		final BlockHitResult hit =
				clipTeleportRay(level, entity, rayOrigin, rayEnd);

		if (hit.getType() == HitResult.Type.MISS) {
			for (double travelled = range;
			     travelled >= 0.5;
			     travelled -= 0.25) {

				final Vec3 candidate =
						origin.add(direction.scale(travelled));

				final AABB targetBox =
						entity.getBoundingBox()
								.move(candidate.subtract(origin));

				if (level.noCollision(entity, targetBox))
					return wrap(candidate);
			}

			return null;
		}

		final BlockPos target = hit.getBlockPos();
		final BlockState state = level.getBlockState(target);

		final boolean centerTarget =
				state.getBlock() instanceof DoorBlock ||
						state.getBlock() instanceof TrapDoorBlock ||
						state.getBlock() instanceof IronBarsBlock ||
						state.getBlock() instanceof StainedGlassPaneBlock;

		if (centerTarget) {
			return new MyVec3(
					target.getX() + 0.5,
					target.getY(),
					target.getZ() + 0.5
			);
		}

		final VoxelShape shape =
				state.getCollisionShape(level, target);

		if (!shape.isEmpty()) {
			final double topY =
					target.getY() + shape.max(Direction.Axis.Y);

			final Vec3 above = new Vec3(
					target.getX() + 0.5,
					topY + 1.0e-4,
					target.getZ() + 0.5
			);

			final AABB targetBox =
					entity.getBoundingBox()
							.move(above.subtract(origin));

			if (level.noCollision(entity, targetBox))
				return wrap(above);
		}

		final Direction beforeDirection = hit.getDirection();
		final BlockPos before = target.relative(beforeDirection);

		final Vec3 candidate = new Vec3(
				before.getX() + 0.5 +
						beforeDirection.getStepX() * 1.0e-4,
				before.getY() +
						beforeDirection.getStepY() * 1.0e-4,
				before.getZ() + 0.5 +
						beforeDirection.getStepZ() * 1.0e-4
		);

		final AABB targetBox =
				entity.getBoundingBox()
						.move(candidate.subtract(origin));

		return level.noCollision(entity, targetBox)
				? wrap(candidate)
				: null;
	}

	private static BlockHitResult clipTeleportRay(
			final ServerLevel level,
			final Entity caster,
			final Vec3 start,
			final Vec3 end) {

		final Vec3 direction = end.subtract(start).normalize();
		Vec3 current = start;

		for (int i = 0; i < 64; ++i) {
			final BlockHitResult hit = level.clip(
					new ClipContext(
							current,
							end,
							ClipContext.Block.COLLIDER,
							ClipContext.Fluid.NONE,
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
				Direction.getNearest(direction.x, direction.y, direction.z),
				BlockPos.containing(end)
		);
	}

	@Override
	public MyVec3 safeHorizontalDestination(
			final MyLivingEntity abstractEntity,
			final double range) {

		final LivingEntity entity = nativeEntity(abstractEntity);

		if (!(entity.level() instanceof ServerLevel level))
			return null;

		final Vec3 look = entity.getLookAngle();
		final Vec3 horizontal = new Vec3(look.x, 0.0, look.z);

		if (horizontal.lengthSqr() <= 1.0e-8)
			return null;

		final Vec3 direction = horizontal.normalize();
		final Vec3 origin = entity.position();
		final Vec3 rayOrigin = entity.getEyePosition();
		final Vec3 rayEnd = rayOrigin.add(direction.scale(range));

		final BlockHitResult hit = level.clip(new ClipContext(
				rayOrigin,
				rayEnd,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				entity
		));

		double travel = range;

		if (hit.getType() == HitResult.Type.BLOCK) {
			travel = Math.max(
					0.0,
					rayOrigin.distanceTo(hit.getLocation()) -
							entity.getBbWidth() * 0.5 -
							0.05
			);
		}

		if (travel <= 0.0)
			return null;

		for (double distance = travel;
		     distance >= 0.25;
		     distance -= 0.25) {

			final Vec3 candidate = origin.add(direction.scale(distance));
			final AABB targetBox =
					entity.getBoundingBox().move(candidate.subtract(origin));

			if (level.noCollision(entity, targetBox))
				return wrap(candidate);
		}

		return null;
	}

	@Override
	public List<MyLivingEntity> entitiesIntersectingMovement(
			final MyLivingEntity abstractEntity,
			final MyVec3 from,
			final MyVec3 to,
			final double inflate) {

		final LivingEntity entity = nativeEntity(abstractEntity);

		if (!(entity.level() instanceof ServerLevel level))
			return List.of();

		final Vec3 origin = new Vec3(from.x(), from.y(), from.z());
		final Vec3 destination = new Vec3(to.x(), to.y(), to.z());

		final AABB sweptArea = entity.getBoundingBox()
				.expandTowards(destination.subtract(origin))
				.inflate(1.0);

		return level.getEntitiesOfClass(
						LivingEntity.class,
						sweptArea,
						target -> {
							if (target == entity)
								return false;

							final AABB hitbox =
									target.getBoundingBox().inflate(inflate);

							return hitbox.clip(origin, destination).isPresent();
						}
				)
				.stream()
				.map(ForgeLivingEntity::new)
				.map(MyLivingEntity.class::cast)
				.toList();
	}
	@Override
	public boolean upgradeFirstEnchantment(
			final MyLivingEntity abstractEntity,
			final int experienceCost,
			final EnchantmentFilter option) {

		final LivingEntity entity = nativeEntity(abstractEntity);

		if (!(entity instanceof ServerPlayer player))
			return false;

		final ItemStack stack = player.getMainHandItem();

		if (option == EnchantmentFilter.ENCHANTMENT_BOOK_ONLY &&
				!stack.is(Items.ENCHANTED_BOOK)) {

			player.displayClientMessage(
					message(
							"message.vitalrelics.enchantment_book_required",
							"Hold an enchanted book in your main hand."
					),
					true
			);

			return false;
		}

		final Map<Enchantment, Integer> enchantments =
				EnchantmentHelper.getEnchantments(stack);

		if (enchantments.isEmpty()) {
			player.displayClientMessage(
					message(
							"message.vitalrelics.item_not_enchanted",
							"The held item is not enchanted."
					),
					true
			);

			return false;
		}

		Enchantment selected = null;
		int selectedLevel = 0;

		for (final var entry : enchantments.entrySet()) {
			final Enchantment enchantment = entry.getKey();
			final int level = entry.getValue();

			if (level >= enchantment.getMaxLevel())
				continue;

			selected = enchantment;
			selectedLevel = level;
			break;
		}

		if (selected == null) {
			player.displayClientMessage(
					message(
							"message.vitalrelics.enchantments_at_maximum",
							"Every enchantment is already at its maximum level."
					),
					true
			);

			return false;
		}

		if (!player.isCreative() &&
				player.experienceLevel < experienceCost) {

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

		final int upgradedLevel = selectedLevel + 1;

		enchantments.put(selected, upgradedLevel);
		EnchantmentHelper.setEnchantments(enchantments, stack);

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

		return true;
	}

	@Override
	public boolean removeCurse(
			final MyLivingEntity abstractEntity,
			final int experienceCost) {

		final LivingEntity entity = nativeEntity(abstractEntity);

		if (!(entity instanceof ServerPlayer player))
			return false;

		final ItemStack stack = player.getMainHandItem();

		final Map<Enchantment, Integer> enchantments =
				EnchantmentHelper.getEnchantments(stack);

		Enchantment curse = null;

		for (final Enchantment enchantment : enchantments.keySet()) {
			if (enchantment.isCurse()) {
				curse = enchantment;
				break;
			}
		}

		if (curse == null) {
			player.displayClientMessage(
					message(
							"message.vitalrelics.no_curse",
							"The held item has no curse to remove."
					),
					true
			);

			return false;
		}

		if (!player.isCreative() &&
				player.experienceLevel < experienceCost) {

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

		enchantments.remove(curse);
		EnchantmentHelper.setEnchantments(enchantments, stack);

		if (!player.isCreative())
			player.giveExperienceLevels(-experienceCost);

		return true;
	}

	@Override
	public boolean resetRepairCost(
			final MyLivingEntity abstractEntity,
			final int experienceCost) {

		final LivingEntity entity = nativeEntity(abstractEntity);

		if (!(entity instanceof ServerPlayer player))
			return false;

		final ItemStack stack = player.getMainHandItem();

		if (stack.getBaseRepairCost() <= 0) {
			player.displayClientMessage(
					message(
							"message.vitalrelics.no_anvil_penalty",
							"The held item has no anvil penalty to remove."
					),
					true
			);

			return false;
		}

		if (!player.isCreative() &&
				player.experienceLevel < experienceCost) {

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

		stack.setRepairCost(0);

		if (!player.isCreative())
			player.giveExperienceLevels(-experienceCost);

		return true;
	}

	@Override
	public void syncSpellHud(
			final MyLivingEntity caster,
			final String spellId,
			final int cooldownTicks) {

		final ServerPlayer player = player(caster);

		if (player == null)
			return;

		ForgeNetwork.sendSpellHud(player, spellId, cooldownTicks);
	}

	@Override
	public void clearSpellHud(final MyLivingEntity caster) {
		final ServerPlayer player = player(caster);

		if (player == null)
			return;

		ForgeNetwork.sendSpellHud(player, "", 0);
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
	public void showMessage(
			final MyLivingEntity caster,
			final String key,
			final String fallback) {

		final ServerPlayer player = player(caster);

		if (player == null)
			return;

		player.displayClientMessage(message(key, fallback), true);
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
	public void summonVisualLightning(final MyLivingEntity target) {
		final LivingEntity entity = nativeEntity(target);

		if (!(entity.level() instanceof ServerLevel level))
			return;

		final LightningBolt lightning =
				EntityType.LIGHTNING_BOLT.create(level);

		if (lightning == null)
			return;

		lightning.setVisualOnly(true);
		lightning.moveTo(
				entity.getX(),
				entity.getY(),
				entity.getZ()
		);

		level.addFreshEntity(lightning);
	}


	@Override
	public boolean isEntityValid(final UUID uuid) {
		final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

		if (server == null)
			return false;

		for (final ServerLevel level : server.getAllLevels()) {
			final Entity entity = level.getEntity(uuid);

			if (entity instanceof LivingEntity livingEntity &&
					!livingEntity.isRemoved() &&
					!livingEntity.isDeadOrDying())
				return true;
		}

		return false;
	}

	@Override
	public boolean disenchantToOffhandBook(
			final MyLivingEntity abstractEntity,
			final int experienceCost) {

		final LivingEntity entity = nativeEntity(abstractEntity);

		if (!(entity instanceof ServerPlayer player))
			return false;

		final ItemStack stack = player.getMainHandItem();
		final ItemStack offhand = player.getOffhandItem();

		if (!offhand.is(Items.BOOK)) {
			player.displayClientMessage(
					message(
							"message.vitalrelics.disenchantment_book_required",
							"Hold a book in your off hand."
					),
					true
			);
			return false;
		}

		final Map<Enchantment, Integer> enchantments =
				EnchantmentHelper.getEnchantments(stack);

		if (enchantments.isEmpty()) {
			player.displayClientMessage(
					message(
							"message.vitalrelics.item_not_enchanted",
							"The held item is not enchanted."
					),
					true
			);
			return false;
		}

		Enchantment selected = null;
		int selectedLevel = 0;

		for (final var entry : enchantments.entrySet()) {
			final Enchantment enchantment = entry.getKey();

			if (enchantment.isCurse())
				continue;

			selected = enchantment;
			selectedLevel = entry.getValue();
			break;
		}

		if (selected == null) {
			player.displayClientMessage(
					message(
							"message.vitalrelics.disenchantment_no_removable_enchantment",
							"The held item has no removable enchantment."
					),
					true
			);
			return false;
		}

		if (!player.isCreative() &&
				player.experienceLevel < experienceCost) {

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

		enchantments.remove(selected);
		EnchantmentHelper.setEnchantments(enchantments, stack);

		final ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		book.enchant(selected, selectedLevel);

		if (!player.isCreative())
			offhand.shrink(1);

		if (!player.getInventory().add(book))
			player.drop(book, false);

		if (!player.isCreative())
			player.giveExperienceLevels(-experienceCost);

		player.displayClientMessage(
				message(
						"message.vitalrelics.disenchantment_success",
						"Enchantment transferred to the book."
				),
				true
		);

		return true;
	}
}
