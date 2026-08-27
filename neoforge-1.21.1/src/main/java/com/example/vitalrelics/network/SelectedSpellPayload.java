package com.example.vitalrelics.network;

import com.example.vitalrelics.common.Manifest;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectedSpellPayload(
		String spellId,
		int cooldownTicks)
		implements CustomPacketPayload {

	public static final Type<SelectedSpellPayload> TYPE =
			new Type<>(
					ResourceLocation.fromNamespaceAndPath(
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