package com.example.vitalrelics.platform;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Translations;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MyRuntimeUtils;
import com.example.vitalrelics.common.utils.MyVec3;
import com.example.vitalrelics.network.NeoNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
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
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.example.vitalrelics.Utils.message;

public final class NeoRuntimeUtils implements MyRuntimeUtils {
	public static final NeoRuntimeUtils INSTANCE = new NeoRuntimeUtils();

	private NeoRuntimeUtils() {}

	@Override
	public void log(final String message) {
		VitalRelics.LOGGER.info(message);
	}

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

		final Vec3 end =
				origin.add(direction.scale(visibleRange));

		final AABB searchBox =
				entity.getBoundingBox()
						.expandTowards(direction.scale(visibleRange))
						.inflate(1.0);

		LivingEntity selected = null;
		double selectedDistance = Double.MAX_VALUE;

		for (final LivingEntity candidate :
				level.getEntitiesOfClass(
						LivingEntity.class,
						searchBox,
						target -> target != entity && target.isAlive()
				)) {

			final Vec3 hit =
					candidate.getBoundingBox()
							.inflate(candidate.getPickRadius())
							.clip(origin, end)
							.orElse(null);

			if (hit == null)
				continue;

			final double distance =
					origin.distanceToSqr(hit);

			if (distance < selectedDistance) {
				selected = candidate;
				selectedDistance = distance;
			}
		}

		return selected == null
				? null
				: new NeoLivingEntity(selected);
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

			if (clipStart.distanceToSqr(rayOrigin) >=
					rayEnd.distanceToSqr(rayOrigin)) {

				hit = BlockHitResult.miss(
						rayEnd,
						Direction.getNearest(
								direction.x,
								direction.y,
								direction.z
						),
						BlockPos.containing(rayEnd)
				);

				break;
			}
		}

		/*
		 * Sky / no block:
		 * use the farthest collision-free destination.
		 */
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

		final Direction beforeDirection =
				hit.getDirection();

		final BlockPos before =
				target.relative(beforeDirection);

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

	@Override
	public MyVec3 safeHorizontalDestination(
			final MyLivingEntity abstractEntity,
			final double range) {

		final LivingEntity entity = nativeEntity(abstractEntity);

		if (!(entity.level() instanceof ServerLevel level))
			return null;

		final Vec3 look = entity.getLookAngle();
		final Vec3 horizontal =
				new Vec3(look.x, 0.0, look.z);

		if (horizontal.lengthSqr() <= 1.0e-8)
			return null;

		final Vec3 direction = horizontal.normalize();
		final Vec3 origin = entity.position();

		final Vec3 rayOrigin = entity.getEyePosition();
		final Vec3 rayEnd =
				rayOrigin.add(direction.scale(range));

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

			final Vec3 candidate =
					origin.add(direction.scale(distance));

			final AABB targetBox =
					entity.getBoundingBox()
							.move(candidate.subtract(origin));

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

		final LivingEntity entity =
				nativeEntity(abstractEntity);

		if (!(entity.level() instanceof ServerLevel level))
			return List.of();

		final Vec3 origin =
				new Vec3(from.x(), from.y(), from.z());

		final Vec3 destination =
				new Vec3(to.x(), to.y(), to.z());

		final AABB sweptArea =
				entity.getBoundingBox()
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
				.map(NeoLivingEntity::new)
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

		final var component = stack.is(Items.ENCHANTED_BOOK)
				? DataComponents.STORED_ENCHANTMENTS
				: DataComponents.ENCHANTMENTS;

		final ItemEnchantments enchantments =
				stack.getOrDefault(component, ItemEnchantments.EMPTY);

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

		final ItemEnchantments.Mutable mutable =
				new ItemEnchantments.Mutable(enchantments);

		boolean upgraded = false;
		int upgradedLevel = 0;

		for (final var enchantment : enchantments.keySet()) {
			final int level = enchantments.getLevel(enchantment);
			final int maximum = enchantment.value().getMaxLevel();

			if (level >= maximum)
				continue;

			upgradedLevel = level + 1;
			mutable.set(enchantment, upgradedLevel);
			upgraded = true;
			break;
		}

		if (!upgraded) {
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

		stack.set(component, mutable.toImmutable());

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

		final var component = stack.is(Items.ENCHANTED_BOOK)
				? DataComponents.STORED_ENCHANTMENTS
				: DataComponents.ENCHANTMENTS;

		final ItemEnchantments enchantments =
				stack.getOrDefault(component, ItemEnchantments.EMPTY);

		final var curse = enchantments.keySet().stream()
				.filter(enchantment -> enchantment.is(EnchantmentTags.CURSE))
				.findFirst()
				.orElse(null);

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

		final ItemEnchantments.Mutable mutable =
				new ItemEnchantments.Mutable(enchantments);

		mutable.set(curse, 0);
		stack.set(component, mutable.toImmutable());

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

		final int repairCost =
				stack.getOrDefault(DataComponents.REPAIR_COST, 0);

		if (repairCost <= 0) {
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

		stack.remove(DataComponents.REPAIR_COST);

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

		PacketDistributor.sendToPlayer(
				player,
				new NeoNetwork.SelectedSpellPayload(spellId, cooldownTicks)
		);
	}

	@Override
	public void clearSpellHud(final MyLivingEntity caster) {
		final ServerPlayer player = player(caster);

		if (player == null)
			return;

		PacketDistributor.sendToPlayer(
				player,
				new NeoNetwork.SelectedSpellPayload("", 0)
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
		final MinecraftServer server =
				ServerLifecycleHooks.getCurrentServer();

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

		final var component = stack.is(Items.ENCHANTED_BOOK)
				? DataComponents.STORED_ENCHANTMENTS
				: DataComponents.ENCHANTMENTS;

		final ItemEnchantments enchantments =
				stack.getOrDefault(component, ItemEnchantments.EMPTY);

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

		Holder<Enchantment> selected = null;
		int selectedLevel = 0;

		for (final Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(EnchantmentTags.CURSE))
				continue;

			selected = enchantment;
			selectedLevel = enchantments.getLevel(enchantment);
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

		final ItemEnchantments.Mutable remaining =
				new ItemEnchantments.Mutable(enchantments);

		remaining.set(selected, 0);
		stack.set(component, remaining.toImmutable());

		final ItemEnchantments.Mutable extracted =
				new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

		extracted.set(selected, selectedLevel);

		final ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		book.set(
				DataComponents.STORED_ENCHANTMENTS,
				extracted.toImmutable()
		);

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
