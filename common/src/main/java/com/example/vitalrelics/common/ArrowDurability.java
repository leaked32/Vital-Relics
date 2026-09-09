package com.example.vitalrelics.common;

/**
 * Shared durability math; hardness is Minecraft's destroy speed, not blast resistance.
 */
public final class ArrowDurability {
	private double remaining;
	private final double lossPerTick;

	public ArrowDurability(double durability, double lossPerTick) {
		if (!Double.isFinite(durability) || durability <= 0 || !Double.isFinite(
				lossPerTick) || lossPerTick < 0)
			throw new IllegalArgumentException("Invalid arrow durability");
		this.remaining = durability;
		this.lossPerTick = lossPerTick;
	}

	public boolean tick() {
		remaining = Math.max(0, remaining - lossPerTick);
		return remaining > 0;
	}

	public boolean canBreak(double hardness) {
		return Double.isFinite(hardness) && hardness >= 0 && remaining > hardness;
	}

	public void spend(double hardness) {
		if (!canBreak(hardness)) throw new IllegalArgumentException("Insufficient durability");
		remaining -= hardness;
	}

	public double remaining() {
		return remaining;
	}
}
