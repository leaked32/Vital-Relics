package com.example.vitalrelics.client;

import com.example.vitalrelics.SpellNetwork;
import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

import static com.example.vitalrelics.Utils.gatherRelics;

@EventBusSubscriber(modid = VitalRelics.MODID, value = Dist.CLIENT)
public final class SpellClientInput {
	private static String selectedSpellId = null;

	private SpellClientInput() {}

	@SubscribeEvent
	public static void onMouseScroll(final InputEvent.MouseScrollingEvent event) {
		if (!Screen.hasShiftDown())
			return;

		final double delta = event.getScrollDelta();

		if (delta == 0.0 || selectNext(delta > 0.0 ? -1 : 1) == null)
			return;

		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onClientTick(final ClientTickEvent.Post event) {
		final Minecraft minecraft = Minecraft.getInstance();

		while (SpellKeyMappings.CAST_SPELL.consumeClick()) {
			final String spellId = selectedSpell(minecraft);

			if (spellId == null)
				return;

			/*
			 * Q is also vanilla's drop key. Both mappings receive its click,
			 * so consume the matching vanilla click only when a spell is cast.
			 */
			minecraft.options.keyDrop.consumeClick();
			ClientPacketDistributor.sendToServer(
					new SpellNetwork.CastSpellPayload(spellId)
			);
		}
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
				current + direction,
				spellIds.size()
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
			selectedSpellId = spellIds.getFirst();

		return selectedSpellId;
	}

	private static List<String> spellIds(final Minecraft minecraft) {
		if (minecraft.player == null || minecraft.screen != null)
			return List.of();

		return new ArrayList<>(
				RelicSpells.gatherSpells(gatherRelics(minecraft.player)).keySet()
		);
	}
}
