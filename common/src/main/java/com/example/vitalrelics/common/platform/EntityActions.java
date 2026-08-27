package com.example.vitalrelics.common.platform;

public final class EntityActions {
	private EntityActions() {}

	public static void teleport(
			final MyLivingEntity entity,
			final double x,
			final double y,
			final double z) {

		entity.teleport(x, y, z);
		entity.playSound(MySound.TELEPORT);
	}
}