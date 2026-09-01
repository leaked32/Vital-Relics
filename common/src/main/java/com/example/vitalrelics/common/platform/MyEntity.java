package com.example.vitalrelics.common.platform;

import java.util.UUID;

public interface MyEntity {
	double x();
	double y();
	double z();

	void setVelocity(double x, double y, double z);
	void markMovementChanged();

	boolean isLoaded();
	boolean isClientSide();

	UUID uuid();
}
