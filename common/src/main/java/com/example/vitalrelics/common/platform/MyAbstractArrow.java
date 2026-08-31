package com.example.vitalrelics.common.platform;

public interface MyAbstractArrow {
	double baseDamage();
	void setBaseDamage(double damage);
	void scaleVelocity(double multiplier);

	void retarget(
			MyLivingEntity newOwner,
			double speedMultiplier,
			double damageMultiplier,
			double minimumDamageFromAttack);
}
