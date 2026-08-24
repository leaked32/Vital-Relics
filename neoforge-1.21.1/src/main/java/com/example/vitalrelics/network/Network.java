package com.example.vitalrelics.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class Network {
	private static final String PROTOCOL_VERSION = "1";

	private Network() {}

	public static void registerPayloadHandlers(
			final RegisterPayloadHandlersEvent event) {

		event.registrar(PROTOCOL_VERSION)
				.playToServer(
						NetworkPayload.TYPE,
						NetworkPayload.STREAM_CODEC,
						(payload, context) -> {
							context.enqueueWork(() -> {
								if (context.player()
										instanceof ServerPlayer player) {

									SpellSystem.activate(
											player,
											payload.abilityId()
									);
								}
							});
						}
				);
	}
}