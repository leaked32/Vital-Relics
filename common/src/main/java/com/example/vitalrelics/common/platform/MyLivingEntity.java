package com.example.vitalrelics.common.platform;

import java.util.List;
import java.util.UUID;

public interface MyLivingEntity {
	void teleport(double x, double y, double z);

	void playSound(MySound sound);

	boolean hurt(MyDamageSource source, float amount);

	void resetInvulnerableTime();

	float health();

	float maxHealth();

	float attackDamage();

	double x();

	double z();

	void push(double x, double y, double z);

	void markMovementChanged();

	boolean isDeadOrDying();

	boolean isLoaded();

	boolean isClientSide();

	boolean is(MyLivingEntity other);

	boolean isAllied(MyLivingEntity other);

	boolean hostileTargeted(MyLivingEntity other);

	List<MyLivingEntity> livingEntitiesInRange(double radius);

	void addEffect(MyEffect effect, int duration, int amplifier);

	UUID uuid();

	String typeId();

	int serverTick();

	void logInfo(String message);
}
