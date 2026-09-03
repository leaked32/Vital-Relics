package com.example.vitalrelics.client;

import com.example.vitalrelics.RelicItem;
import com.example.vitalrelics.common.*;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Translations;
import com.example.vitalrelics.network.NeoNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class VitalClientEvents {
	private VitalClientEvents() {}
	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(Manifest.MODID, "main")
	);

	public static void registerListeners(final IEventBus modEventBus) {
		modEventBus.addListener(VitalClientEvents::registerKeyMappings);
		modEventBus.addListener(VitalClientEvents::registerGuiLayers);
		modEventBus.addListener(VitalClientEvents::registerSpecialModelRenderers);

		NeoForge.EVENT_BUS.addListener(VitalClientEvents::clientTick);
		NeoForge.EVENT_BUS.addListener(VitalClientEvents::mouseScroll);
		NeoForge.EVENT_BUS.addListener(VitalClientEvents::onItemTooltip);
	}

	public static void registerSpecialModelRenderers(
			final RegisterSpecialModelRendererEvent event) {

		event.register(
				Identifier.fromNamespaceAndPath(Manifest.MODID, "relic"),
				RelicRenderer.Unbaked.MAP_CODEC
		);
	}

	/*
	KEY REGISTRATION
	 */

	public static final KeyMapping ACTIVE_SKILL_KEY =
			new KeyMapping(
					"key.vitalrelics.cast_spell",
					KeyConflictContext.IN_GAME,
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_R,
					KEY_CATEGORY
			);

	public static final KeyMapping SWITCH_SKILL_KEY =
			new KeyMapping(
					"key.vitalrelics.switch_spell",
					KeyConflictContext.IN_GAME,
					InputConstants.Type.KEYSYM,
					GLFW.GLFW_KEY_LEFT_SHIFT,
					KEY_CATEGORY
			);

	public static void clientTick(final ClientTickEvent.Post event) {
		ClientSpellState.clientTick();

		while (ACTIVE_SKILL_KEY.consumeClick()) {
			final Minecraft minecraft = Minecraft.getInstance();
			minecraft.options.keyDrop.consumeClick();

			ClientPacketDistributor.sendToServer(
					new NeoNetwork.NetworkPayload(MySpellSystem.CAST_SPELL)
			);
		}

		Translations.get().setSelectedLocale(
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

		ClientPacketDistributor.sendToServer(
				new NeoNetwork.NetworkPayload(
						delta > 0.0
								? MySpellSystem.SWITCH_SPELL_PREVIOUS
								: MySpellSystem.SWITCH_SPELL_NEXT
				)
		);

		event.setCanceled(true);
	}

	/*
	HUD
	 */

	private static final Identifier SPELL_HUD =
			Identifier.fromNamespaceAndPath(
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
			final net.minecraft.client.gui.GuiGraphicsExtractor graphics,
			final net.minecraft.client.DeltaTracker deltaTracker) {

		final Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.gui.hud.isHidden())
			return;

		final String spellId = ClientSpellState.selectedSpellId();

		if (spellId == null || spellId.isBlank())
			return;

		final String spellName =
				Translations.get().translate(
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
				font.width(spellName)
						+ gap
						+ font.width(status)
						+ padding * 2;

		final int height = 18;

		// Anchor the spell HUD to the bottom-left of the screen.
		// Its dynamic width therefore grows toward the right.
		final int x = 6;
		final int y = graphics.guiHeight() - 22;

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
				x,
				y + 1,
				x + 2,
				y + height - 1,
				cooldownTicks > 0 ? 0xFF9E434C : 0xFF6FA57A
		);

		graphics.text(
				font,
				spellName,
				x + padding,
				y + 5,
				0xFFE8D8DC,
				true
		);

		final int statusX =
				x + width - padding - font.width(status);

		graphics.text(
				font,
				status,
				statusX,
				y + 5,
				cooldownTicks > 0 ? 0xFFE38A91 : 0xFFA8D6AF,
				true
		);
	}


	public static void onItemTooltip(final ItemTooltipEvent event) {
		if (!(event.getItemStack().getItem() instanceof RelicItem item))
			return;

		for (final RelicText.Text line : RelicText.tooltipLines(item.relic()))
			event.getToolTip().add(RelicItem.component(line));
	}
}
