package com.example.vitalrelics.platform;

import com.example.vitalrelics.common.platform.MyAbstractArrow;
import com.example.vitalrelics.common.platform.MyLivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;

public final class NeoAbstractArrow extends NeoForgeEntity implements MyAbstractArrow {
	private final AbstractArrow arrow;

	public NeoAbstractArrow(final AbstractArrow arrow) {
		super(arrow);
		this.arrow = arrow;
	}

	@Override
	public MyLivingEntity owner() {
		if (!(arrow.getOwner() instanceof LivingEntity owner))
			return null;

		return new NeoLivingEntity(owner);
	}

	@Override
	public double velocityX() {
		return arrow.getDeltaMovement().x;
	}

	@Override
	public double velocityY() {
		return arrow.getDeltaMovement().y;
	}

	@Override
	public double velocityZ() {
		return arrow.getDeltaMovement().z;
	}

	@Override
	public double baseDamage() {
		return arrow.baseDamage;
	}

	@Override
	public void setBaseDamage(final double damage) {
		arrow.setBaseDamage(damage);
	}

	@Override
	public void setOwner(final MyLivingEntity owner) {
		if (!(owner instanceof NeoLivingEntity neoOwner))
			throw new IllegalArgumentException("Expected NeoLivingEntity");

		arrow.setOwner(neoOwner.nativeEntity());
	}

	@Override
	public void retarget(
			final MyLivingEntity newOwner,
			final double speedMultiplier,
			final double damageMultiplier,
			final double minimumDamageFromAttack) {

		if (!(newOwner instanceof NeoLivingEntity neoOwner))
			throw new IllegalArgumentException("Expected NeoLivingEntity");

		final LivingEntity newOwnerEntity = neoOwner.nativeEntity();
		final Entity oldOwner = arrow.getOwner();
		final Vec3 currentPos = arrow.position();
		final Vec3 direction;

		if (oldOwner != null) {
			final double distance = currentPos.distanceTo(oldOwner.position());
			final double baseHeight = oldOwner.getEyeHeight() * 0.9;
			final double extraHeight = Math.min(distance * 0.02, 4.0);
			final Vec3 ownerPos = oldOwner.position().add(0, baseHeight + extraHeight, 0);

			direction = ownerPos.subtract(currentPos).normalize();
		} else {
			direction = arrow.getDeltaMovement().normalize().scale(-1.0);
		}

		final double newSpeed = arrow.getDeltaMovement().length() * speedMultiplier;

		arrow.setDeltaMovement(direction.scale(newSpeed));
		arrow.needsSync = true;
		arrow.setOwner(newOwnerEntity);

		final double multipliedDamage = arrow.baseDamage * damageMultiplier;
		final double minimumDamage =
				newOwnerEntity.getAttributeValue(Attributes.ATTACK_DAMAGE) * minimumDamageFromAttack;

		arrow.setBaseDamage(Math.max(multipliedDamage, minimumDamage));
	}
}
