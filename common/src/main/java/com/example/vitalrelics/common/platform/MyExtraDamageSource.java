package com.example.vitalrelics.common.platform;

public record MyExtraDamageSource(MyLivingEntity attacker) implements MyDamageSource {
	@Override
	public MyDamageKind kind() {
		return MyDamageKind.EXTRA_DAMAGE;
	}
}
