package com.example.vitalrelics.network;

import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicSpells;
import com.example.vitalrelics.common.Scheduler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.vitalrelics.Utils.gatherRelics;

public final class Network {
	private static final String PROTOCOL_VERSION = "1";

	private static final SimpleChannel CHANNEL =
			NetworkRegistry.newSimpleChannel(
					new ResourceLocation(Manifest.MODID, "main"),
					() -> PROTOCOL_VERSION,
					PROTOCOL_VERSION::equals,
					PROTOCOL_VERSION::equals
			);

	private static int nextPacketId = 0;

	private Network() {}

	public static void register() {
		CHANNEL.messageBuilder(
						NetworkPacket.class,
						nextPacketId++
				)
				.encoder(NetworkPacket::encode)
				.decoder(NetworkPacket::decode)
				.consumerMainThread(NetworkPacket::handle)
				.add();

		CHANNEL.messageBuilder(
						SelectedSpellPacket.class,
						nextPacketId++,
						NetworkDirection.PLAY_TO_CLIENT
				)
				.encoder(SelectedSpellPacket::encode)
				.decoder(SelectedSpellPacket::decode)
				.consumerMainThread(SelectedSpellPacket::handle)
				.add();
	}

	public static void sendToServer(final String abilityId) {
		CHANNEL.sendToServer(new NetworkPacket(abilityId));
	}

	public static void syncSpellHud(final ServerPlayer player) {
		final int tick = player.serverLevel()
				.getServer()
				.getTickCount();

		final Map<String, Relic.Spells.Info> spells =
				RelicSpells.gatherSpells(gatherRelics(player));

		final List<String> spellIds =
				new ArrayList<>(spells.keySet());

		final String selected =
				Scheduler.INSTANCE().selectedSpell(
						player.getUUID(),
						spellIds,
						tick
				);

		if (selected == null) {
			sendToPlayer(
					player,
					new SelectedSpellPacket("", 0)
			);
			return;
		}

		final int cooldownTicks =
				Scheduler.INSTANCE().getSpellCooldownRemaining(
						player.getUUID(),
						selected,
						tick
				);

		sendToPlayer(
				player,
				new SelectedSpellPacket(
						selected,
						cooldownTicks
				)
		);
	}

	private static void sendToPlayer(
			final ServerPlayer player,
			final SelectedSpellPacket packet) {

		CHANNEL.send(
				PacketDistributor.PLAYER.with(() -> player),
				packet
		);
	}
}
