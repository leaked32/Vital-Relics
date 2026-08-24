package com.example.vitalrelics;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SpellNetwork {
	private static final String NETWORK_VERSION = "1";

	private SpellNetwork() {}

	public record CastSpellPayload(String spellId) implements CustomPacketPayload {
		public static final Type<CastSpellPayload> TYPE = new Type<>(
				ResourceLocation.fromNamespaceAndPath(VitalRelics.MODID, "cast_spell")
		);

		public static final StreamCodec<ByteBuf, CastSpellPayload> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.STRING_UTF8,
						CastSpellPayload::spellId,
						CastSpellPayload::new
				);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);

		registrar.playToServer(
				CastSpellPayload.TYPE,
				CastSpellPayload.STREAM_CODEC,
				SpellNetwork::handleCastSpell
		);
	}

	private static void handleCastSpell(
			final CastSpellPayload payload,
			final IPayloadContext context) {

		if (context.player() instanceof ServerPlayer player)
			SpellService.cast(player, payload.spellId());
	}
}
