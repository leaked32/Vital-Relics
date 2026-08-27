package com.example.vitalrelics.network;

import com.example.vitalrelics.common.MySpellSystem;
import com.example.vitalrelics.platform.NeoLivingEntity;
import com.example.vitalrelics.platform.NeoSpellRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class SpellSystem {
	private SpellSystem() {}

	public static final String CAST_SPELL =
			MySpellSystem.CAST_SPELL;

	public static final String SWITCH_SPELL_NEXT =
			MySpellSystem.SWITCH_SPELL_NEXT;

	public static final String SWITCH_SPELL_PREVIOUS =
			MySpellSystem.SWITCH_SPELL_PREVIOUS;

	public static void activate(
			final LivingEntity caster,
			final String abilityId) {

		MySpellSystem.INSTANCE.activate(
				new NeoLivingEntity(caster),
				abilityId,
				NeoSpellRuntime.INSTANCE
		);
	}

	public static void syncSpellHud(
			final ServerPlayer player) {

		MySpellSystem.INSTANCE.syncSpellHud(
				new NeoLivingEntity(player),
				NeoSpellRuntime.INSTANCE
		);
	}
}