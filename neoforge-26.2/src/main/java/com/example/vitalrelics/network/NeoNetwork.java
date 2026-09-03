package com.example.vitalrelics.network;

import com.example.vitalrelics.common.MySpellSystem;
import com.example.vitalrelics.platform.NeoLivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;


import com.example.vitalrelics.common.Manifest;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class NeoNetwork {
	private static final String PROTOCOL_VERSION = "0.6.7";

	private NeoNetwork() {}

	public static void registerPayloadHandlers(
			final RegisterPayloadHandlersEvent event) {

		final var registrar =
				event.registrar(PROTOCOL_VERSION);

		registrar.playToServer(
				NetworkPayload.TYPE,
				NetworkPayload.STREAM_CODEC,
				(payload, context) -> {
					context.enqueueWork(() -> {
						if (context.player() instanceof ServerPlayer player) {
							MySpellSystem.INSTANCE.activate(
									new NeoLivingEntity(player),
									payload.abilityId());
						}
					});
				}
		);

		registrar.playToClient(
				SelectedSpellPayload.TYPE,
				SelectedSpellPayload.STREAM_CODEC,
				(payload, context) -> {
					context.enqueueWork(() ->
							com.example.vitalrelics.client.ClientSpellState.update(
									payload.spellId(),
									payload.cooldownTicks()
							)
					);
				}
		);
	}

	public record NetworkPayload(String abilityId)
			implements CustomPacketPayload {

		public static final Type<NetworkPayload> TYPE =
				new Type<>(
						Identifier.fromNamespaceAndPath(
								Manifest.MODID,
								"activate_ability"
						)
				);

		public static final StreamCodec<
				ByteBuf,
				NetworkPayload
				> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8,
				NetworkPayload::abilityId,
				NetworkPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}


	public record SelectedSpellPayload(
			String spellId,
			int cooldownTicks)
			implements CustomPacketPayload {

		public static final Type<SelectedSpellPayload> TYPE =
				new Type<>(
						Identifier.fromNamespaceAndPath(
								Manifest.MODID,
								"selected_spell"
						)
				);

		public static final StreamCodec<ByteBuf, SelectedSpellPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.STRING_UTF8,
						SelectedSpellPayload::spellId,
						ByteBufCodecs.VAR_INT,
						SelectedSpellPayload::cooldownTicks,
						SelectedSpellPayload::new
				);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
