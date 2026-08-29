package com.example.vitalrelics.client;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.MySpellSystem;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Translations;
import com.example.vitalrelics.network.ForgeNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Locale;


public final class VitalClientEvents {
	private static final ResourceLocation FLAT_MODEL =
			new ResourceLocation(Manifest.MODID, "item/flat");

	private VitalClientEvents() {}

	public static void register(final IEventBus modEventBus) {
		modEventBus.addListener(VitalClientEvents::registerKeys);
		modEventBus.addListener(VitalClientEvents::registerAdditionalModels);
		modEventBus.addListener(VitalClientEvents::modifyBakingResult);
		modEventBus.addListener(VitalClientEvents::registerGuiOverlays);

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
					GLFW.GLFW_KEY_R, "key.categories.vitalrelics"
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

		ClientSpellState.clientTick();

		while (ACTIVE_SKILL_KEY.consumeClick()) {
			Minecraft.getInstance().options.keyDrop.consumeClick();
			ForgeNetwork.sendToServer(MySpellSystem.CAST_SPELL);
		}

		Translations.get().setSelectedLocale(
				Minecraft.getInstance().getLanguageManager().getSelected()
		);
	}

	public static void mouseScroll(final InputEvent.MouseScrollingEvent event) {
		if (!SWITCH_SKILL_KEY.isDown())
			return;

		final double delta = event.getScrollDelta();

		if (delta == 0.0)
			return;

		ForgeNetwork.sendToServer(
				delta > 0.0
						? MySpellSystem.SWITCH_SPELL_PREVIOUS
						: MySpellSystem.SWITCH_SPELL_NEXT
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

		for (final Relic relic : Loader.get().relics_) {
			final ModelResourceLocation itemModel =
					new ModelResourceLocation(
							Manifest.MODID,
							relic.id,
							"inventory"
					);

			event.getModels().put(itemModel, flat);
		}
	}

	/*
	HUD
	 */

	public static void registerGuiOverlays(
			final RegisterGuiOverlaysEvent event) {

		event.registerAboveAll(
				"spell_hud",
				(gui, graphics, partialTick, screenWidth, screenHeight) ->
						renderSpellHud(
								graphics,
								screenWidth,
								screenHeight
						)
		);
	}

	private static void renderSpellHud(
			final GuiGraphics graphics,
			final int guiWidth,
			final int guiHeight) {

		final Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.options.hideGui)
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
								Locale.ROOT,
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

		// Anchor the spell HUD to the bottom-left of the screen.
		// Its dynamic width therefore grows toward the right.
		final int x = 6;
		final int y = guiHeight - 22;

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
				cooldownTicks > 0
						? 0xFF9E434C
						: 0xFF6FA57A
		);

		graphics.drawString(
				font,
				spellName,
				x + padding,
				y + 5,
				0xFFE8D8DC,
				true
		);

		final int statusX =
				x + width -
						padding -
						font.width(status);

		graphics.drawString(
				font,
				status,
				statusX,
				y + 5,
				cooldownTicks > 0
						? 0xFFE38A91
						: 0xFFA8D6AF,
				true
		);
	}
}
