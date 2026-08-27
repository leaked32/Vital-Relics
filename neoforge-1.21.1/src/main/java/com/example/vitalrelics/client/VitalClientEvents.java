package com.example.vitalrelics.client;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Manifest;
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
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public final class VitalClientEvents {
	private static final ResourceLocation FLAT_ID =
			ResourceLocation.fromNamespaceAndPath(Manifest.MODID, "item/flat");

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

		modEventBus.addListener(VitalClientEvents::registerGuiLayers);
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

		ClientSpellState.clientTick();

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
							Manifest.MODID,
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

	/*
	HUD
	 */

	private static final ResourceLocation SPELL_HUD =
			ResourceLocation.fromNamespaceAndPath(
					Manifest.MODID,
					"spell_hud"
			);

	public static void registerGuiLayers(
			final RegisterGuiLayersEvent event) {

		event.registerAboveAll(
				SPELL_HUD,
				VitalClientEvents::renderSpellHud
		);
	}

	private static void renderSpellHud(
			final net.minecraft.client.gui.GuiGraphics graphics,
			final net.minecraft.client.DeltaTracker deltaTracker) {

		final Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.options.hideGui)
			return;

		final String spellId = ClientSpellState.selectedSpellId();

		if (spellId == null || spellId.isBlank())
			return;

		final String spellName =
				RelicTranslations.INSTANCE.translate(
						"relic.vitalrelics.spell." + spellId,
						Relic.itemDisplayName(spellId)
				);

		final int cooldownTicks =
				ClientSpellState.cooldownTicks();

		final String status =
				cooldownTicks > 0
						? String.format(
						java.util.Locale.ROOT,
						"%.1fs",
						cooldownTicks / 20.0
				)
						: "Ready";

		final var font = minecraft.font;

		final int padding = 6;
		final int gap = 12;

		final int width =
				font.width(spellName) +
						gap +
						font.width(status) +
						padding * 2;

		final int height = 18;

		final int hotbarLeft =
				graphics.guiWidth() / 2 - 91;

		final int x =
				hotbarLeft - width - 6;

		final int y =
				graphics.guiHeight() - 22;

		// Main background.
		graphics.fill(
				x,
				y,
				x + width,
				y + height,
				0xB0101014
		);

		// Top highlight.
		graphics.fill(
				x,
				y,
				x + width,
				y + 1,
				0xFF8C4650
		);

		// Bottom shadow.
		graphics.fill(
				x,
				y + height - 1,
				x + width,
				y + height,
				0xFF35191E
		);

		// Cooldown / ready accent.
		graphics.fill(
				x, y + 1, x + 2, y + height - 1,
				cooldownTicks > 0 ? 0xFF9E434C : 0xFF6FA57A
		);

		graphics.drawString(
				font, spellName, x + padding, y + 5,
				0xFFE8D8DC, true
		);

		final int statusX = x + width - padding - font.width(status);

		graphics.drawString(
				font, status, statusX, y + 5,
				cooldownTicks > 0 ? 0xFFE38A91 : 0xFFA8D6AF, true
		);
	}


}
