package com.example.vitalrelics.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
				Network.syncSpellHud(player);
			}
		});

		context.setPacketHandled(true);
	}
}
