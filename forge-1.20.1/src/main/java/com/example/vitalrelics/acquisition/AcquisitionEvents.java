package com.example.vitalrelics.acquisition;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Acquisition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class AcquisitionEvents {
	private AcquisitionEvents() {}

	@SubscribeEvent
	public static void onLootTableLoad(final LootTableLoadEvent event) {
		final String table = event.getName().toString();

		for (final var entry : VitalRelics.acquisition.data.loot.entrySet()) {
			final ResourceLocation id =
					new ResourceLocation(VitalRelics.MODID, entry.getKey());

			if (!BuiltInRegistries.ITEM.containsKey(id))
				continue;

			final Item item = BuiltInRegistries.ITEM.get(id);

			for (final Acquisition.Loot rule : entry.getValue()) {
				if (!rule.table.equals(table))
					continue;

				final LootPool pool = LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1))
						.add(LootItem.lootTableItem(item)
								.when(LootItemRandomChanceCondition.randomChance(
										(float)rule.chance)))
						.build();

				event.getTable().addPool(pool);
			}
		}
	}
}