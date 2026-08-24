package com.example.vitalrelics.network;

import com.example.vitalrelics.VitalRelics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NetworkPayload(String abilityId)
		implements CustomPacketPayload {

	public static final Type<NetworkPayload> TYPE =
			new Type<>(
					ResourceLocation.fromNamespaceAndPath(
							VitalRelics.MODID,
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