package com.example.vitalrelics;

import com.example.vitalrelics.common.*;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Translations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//import static com.example.vitalrelics.compat.TouhouMaidCompat.gatherMaidRelics;

public class Utils {

	/*
	What does it have?
	 */

	public static List<Relic> gatherRelics(final LivingEntity entity) {
		final List<Relic> out = new ArrayList<>();

		// Player inventory / hotbar.
		if (entity instanceof Player player) {
			final var inventory = player.getInventory();

			for (int i = 0; i < inventory.getContainerSize(); ++i) {
				final ItemStack stack = inventory.getItem(i);
				final boolean hotbar = i < 9;

				addRelic(
						out,
						stack,
						hotbar ? "in_hotbar" : "in_inventory",
						"in_inventory"
				);
			}
		}

		// Curios works on LivingEntity, not only Player.
		CuriosApi.getCuriosInventory(entity).ifPresent(inv -> {
			for (final var slot : inv.findCurios(stack -> true))
				addRelic(out, slot.stack(), "in_curios_api_slots");
		});

		// Optional Touhou Little Maid integration.
//		if (ModList.get().isLoaded("touhou_little_maid")) {
//			gatherMaidRelics(entity, out);
//		}

		// Relics assigned directly to spawned enemies.
		gatherEnemyRelics(entity, out);

		return out;
	}

	private static boolean effectiveAt(final Relic relic, final String location) {
		if (relic.effective_slots.isEmpty()) {
			return location.equals("in_curios_api_slots")
					|| location.equals("in_touhou_little_maid_curios_slots");
		}

		return relic.effective_slots.contains(location);
	}

	public static void addRelic(
			final List<Relic> out,
			final ItemStack stack,
			final String... locations) {

		if (stack.isEmpty())
			return;

		final Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!id.getNamespace().equals(Manifest.MODID))
			return;

		final Relic relic = Loader.get().find(id.getPath());
		if (relic == null)
			return;

		for (final String location : locations) {
			if (effectiveAt(relic, location)) {
				out.add(relic);
				return;
			}
		}
	}

	/*
	Special Abilities
	 */

	public static void metalMending(final LivingEntity entity, final int level) {
		if (level <= 0)
			return;

		int remaining = level;

		remaining = repairStack(entity.getMainHandItem(), remaining);

		if (remaining <= 0)
			return;

		remaining = repairStack(entity.getOffhandItem(), remaining);

		if (remaining <= 0)
			return;

		for (final EquipmentSlot slot : new EquipmentSlot[] {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY
		}) {
			remaining = repairStack(entity.getItemBySlot(slot), remaining);

			if (remaining <= 0)
				return;
		}

		if (entity instanceof Player player) {
			final var inventory = player.getInventory();

			for (int i = 0; i < inventory.getContainerSize(); ++i) {
				remaining = repairStack(inventory.getItem(i), remaining);

				if (remaining <= 0)
					return;
			}
		}
	}

	private static int repairStack(final ItemStack stack, final int remaining) {
		if (!stack.isDamageableItem() || !stack.isDamaged())
			return remaining;

		final int repairAmount = Math.min(remaining, stack.getDamageValue());
		stack.setDamageValue(stack.getDamageValue() - repairAmount);
		return remaining - repairAmount;
	}

	/*
	Util Functions
	 */

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
		if (live0 == null || live1 == null || live0 == live1 || live0.getUUID() == live1.getUUID())
			return false;

		// 1. Use the built-in isAlliedTo (covers scoreboard teams + some vanilla behaviors)
		if (live0.isAlliedTo(live1))
			return true;

		// 2. Check if target is a tamed animal owned by base
		if (live1 instanceof TamableAnimal tamable && tamable.isTame()) {
			final LivingEntity owner = tamable.getOwner();

			if (owner != null && owner.is(live0))
				return true;
		}

		// 3. Also check the other way around (if base is the pet and target is the owner)
		if (live0 instanceof TamableAnimal tamableBase && tamableBase.isTame()) {
			final LivingEntity owner = tamableBase.getOwner();

			if (owner != null && owner.is(live1))
				return true;
		}

		return false;
	}

	public static Component message(
			final String key,
			final String fallback,
			final Object... arguments) {

		final String pattern = Translations.get().translate(key, fallback);

		return Component.literal(String.format(Locale.ROOT, pattern, arguments));
	}

	public static String effectName(final String id) {
		final Identifier location = Identifier.parse(id);
		final var effect = BuiltInRegistries.MOB_EFFECT.get(location);

		if (effect.isEmpty())
			return null;

		return Component.translatable(
				effect.get().value().getDescriptionId()
		).getString();
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
		final var optionalList = tag.getList(Manifest.ENEMY_RELICS_TAG);

		if (optionalList.isEmpty())
			return;

		final var list = optionalList.get();

		for (int i = 0; i < list.size(); ++i) {
			final String id = list.getString(i).orElse("");

			final Relic relic = Loader.get().find(id);
			if (relic != null)
				out.add(relic);
		}
	}

	public static boolean enemyRelicsRolled(final LivingEntity entity) {
		return entity.getPersistentData()
				.getBoolean(Manifest.ENEMY_RELICS_ROLLED_TAG)
				.orElse(false);
	}

	public static void markEnemyRelicsRolled(final LivingEntity entity) {
		entity.getPersistentData()
				.putBoolean(Manifest.ENEMY_RELICS_ROLLED_TAG, true);
	}

}
