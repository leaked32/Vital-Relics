package com.example.vitalrelics.acquisition;

import com.example.vitalrelics.common.relics.Acquisition;
import com.example.vitalrelics.common.Manifest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;

public final class AcquisitionEvents {
	private AcquisitionEvents() {}

	@SubscribeEvent
	public static void onLootTableLoad(final LootTableLoadEvent event) {
		final String table = event.getName().toString();

		for (final var entry : Acquisition.get().data.loot.entrySet()) {
			final Identifier id =
					Identifier.fromNamespaceAndPath(Manifest.MODID, entry.getKey());

			if (!BuiltInRegistries.ITEM.containsKey(id))
				continue;

			final var item = BuiltInRegistries.ITEM.get(id);

			if (item.isEmpty())
				continue;

			final Item nativeItem = item.get().value();

			for (final Acquisition.Data.Loot rule : entry.getValue()) {
				if (!rule.table.equals(table))
					continue;

				final LootPool pool = LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1))
						.add(LootItem.lootTableItem(nativeItem)
								.when(LootItemRandomChanceCondition.randomChance(
										(float)rule.chance)))
						.build();

				event.getTable().addPool(pool);
			}
		}
	}

//	@SubscribeEvent
//	public static void onRecipesUpdated(final RecipesUpdatedEvent event) {
//		System.out.println("Recipes updated");
//
//		for (final var recipe : event.getRecipeManager().getRecipes()) {
//			if (recipe.id().getNamespace().equals(VitalRelics.MODID)) {
//				System.out.println(
//						recipe.id() + " : " +
//								recipe.value().getClass().getName()
//				);
//			}
//		}
//	}
}