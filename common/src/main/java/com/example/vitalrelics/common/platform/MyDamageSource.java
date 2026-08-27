package com.example.vitalrelics.common.platform;

public record MyDamageSource(
		MyLivingEntity attacker,
		MyDamageKind kind) {

	public boolean isExtraDamage() {
		return kind == MyDamageKind.EXTRA_DAMAGE;
	}
}
