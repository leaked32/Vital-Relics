package com.example.vitalrelics.network;

import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicSpells;
import com.example.vitalrelics.common.scheduled.Scheduler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

import static com.example.vitalrelics.Utils.gatherRelics;

public final class SpellSystem {
	@FunctionalInterface
	public interface Handler {
		boolean activate(ServerPlayer player, Relic.Spells.Info spell);
	}

	private static final Map<String, Handler> HANDLERS = new HashMap<>();

	private SpellSystem() {}

	public static void register(final String abilityId, final Handler handler) {
		if (HANDLERS.putIfAbsent(abilityId, handler) != null) {
			throw new IllegalStateException(
					"Spell already registered: " + abilityId
			);
		}
	}



	public static final String CAST_SPELL = "cast_spell";
	public static final String SWITCH_SPELL = "switch_spell";

	public static void activate(final ServerPlayer player, final String abilityId) {
		if (!abilityId.matches("[a-z0-9_./-]{1,64}"))
			return;

		final Relic.Spells.Info spell =
				RelicSpells.gatherSpells(gatherRelics(player)).get(abilityId);

		if (spell == null ||
				Scheduler.INSTANCE().isSpellCoolingDown(
						player.getUUID(), abilityId, player.getServer().getTickCount()
				)) {
			return;
		}

		final Handler handler = HANDLERS.get(abilityId);

		if (handler != null && handler.activate(player, spell)) {
			Scheduler.INSTANCE().setSpellCooldown(
					player.getUUID(),
					abilityId,
					player.getServer().getTickCount(),
					RelicSpells.cooldownTicks(spell)
			);
		}
	}

	static {
		register("teleport", (player, spell) -> {
			final double distance = RelicSpells.numberParameter(spell, "intensity", 0.0);

			if (distance <= 0.0)
				return false;

			final ServerLevel level = player.serverLevel();
			final Vec3 origin = player.position();
			final Vec3 direction = player.getLookAngle().normalize();
			Vec3 destination = origin;

			for (double travelled = 0.5;
			     travelled <= distance;
			     travelled += 0.5) {

				final Vec3 candidate =
						origin.add(direction.scale(travelled));
				final AABB targetBox =
						player.getBoundingBox().move(candidate.subtract(origin));

				if (!level.noCollision(player, targetBox))
					break;

				destination = candidate;
			}

			if (!destination.equals(origin)) {
				player.teleportTo(
						level,
						destination.x, destination.y, destination.z,
						player.getYRot(), player.getXRot()
				);
				return true;
			}

			return false;
		});
	}
}