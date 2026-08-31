package com.example.vitalrelics.platform;

import com.example.vitalrelics.Utils;
import com.example.vitalrelics.common.platform.MyAbstractArrow;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;

public final class ForgeAbstractArrow implements MyAbstractArrow {
	private final AbstractArrow arrow;

	public ForgeAbstractArrow(final AbstractArrow arrow) {
		this.arrow = arrow;
	}

	@Override
	public double baseDamage() {
		return arrow.getBaseDamage();
	}

	@Override
	public void setBaseDamage(final double damage) {
		arrow.setBaseDamage(damage);
	}

	@Override
	public void scaleVelocity(final double multiplier) {
		arrow.setDeltaMovement(arrow.getDeltaMovement().scale(multiplier));
	}

	@Override
	public void retarget(
			final MyLivingEntity newOwner,
			final double speedMultiplier,
			final double damageMultiplier,
			final double minimumDamageFromAttack) {

		Utils.retargetArrow(
				arrow,
				((ForgeLivingEntity) newOwner).entity,
				speedMultiplier,
				damageMultiplier,
				minimumDamageFromAttack
		);
	}
}
