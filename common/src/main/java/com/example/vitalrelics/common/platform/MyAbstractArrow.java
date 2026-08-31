package com.example.vitalrelics.common.platform;

public interface MyAbstractArrow {
	MyLivingEntity owner();

	double x();
	double y();
	double z();

	double velocityX();
	double velocityY();
	double velocityZ();
	void setVelocity(double x, double y, double z);
	void markMovementChanged();

	double baseDamage();
	void setBaseDamage(double damage);

	void setOwner(MyLivingEntity owner);
}
