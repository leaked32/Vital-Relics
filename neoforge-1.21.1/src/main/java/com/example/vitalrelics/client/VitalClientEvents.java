package com.example.vitalrelics.client;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicSpells;
import com.example.vitalrelics.network.NetworkPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static com.example.vitalrelics.Utils.gatherRelics;

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

	private static String selectedSpellId;

	@SubscribeEvent
	public static void clientTick(
			final ClientTickEvent.Post event) {

		while (ACTIVE_SKILL_KEY.consumeClick()) {
			final Minecraft minecraft = Minecraft.getInstance();
			final String spellId = selectedSpell(minecraft);

			if (spellId == null)
				continue;

			minecraft.options.keyDrop.consumeClick();
			PacketDistributor.sendToServer(new NetworkPayload(spellId));
		}
	}

	@SubscribeEvent
	public static void register(final RegisterKeyMappingsEvent event) {
		event.register(ACTIVE_SKILL_KEY);
		event.register(SWITCH_SKILL_KEY);
	}

	@SubscribeEvent
	public static void mouseScroll(final InputEvent.MouseScrollingEvent event) {
		if (!SWITCH_SKILL_KEY.isDown())
			return;

		final double delta = event.getScrollDeltaY();

		if (delta == 0.0 || selectNext(delta > 0.0 ? -1 : 1) == null)
			return;

		event.setCanceled(true);
	}

	private static String selectNext(final int direction) {
		final Minecraft minecraft = Minecraft.getInstance();
		final List<String> spellIds = spellIds(minecraft);

		if (spellIds.isEmpty()) {
			selectedSpellId = null;
			return null;
		}

		final int current = Math.max(0, spellIds.indexOf(selectedSpellId));
		selectedSpellId = spellIds.get(Math.floorMod(
				current + direction, spellIds.size()
		));

		minecraft.player.displayClientMessage(
				Component.literal("Selected spell: " +
						Relic.itemDisplayName(selectedSpellId)),
				true
		);
		return selectedSpellId;
	}

	private static String selectedSpell(final Minecraft minecraft) {
		final List<String> spellIds = spellIds(minecraft);

		if (spellIds.isEmpty()) {
			selectedSpellId = null;
			return null;
		}

		if (!spellIds.contains(selectedSpellId))
			selectedSpellId = spellIds.get(0);

		return selectedSpellId;
	}

	private static List<String> spellIds(final Minecraft minecraft) {
		if (minecraft.player == null || minecraft.screen != null)
			return List.of();

		return new ArrayList<>(
				RelicSpells.gatherSpells(
						gatherRelics(minecraft.player)
				).keySet()
		);
	}

	/*
	Client Rendering
	 */

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
