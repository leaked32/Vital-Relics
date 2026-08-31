package com.example.vitalrelics.platform;

import com.example.vitalrelics.common.platform.MyDamageSource;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ForgeDamageSource implements MyDamageSource {
	private final DamageSource source;

	public ForgeDamageSource(final DamageSource source) {
		this.source = source;
	}

	public DamageSource nativeSource() {
		return source;
	}

	@Override
	public MyLivingEntity attacker() {
		final Entity entity = source.getEntity();
		return entity instanceof LivingEntity living
				? new ForgeLivingEntity(living)
				: null;
	}

	@Override
	public boolean isExtraDamage() {
		return source.toString().contains("vitalrelics.extra_damage");
	}
}
