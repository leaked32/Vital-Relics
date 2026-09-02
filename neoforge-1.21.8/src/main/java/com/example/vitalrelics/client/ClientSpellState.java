package com.example.vitalrelics.client;

public final class ClientSpellState {
	private static String selectedSpellId = "";
	private static int cooldownTicks = 0;

	private ClientSpellState() {}

	public static void update(
			final String spellId,
			final int remainingCooldownTicks) {

		selectedSpellId = spellId;
		cooldownTicks = Math.max(0, remainingCooldownTicks);
	}

	public static void clientTick() {
		if (cooldownTicks > 0)
			--cooldownTicks;
	}

	public static String selectedSpellId() {
		return selectedSpellId;
	}

	public static int cooldownTicks() {
		return cooldownTicks;
	}
}