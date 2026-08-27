package com.example.vitalrelics.network;

import com.example.vitalrelics.client.ClientSpellState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
