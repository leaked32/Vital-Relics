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
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;


public final class VitalClientEvents {
	private static final ResourceLocation FLAT_MODEL =
			new ResourceLocation(VitalRelics.MODID, "item/flat");

	private VitalClientEvents() {}

	public static void register(final IEventBus modEventBus) {
		modEventBus.addListener(VitalClientEvents::registerKeys);
		modEventBus.addListener(VitalClientEvents::registerAdditionalModels);
		modEventBus.addListener(VitalClientEvents::modifyBakingResult);

		MinecraftForge.EVENT_BUS.addListener(VitalClientEvents::clientTick);
		MinecraftForge.EVENT_BUS.addListener(VitalClientEvents::mouseScroll);
	}

	/*
	KEY REGISTRATION
	 */

	public static final KeyMapping ACTIVE_SKILL_KEY =
			new KeyMapping(
					"key.vitalrelics.cast_spell",
					KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_Q, "key.categories.vitalrelics"
			);

	public static final KeyMapping SWITCH_SKILL_KEY =
			new KeyMapping(
					"key.vitalrelics.switch_spell",
					KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_LEFT_SHIFT, "key.categories.vitalrelics"
			);

	public static void registerKeys(final RegisterKeyMappingsEvent event) {
		event.register(ACTIVE_SKILL_KEY);
		event.register(SWITCH_SKILL_KEY);
	}

	public static void clientTick(final TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;

		while (ACTIVE_SKILL_KEY.consumeClick()) {
			Minecraft.getInstance().options.keyDrop.consumeClick();
			Network.sendToServer(SpellSystem.CAST_SPELL);
		}
	}

	public static void mouseScroll(final InputEvent.MouseScrollingEvent event) {
		if (!SWITCH_SKILL_KEY.isDown())
			return;

		final double delta = event.getScrollDelta();

		if (delta == 0.0)
			return;

		Network.sendToServer(
				delta > 0.0
						? SpellSystem.SWITCH_SPELL_PREVIOUS
						: SpellSystem.SWITCH_SPELL_NEXT
		);

		event.setCanceled(true);
	}

	/*
	Client Rendering
	 */

	public static void registerAdditionalModels(
			final ModelEvent.RegisterAdditional event) {

		event.register(FLAT_MODEL);
	}

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