package com.example.vitalrelics.common.platform;

public interface MyDamageSource {
	MyLivingEntity attacker();

	MyDamageKind kind();

	default boolean isExtraDamage() {
		return kind() == MyDamageKind.EXTRA_DAMAGE;
	}

	enum MyDamageKind {
		EXTRA_DAMAGE,
		OTHER
	}
}
