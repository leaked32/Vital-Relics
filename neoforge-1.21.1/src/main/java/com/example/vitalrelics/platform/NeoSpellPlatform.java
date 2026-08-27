package com.example.vitalrelics.platform;

import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicTranslations;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MySpellPlatform;
import com.example.vitalrelics.network.SelectedSpellPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

import static com.example.vitalrelics.Utils.gatherRelics;
import static com.example.vitalrelics.Utils.message;

public final class NeoSpellPlatform implements MySpellPlatform {
	public static final NeoSpellPlatform INSTANCE = new NeoSpellPlatform();

	private NeoSpellPlatform() {}

	private static ServerPlayer player(final MyLivingEntity entity) {
		if (!(entity instanceof NeoLivingEntity neo) ||
				!(neo.nativeEntity() instanceof ServerPlayer player)) {

			return null;
		}

		return player;
	}

	private static String spellName(final String id) {
		return RelicTranslations.INSTANCE.translate(
				"relic.vitalrelics.spell." + id,
				Relic.itemDisplayName(id)
		);
	}

	@Override
	public List<Relic> gatherRelics(final MyLivingEntity caster) {
		if (!(caster instanceof NeoLivingEntity neo))
			return List.of();

		return com.example.vitalrelics.Utils.gatherRelics(
				neo.nativeEntity()
		);
	}
	@Override
	public void syncSpellHud(
			final MyLivingEntity caster,
			final String spellId,
			final int cooldownTicks) {

		final ServerPlayer player = player(caster);
		if (player == null)
			return;

		PacketDistributor.sendToPlayer(
				player,
				new SelectedSpellPayload(spellId, cooldownTicks)
		);
	}

	@Override
	public void clearSpellHud(final MyLivingEntity caster) {
		final ServerPlayer player = player(caster);
		if (player == null)
			return;

		PacketDistributor.sendToPlayer(
				player,
				new SelectedSpellPayload("", 0)
		);
	}

	@Override
	public void showSelectedSpell(
			final MyLivingEntity caster,
			final String spellId) {

		final ServerPlayer player = player(caster);
		if (player == null)
			return;

		player.displayClientMessage(
				message(
						"message.vitalrelics.selected_spell",
						"Selected spell: %s",
						spellName(spellId)
				),
				true
		);
	}

	@Override
	public void showSpellCooldown(
			final MyLivingEntity caster,
			final String spellId,
			final int remainingTicks) {

		final ServerPlayer player = player(caster);
		if (player == null)
			return;

		player.displayClientMessage(
				message(
						"message.vitalrelics.spell_cooldown",
						"%s cooldown: %s",
						spellName(spellId),
						String.format(
								Locale.ROOT,
								"%.1fs",
								remainingTicks / 20.0
						)
				),
				true
		);
	}
}
