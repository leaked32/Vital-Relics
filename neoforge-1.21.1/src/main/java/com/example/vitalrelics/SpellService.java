package com.example.vitalrelics;

import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicSpells;
import com.example.vitalrelics.common.scheduled.Scheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

import static com.example.vitalrelics.Utils.gatherRelics;

public final class SpellService {
	@FunctionalInterface
	private interface SpellHandler {
		boolean cast(ServerPlayer player, Relic.Spells.Info spell);
	}

	/*
	 * New spell IDs only need one handler registration here. Each handler
	 * receives the best equipped configuration for its own ID.
	 */
	private static final Map<String, SpellHandler> SPELL_HANDLERS = Map.of(
			"teleport", SpellService::teleport
	);

	private SpellService() {}

	public static void cast(
			final ServerPlayer player,
			final String spellId) {

		if (!spellId.matches("[a-z0-9_./-]{1,64}"))
			return;

		final Relic.Spells.Info spell =
				RelicSpells.gatherSpells(gatherRelics(player)).get(spellId);

		if (spell == null ||
				Scheduler.INSTANCE().isSpellCoolingDown(
						player.getUUID(), spellId, player.getServer().getTickCount())) {
			return;
		}

		final SpellHandler handler = SPELL_HANDLERS.get(spellId);

		if (handler != null && handler.cast(player, spell)) {
			Scheduler.INSTANCE().setSpellCooldown(
					player.getUUID(),
					spellId,
					player.getServer().getTickCount(),
					RelicSpells.cooldownTicks(spell)
			);
		}
	}

	private static boolean teleport(
			final ServerPlayer player,
			final Relic.Spells.Info spell) {

		final double distance = Math.clamp(
				RelicSpells.numberParameter(spell, "intensity", 0.0),
				0.0,
				64.0
		);

		if (distance <= 0.0)
			return false;

		final ServerLevel level = player.serverLevel();
		final Vec3 origin = player.position();
		final Vec3 direction = player.getLookAngle().normalize();
		Vec3 destination = origin;

		for (double travelled = 0.5; travelled <= distance; travelled += 0.5) {
			final Vec3 candidate = origin.add(direction.scale(travelled));
			final AABB targetBox = player.getBoundingBox().move(candidate.subtract(origin));

			if (!level.noCollision(player, targetBox))
				break;

			destination = candidate;
		}

		if (destination.equals(origin))
			return false;

		player.teleportTo(
				level,
				destination.x,
				destination.y,
				destination.z,
				player.getYRot(),
				player.getXRot()
		);
		return true;
	}
}
