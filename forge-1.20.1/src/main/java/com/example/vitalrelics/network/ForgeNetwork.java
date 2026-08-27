package com.example.vitalrelics.network;

import com.example.vitalrelics.common.Manifest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.example.vitalrelics.client.ClientSpellState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;


import java.util.function.Supplier;

public final class ForgeNetwork {
	private static final String PROTOCOL_VERSION = "1";

	private static final SimpleChannel CHANNEL =
			NetworkRegistry.newSimpleChannel(
					new ResourceLocation(Manifest.MODID, "main"),
					() -> PROTOCOL_VERSION,
					PROTOCOL_VERSION::equals,
					PROTOCOL_VERSION::equals
			);

	private static int nextPacketId = 0;

	private ForgeNetwork() {}

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

	public static void sendSpellHud(
			final ServerPlayer player,
			final String spellId,
			final int cooldownTicks) {

		CHANNEL.send(
				PacketDistributor.PLAYER.with(() -> player),
				new SelectedSpellPacket(spellId, cooldownTicks)
		);
	}


	public record NetworkPacket(String abilityId) {
		public static void encode(
				final NetworkPacket packet,
				final FriendlyByteBuf buffer) {

			buffer.writeUtf(packet.abilityId(), 64);
		}

		public static NetworkPacket decode(
				final FriendlyByteBuf buffer) {

			return new NetworkPacket(buffer.readUtf(64));
		}

		public static void handle(
				final NetworkPacket packet,
				final Supplier<NetworkEvent.Context> contextSupplier) {

			final NetworkEvent.Context context = contextSupplier.get();

			context.enqueueWork(() -> {
				final ServerPlayer player = context.getSender();

				if (player != null) {
					// TODO, implement the handler later.
					SpellSystem.activate(player, packet.abilityId());

					/*
					 * SpellSystem may have changed the selected spell or started
					 * a cooldown, so synchronize the final authoritative state.
					 */
					SpellSystem.syncSpellHud(player);
				}
			});

			context.setPacketHandled(true);
		}
	}

	public record SelectedSpellPacket(
			String spellId,
			int cooldownTicks) {

		public static void encode(
				final SelectedSpellPacket packet,
				final FriendlyByteBuf buffer) {

			buffer.writeUtf(packet.spellId(), 64);
			buffer.writeVarInt(packet.cooldownTicks());
		}

		public static SelectedSpellPacket decode(
				final FriendlyByteBuf buffer) {

			return new SelectedSpellPacket(
					buffer.readUtf(64),
					buffer.readVarInt()
			);
		}

		public static void handle(
				final SelectedSpellPacket packet,
				final Supplier<NetworkEvent.Context> contextSupplier) {

			final NetworkEvent.Context context = contextSupplier.get();

			context.enqueueWork(() ->
					DistExecutor.unsafeRunWhenOn(
							Dist.CLIENT,
							() -> () ->
									ClientSpellState.update(
											packet.spellId(),
											packet.cooldownTicks()
									)
					)
			);

			context.setPacketHandled(true);
		}
	}

}
