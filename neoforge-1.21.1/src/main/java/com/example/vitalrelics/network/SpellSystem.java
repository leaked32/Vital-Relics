package com.example.vitalrelics.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public final class SpellSystem {
	@FunctionalInterface
	public interface Handler {
		void activate(ServerPlayer player, int level);
	}

	private static final Map<String, Handler> HANDLERS = new HashMap<>();

	private SpellSystem() {}

	public static void register(final String abilityId, final Handler handler) {
		if (HANDLERS.putIfAbsent(abilityId, handler) != null) {
			throw new IllegalStateException(
					"Spell already registered: " + abilityId
			);
		}
	}



	public static final String CAST_SPELL = "cast_spell";
	public static final String SWITCH_SPELL = "switch_spell";

	public static void activate(final ServerPlayer player, final String abilityId) {


	}
}