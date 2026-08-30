package com.example.vitalrelics.common.platform;

import java.util.List;
import java.util.UUID;

public interface MyLivingEntity {
	void teleport(double x, double y, double z);

	void playSound(MySound sound);

	boolean hurt(MyDamageSource source, float amount);
	boolean hurtThorns(MyLivingEntity source, float amount);

	void resetInvulnerableTime();
	int invulnerableTime();
	void setInvulnerableTime(int ticks);

	void setHealth(float health);
	void setHurtMark(MyDamageSource source);
	float health();
	float maxHealth();
	float attackDamage();

	void heal(float amount);
	void feed(int nutrition, float saturation);
	void mendEquipment(int level);

	double x();
	double y();
	double z();

	double horizontalLookX();
	double horizontalLookZ();

	void setVelocity(double x, double y, double z);
	void push(double x, double y, double z);
	void markMovementChanged();

	boolean isDeadOrDying();
	boolean isLoaded();
	boolean isClientSide();
	boolean isServerPlayer();
	boolean is(MyLivingEntity other);
	boolean isAllied(MyLivingEntity other);
	boolean isHostileTargeted(MyLivingEntity other);

	List<MyLivingEntity> livingEntitiesInRange(double radius);

	void addEffect(MyEffect effect, int duration, int amplifier);

	UUID uuid();

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
