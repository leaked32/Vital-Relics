package com.example.vitalrelics.platform;

import com.example.vitalrelics.common.platform.MyEntity;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class NeoForgeEntity implements MyEntity {
	protected final Entity entity;

	public NeoForgeEntity(final Entity entity) {
		this.entity = entity;
	}

	public Entity nativeEntity() {
		return entity;
	}

	@Override
	public double x() {
		return entity.getX();
	}

	@Override
	public double y() {
		return entity.getY();
	}

	@Override
	public double z() {
		return entity.getZ();
	}

	@Override
	public void setVelocity(final double x, final double y, final double z) {
		entity.setDeltaMovement(x, y, z);
	}

	@Override
	public void markMovementChanged() {
		entity.hasImpulse = true;
	}

	@Override
	public boolean isLoaded() {
		return entity.level().isLoaded(entity.blockPosition());
	}

	@Override
	public boolean isClientSide() {
		return entity.level().isClientSide();
	}

	@Override
	public UUID uuid() {
		return entity.getUUID();
	}
}
