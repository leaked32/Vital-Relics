package com.example.vitalrelics.client;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicTranslations;
import com.example.vitalrelics.network.NetworkPayload;
import com.example.vitalrelics.network.SpellSystem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class VitalClientEvents {
	private static final ResourceLocation FLAT_ID =
			ResourceLocation.fromNamespaceAndPath(VitalRelics.MODID, "item/flat");

	private static final ModelResourceLocation FLAT_MODEL =
			ModelResourceLocation.standalone(FLAT_ID);

	private VitalClientEvents() {}

	public static void registerListeners(final IEventBus modEventBus) {
		modEventBus.addListener(VitalClientEvents::registerKeyMappings);
		modEventBus.addListener(VitalClientEvents::registerClientExtensions);
		modEventBus.addListener(VitalClientEvents::registerAdditionalModels);
		modEventBus.addListener(VitalClientEvents::modifyBakingResult);

		NeoForge.EVENT_BUS.addListener(VitalClientEvents::clientTick);
		NeoForge.EVENT_BUS.addListener(VitalClientEvents::mouseScroll);
	}

	/*
	KEY REGISTRATION
	 */

	public static final KeyMapping ACTIVE_SKILL_KEY =
			new KeyMapping(
					"key.vitalrelics.cast_spell", KeyConflictContext.IN_GAME,
					InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, "key.categories.vitalrelics"
			);

	public static final KeyMapping SWITCH_SKILL_KEY =
			new KeyMapping(
					"key.vitalrelics.switch_spell",
					KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_LEFT_SHIFT,"key.categories.vitalrelics"
			);

	public static void clientTick(
			final ClientTickEvent.Post event) {

		while (ACTIVE_SKILL_KEY.consumeClick()) {
			final Minecraft minecraft = Minecraft.getInstance();
			minecraft.options.keyDrop.consumeClick();

			PacketDistributor.sendToServer(
					new NetworkPayload(SpellSystem.CAST_SPELL)
			);
		}

		RelicTranslations.INSTANCE.setSelectedLocale(
				Minecraft.getInstance().getLanguageManager().getSelected()
		);
	}


	public static void registerKeyMappings(
			final RegisterKeyMappingsEvent event) {

		event.register(ACTIVE_SKILL_KEY);
		event.register(SWITCH_SKILL_KEY);
	}

	public static void mouseScroll(final InputEvent.MouseScrollingEvent event) {
		if (!SWITCH_SKILL_KEY.isDown())
			return;

		final double delta = event.getScrollDeltaY();

		if (delta == 0.0)
			return;

		PacketDistributor.sendToServer(new NetworkPayload(
				delta > 0.0
						? SpellSystem.SWITCH_SPELL_PREVIOUS
						: SpellSystem.SWITCH_SPELL_NEXT
		));

		event.setCanceled(true);
	}

	/*
	Client Rendering
	 */

	public static void registerClientExtensions(
			final RegisterClientExtensionsEvent event) {

		final Item[] items = VitalRelics.RELIC_ITEMS.stream()
				.map(holder -> holder.get())
				.toArray(Item[]::new);

		event.registerItem(new RelicClientExtensions(), items);
	}

	public static void registerAdditionalModels(
			final ModelEvent.RegisterAdditional event) {

		// Explicitly load assets/vitalrelics/models/item/flat.json.
		event.register(FLAT_MODEL);
	}

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
