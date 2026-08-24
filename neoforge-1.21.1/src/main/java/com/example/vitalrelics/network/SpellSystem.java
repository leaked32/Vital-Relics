package com.example.vitalrelics.network;

import com.example.vitalrelics.MyDamageInfo;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicSpells;
import com.example.vitalrelics.common.RelicTranslations;
import com.example.vitalrelics.common.scheduled.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.*;

import static com.example.vitalrelics.Utils.*;

public final class SpellSystem {
	@FunctionalInterface
	public interface Handler {
		boolean activate(LivingEntity livingEntity, Relic.Spells.Info spell);
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

	private static Component message(
			final String key,
			final String fallback,
			final Object... arguments) {

		final String pattern =
				RelicTranslations.INSTANCE.translate(key, fallback);

		return Component.literal(
				String.format(Locale.ROOT, pattern, arguments)
		);
	}

	private static String spellName(final String id) {
		return RelicTranslations.INSTANCE.translate(
				"relic.vitalrelics.spell." + id,
				Relic.itemDisplayName(id)
		);
	}


	public static final String CAST_SPELL = "cast_spell";
	public static final String SWITCH_SPELL_NEXT = "switch_spell_next";
	public static final String SWITCH_SPELL_PREVIOUS = "switch_spell_previous";

	public static void activate(
			final LivingEntity caster,
			String abilityId) {

		if (caster.level().isClientSide() ||
				!abilityId.matches("[a-z0-9_./-]{1,64}")) {
			return;
		}

		if (!(caster.level() instanceof ServerLevel level))
			return;

		final int tick = level.getServer().getTickCount();

		final Map<String, Relic.Spells.Info> spells =
				RelicSpells.gatherSpells(gatherRelics(caster));

		final List<String> spellIds = new ArrayList<>(spells.keySet());

		if (abilityId.equals(SWITCH_SPELL_NEXT) ||
				abilityId.equals(SWITCH_SPELL_PREVIOUS)) {

			final int direction = abilityId.equals(SWITCH_SPELL_NEXT) ? 1 : -1;

			final String selected = Scheduler.INSTANCE().selectSpell(
					caster.getUUID(),
					spellIds,
					direction,
					tick
			);

			if (caster instanceof ServerPlayer player && selected != null) {
				player.displayClientMessage(
						message(
								"message.vitalrelics.selected_spell",
								"Selected spell: %s",
								spellName(selected)
						),
						true
				);
			}
			return;
		}

		if (abilityId.equals(CAST_SPELL)) {
			abilityId = Scheduler.INSTANCE().selectedSpell(
					caster.getUUID(),
					spellIds,
					tick
			);

			if (abilityId == null) {
				return;
			}

			final Relic.Spells.Info spell = spells.get(abilityId);

			if (spell == null) {
				return;
			}

			final int remainingTicks =
					Scheduler.INSTANCE().getSpellCooldownRemaining(
							caster.getUUID(), abilityId, tick
					);

			if (remainingTicks > 0) {
				if (caster instanceof ServerPlayer player) {
					player.displayClientMessage(
							message(
									"message.vitalrelics.spell_cooldown",
									"%s cooldown: %s",
									spellName(abilityId),
									String.format(
											Locale.ROOT,
											"%.1fs",
											remainingTicks / 20.0
									)
							),
							true
					);
				}
				return;
			}

			final Handler handler = HANDLERS.get(abilityId);

			if (handler != null && handler.activate(caster, spell)) {
				Scheduler.INSTANCE().setSpellCooldown(
						caster.getUUID(),
						abilityId,
						tick,
						RelicSpells.cooldownTicks(spell)
				);
			}
		}
	}

	static {
		register("teleport", (caster, spell) -> {
			final double distance = Math.clamp(
					RelicSpells.numberParameter(spell, "range", 0.0),
					0.0,
					256.0
			);

			if (distance <= 0.0)
				return false;

			if (!(caster.level() instanceof ServerLevel level))
				return false;

			final Vec3 origin = caster.position();
			final Vec3 rayOrigin = caster.getEyePosition();
			final Vec3 direction = caster.getLookAngle().normalize();

			final BlockHitResult blockHit = level.clip(new ClipContext(
					rayOrigin,
					rayOrigin.add(direction.scale(distance)),
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					caster
			));

			if (blockHit.getType() == HitResult.Type.BLOCK) {
				final BlockPos support = blockHit.getBlockPos();
				final Vec3 candidate = new Vec3(
						support.getX() + 0.5,
						support.getY() + 1.0,
						support.getZ() + 0.5
				);

				final AABB targetBox =
						caster.getBoundingBox().move(candidate.subtract(origin));

				if (level.noCollision(caster, targetBox)) {
					teleport(caster, level, candidate);
					return true;
				}
			}

			Vec3 destination = origin;

			for (double travelled = 0.5;
			     travelled <= distance;
			     travelled += 0.5) {

				final Vec3 candidate =
						origin.add(direction.scale(travelled));

				final AABB targetBox =
						caster.getBoundingBox().move(candidate.subtract(origin));

				if (!level.noCollision(caster, targetBox))
					break;

				destination = candidate;
			}

			if (destination.equals(origin))
				return false;

			teleport(caster, level, destination);
			return true;
		});

		register("curse", (caster, spell) -> {
			final float intensity = (float) RelicSpells.numberParameter(
					spell, "intensity", 0.0
			);

			final double range = Math.clamp(
					RelicSpells.numberParameter(spell, "range", 0.0),
					0.0,
					256.0
			);

			if (intensity <= 0.0F || range <= 0.0)
				return false;

			if (!(caster.level() instanceof ServerLevel level))
				return false;

			final LivingEntity target = pointedLivingEntity(caster, level, range);

			if (target == null) {
				if (caster instanceof ServerPlayer player) {
					player.displayClientMessage(
							message(
									"message.vitalrelics.curse_requires_target",
									"Curse requires a target."
							),
							true
					);
				}
				return false;
			}

			if (isAllied(caster, target))
				return false;

			final float damage = intensity / 100.0F *
					(float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE);

			if (damage <= 0.0F)
				return false;

			MyDamageInfo.directAttack(caster, target, damage, 1);

			level.playSound(
					null,
					caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS,
					0.8F, 1.15F
			);

			return true;
		});
	}
}