package com.example.vitalrelics.platform;

import com.example.vitalrelics.common.platform.MyDamageKind;
import com.example.vitalrelics.common.platform.MyDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import static com.example.vitalrelics.VitalRelics.LOGGER;

public final class NeoDamageSource {
	private static final String DAMAGE_STR =
			"DamageSource (vitalrelics.extra_damage)";

	private NeoDamageSource() {}

	public static MyDamageSource wrap(final DamageSource source) {
		final var nativeAttacker = source.getEntity();

		if (!(nativeAttacker instanceof LivingEntity attacker)) {
			return new MyDamageSource(null, kind(source));
		}

		return new MyDamageSource(
				new NeoLivingEntity(attacker),
				kind(source)
		);
	}

	private static MyDamageKind kind(final DamageSource source) {
		final String message_id = source.toString().trim();
		LOGGER.info("Damage Type is: " + message_id);

		if (message_id.equals(DAMAGE_STR) ||
				message_id.equals("DamageSource (revelationfix.fe_power.0)")) {

			return MyDamageKind.EXTRA_DAMAGE;
		}

		return MyDamageKind.OTHER;
	}
}
