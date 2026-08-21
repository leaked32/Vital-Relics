package com.example.vitalrelics.client;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Relic;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
		modid = VitalRelics.MODID,
		bus = Mod.EventBusSubscriber.Bus.MOD,
		value = Dist.CLIENT
)
public final class VitalClientEvents {
	private static final ResourceLocation FLAT_MODEL =
			new ResourceLocation(VitalRelics.MODID, "item/flat");

	private VitalClientEvents() {}

	@SubscribeEvent
	public static void registerAdditionalModels(
			final ModelEvent.RegisterAdditional event) {

		event.register(FLAT_MODEL);
	}

	@SubscribeEvent
	public static void modifyBakingResult(
			final ModelEvent.ModifyBakingResult event) {

		final BakedModel flat = event.getModels().get(FLAT_MODEL);

		if (flat == null) {
			throw new IllegalStateException(
					"Vital Relics shared flat model was not baked"
			);
		}

		for (final Relic relic : VitalRelics.loader.relics_) {
			final ModelResourceLocation itemModel =
					new ModelResourceLocation(
							VitalRelics.MODID,
							relic.id,
							"inventory"
					);

			event.getModels().put(itemModel, flat);
		}
	}
}