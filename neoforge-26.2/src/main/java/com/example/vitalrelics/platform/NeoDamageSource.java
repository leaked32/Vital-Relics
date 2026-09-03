package com.example.vitalrelics.platform;

import com.example.vitalrelics.common.platform.MyDamageSource;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class NeoDamageSource implements MyDamageSource {
	private final DamageSource source;

	public NeoDamageSource(final DamageSource source) {
		this.source = source;
	}

	public DamageSource nativeSource() {
		return source;
	}

	@Override
	public MyLivingEntity attacker() {
		final Entity entity = source.getEntity();
		return entity instanceof LivingEntity living
				? new NeoLivingEntity(living)
				: null;
	}

	@Override
	public boolean isExtraDamage() {
		return source.toString().contains("vitalrelics.extra_damage");
	}
}
