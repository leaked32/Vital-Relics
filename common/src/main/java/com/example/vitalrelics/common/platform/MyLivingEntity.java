package com.example.vitalrelics.common.platform;

import java.util.List;

public interface MyLivingEntity extends MyEntity {
	void teleport(double x, double y, double z);

	void playSound(MySound sound);

	MyDamageSource extraDamageSource();
	boolean hurt(MyDamageSource source, float amount);
	boolean hurtThorns(MyLivingEntity source, float amount);

	void resetInvulnerable();

	void resetInvulnerableTime();
	int invulnerableTime();
	void setInvulnerableTime(int ticks);

	void setHealth(float health);
	void setHurtMark(MyDamageSource source);
	float health();
	float maxHealth();
	float attackDamage();
	boolean isOnFire();
	void clearFire();

	void heal(float amount);
	void feed(int nutrition, float saturation);
	void mendEquipment(int level);

	double horizontalLookX();
	double horizontalLookZ();

	void push(double x, double y, double z);

	boolean isDeadOrDying();
	boolean isServerPlayer();
	boolean is(MyLivingEntity other);
	boolean isAllied(MyLivingEntity other);
	boolean isHostileTargeted(MyLivingEntity other);

	List<MyLivingEntity> livingEntitiesInRange(double radius);

	void addEffect(MyEffect effect, int duration, int amplifier);

	String typeId();

	int serverTick();

	double width();

	enum MyEffectCategory {
		POSITIVE,
		NEGATIVE,
		NEUTRAL,
		ALL
	}
	record MyEffectInstance(
			String id,
			MyEffectCategory category) {
	}
	List<MyEffectInstance> activeEffects();

	void removeEffect(String id);

	void addEffect(String id, int duration, int amplifier, boolean ambient, boolean visible);
}
