package com.example.vitalrelics.common.platform;

public record MyExtraDamageSource(MyLivingEntity attacker) implements MyDamageSource {
	@Override
	public boolean isExtraDamage() {
		return true;
	}
}
