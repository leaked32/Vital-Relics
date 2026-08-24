package com.example.vitalrelics.client;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.network.SpellSystem;
import com.example.vitalrelics.network.Network;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
		modid = VitalRelics.MODID,
		bus = Mod.EventBusSubscriber.Bus.MOD,
		value = Dist.CLIENT
)
public final class VitalClientEvents {
	private static final ResourceLocation FLAT_MODEL =
			new ResourceLocation(VitalRelics.MODID, "item/flat");

	private VitalClientEvents() {}

	/*
	KEY REGISTRATION
	 */

	public static final KeyMapping ACTIVE_SKILL_KEY =
			new KeyMapping(
					"key.vitalrelics.cast_spell", KeyConflictContext.IN_GAME,
					InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.vitalrelics"
			);

	public static final KeyMapping SWITCH_SKILL_KEY =
			new KeyMapping(
					"key.vitalrelics.switch_spell", KeyConflictContext.IN_GAME,
					InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.vitalrelics"
			);

	@SubscribeEvent
	public static void registerKeys(
			final RegisterKeyMappingsEvent event) {

		event.register(ACTIVE_SKILL_KEY);
		event.register(SWITCH_SKILL_KEY);
	}

	@SubscribeEvent
	public static void clientTick(
			final TickEvent.ClientTickEvent event) {

		if (event.phase != TickEvent.Phase.END)
			return;

		while (ACTIVE_SKILL_KEY.consumeClick()) {
			Network.sendToServer(SpellSystem.CAST_SPELL);
		}

		while (SWITCH_SKILL_KEY.consumeClick()) {
			Network.sendToServer(SpellSystem.SWITCH_SPELL);
		}
	}

	/*
	Client Rendering
	 */

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