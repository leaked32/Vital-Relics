package com.example.vitalrelics.network;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Manifest;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

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
	}

	public static void sendToServer(final String abilityId) {
		CHANNEL.sendToServer(new NetworkPacket(abilityId));
	}
}