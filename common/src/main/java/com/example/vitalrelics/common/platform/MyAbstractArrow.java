package com.example.vitalrelics.common.platform;

public interface MyAbstractArrow extends MyEntity {
	MyLivingEntity owner();

	double velocityX();
	double velocityY();
	double velocityZ();

	double baseDamage();
	void setBaseDamage(double damage);
	void setNoGravity(boolean noGravity);
	void discard();

	void setOwner(MyLivingEntity owner);

	void retarget(
			MyLivingEntity newOwner,
			double speedMultiplier,
			double damageMultiplier,
			double minimumDamageFromAttack);
}
