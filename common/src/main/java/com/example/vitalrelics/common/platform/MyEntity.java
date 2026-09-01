package com.example.vitalrelics.common.platform;

import java.util.List;
import java.util.UUID;

public interface MyEntity {
	double x();
	double y();
	double z();
	double height();

	void moveTo(double x, double y, double z);
	void setVelocity(double x, double y, double z);
	void markMovementChanged();

	boolean isLoaded();
	boolean isClientSide();

	List<MyEntity> entitiesInRange(double radius);

	UUID uuid();
}
