package com.example.vitalrelics.client;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Relic;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(
		modid = VitalRelics.MODID,
		value = Dist.CLIENT
)
public final class VitalClientEvents {
	private static final ResourceLocation FLAT_ID =
			ResourceLocation.fromNamespaceAndPath(VitalRelics.MODID, "item/flat");

	private static final ModelResourceLocation FLAT_MODEL =
			ModelResourceLocation.standalone(FLAT_ID);

	private VitalClientEvents() {}

	@SubscribeEvent
	public static void registerClientExtensions(
			final RegisterClientExtensionsEvent event) {

		final Item[] items = VitalRelics.RELIC_ITEMS.stream()
				.map(holder -> holder.get())
				.toArray(Item[]::new);

		event.registerItem(new RelicClientExtensions(), items);
	}

	@SubscribeEvent
	public static void registerAdditionalModels(
			final ModelEvent.RegisterAdditional event) {

		// Explicitly load assets/vitalrelics/models/item/flat.json.
		event.register(FLAT_MODEL);
	}

	@SubscribeEvent
	public static void modifyBakingResult(
			final ModelEvent.ModifyBakingResult event) {

		final BakedModel flat = event.getModels().get(FLAT_MODEL);

		if (flat == null)
			throw new IllegalStateException(
					"Vital Relics shared flat model was not baked"
			);

		for (final Relic relic : VitalRelics.loader.relics_) {
			final ResourceLocation id =
					ResourceLocation.fromNamespaceAndPath(
							VitalRelics.MODID,
							relic.id
					);

			final ModelResourceLocation itemModel =
					ModelResourceLocation.inventory(id);

			/*
			 * All relic IDs now resolve to exactly the same baked model.
			 * The renderer distinguishes them using the ItemStack ID.
			 */
			event.getModels().put(itemModel, flat);
		}
	}
}