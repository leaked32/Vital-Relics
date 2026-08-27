package com.example.vitalrelics.common;

import com.example.vitalrelics.common.platform.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MySpellSystem {
	@FunctionalInterface
	public interface Handler {
		boolean activate(
				MyLivingEntity caster,
				Relic.Spells.Info spell,
				MyRuntimeUtils runtime
		);
	}

	public static final String CAST_SPELL = "cast_spell";
	public static final String SWITCH_SPELL_NEXT = "switch_spell_next";
	public static final String SWITCH_SPELL_PREVIOUS = "switch_spell_previous";

	public static final MySpellSystem INSTANCE = new MySpellSystem();

	private final Map<String, Handler> handlers = new HashMap<>();

	private MySpellSystem() {
		/*
		BLOCK hit
			-> try center for thin blocks
			-> otherwise try above
			-> if blocked, try before the hit face

		MISS / sky
			-> teleport as far along look direction as possible
		*/
		register(Relic.SPELL_TELEPORT, (caster, spell, runtime) -> {
			final double distance = Math.min(
					Math.max(
							RelicSpells.numberParameter(spell, "range", 0.0),
							0.0
					),
					256.0
			);

			if (distance <= 0.0)
				return false;

			final MyVec3 destination =
					runtime.safeDestinationAlongLook(caster, distance);

			if (destination == null)
				return false;

			caster.teleport(
					destination.x(),
					destination.y(),
					destination.z()
			);
			caster.playSound(MySound.TELEPORT);

			return true;
		});

		register(Relic.SPELL_CURSE, (caster, spell, runtime) -> {
			final float intensity = (float) RelicSpells.numberParameter(
					spell, "intensity", 0.0
			);

			final double range = Math.min(
					256.0,
					Math.max(
							0.0,
							RelicSpells.numberParameter(spell, "range", 0.0)
					)
			);

			if (intensity <= 0.0F || range <= 0.0)
				return false;

			final MyLivingEntity target =
					runtime.pointedLivingEntity(caster, range);

			if (target == null) {
				runtime.showCurseRequiresTarget(caster);
				return false;
			}

			if (caster.isAllied(target))
				return false;

			final float damage = intensity * caster.attackDamage();

			if (damage <= 0.0F)
				return false;

			MyDamageInfo.directAttack(caster, target, damage, 1);
			caster.playSound(MySound.ILLUSIONER_CAST);
			return true;
		});

		register(Relic.SPELL_HEAL, (caster, spell, runtime) -> {
			final float amount = (float) Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "amount", 0.0)
			);

			final float ratio = (float) Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "ratio", 0.0)
			);

			final float healing =
					amount + caster.maxHealth() * ratio;

			if (healing <= 0.0F || caster.health() >= caster.maxHealth())
				return false;

			caster.heal(healing);
			caster.playSound(MySound.PLAYER_LEVELUP);
			return true;
		});

		register(Relic.SPELL_CLEANSE, (caster, spell, runtime) -> {
			if (!MyUtils.cleanseEffects(
					caster,
					MyLivingEntity.MyEffectCategory.NEGATIVE
			))
				return false;

			caster.playSound(MySound.AMETHYST_CHIME);
			return true;
		});

		register(Relic.SPELL_DASH, (caster, spell, runtime) -> {
			final double strength = Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "strength", 0.0)
			);

			final double vertical = RelicSpells.numberParameter(
					spell,
					"vertical",
					0.0
			);

			if (strength <= 0.0)
				return false;

			final double lookX = caster.horizontalLookX();
			final double lookZ = caster.horizontalLookZ();
			final double lengthSqr = lookX * lookX + lookZ * lookZ;

			if (lengthSqr <= 1.0e-8)
				return false;

			final double inverseLength = 1.0 / Math.sqrt(lengthSqr);

			caster.setVelocity(
					lookX * inverseLength * strength,
					vertical,
					lookZ * inverseLength * strength
			);

			caster.markMovementChanged();
			caster.playSound(MySound.DRAGON_FLAP);
			return true;
		});

		register(Relic.SPELL_ARC_BURST, (caster, spell, runtime) -> {
			final float intensity = (float) Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "intensity", 0.0)
			);

			final int range = (int) Math.min(
					256,
					Math.max(
							0,
							Math.round(
									RelicSpells.numberParameter(
											spell,
											"range",
											0.0
									)
							)
					)
			);

			final int count = (int) Math.min(
					64,
					Math.max(
							0,
							Math.round(
									RelicSpells.numberParameter(
											spell,
											"count",
											1.0
									)
							)
					)
			);

			final int weaken = (int) Math.min(
					255,
					Math.max(
							0,
							Math.round(
									RelicSpells.numberParameter(
											spell,
											"weaken",
											0.0
									)
							)
					)
			);

			if (intensity <= 0.0F || range <= 0 || count <= 0)
				return false;

			final float damage = caster.attackDamage() * intensity;

			if (damage <= 0.0F)
				return false;

			MyDamageInfo.directRangedAttack(
					caster,
					damage,
					range,
					count,
					weaken
			);

			caster.playSound(MySound.EVOKER_CAST);
			return true;
		});

		register(Relic.SPELL_REPULSE, (caster, spell, runtime) -> {
			final double range = Math.min(
					64.0,
					Math.max(
							0.0,
							RelicSpells.numberParameter(spell, "range", 0.0)
					)
			);

			final double strength = Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "strength", 0.0)
			);

			final double vertical = Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "vertical", 0.0)
			);

			if (range <= 0.0 || strength <= 0.0)
				return false;

			boolean affected = false;

			for (final MyLivingEntity target :
					MyDamageInfo.getLivingEntitiesInRange(caster, range)) {

				if (!caster.hostileTargeted(target))
					continue;

				final double dx = target.x() - caster.x();
				final double dz = target.z() - caster.z();
				final double lengthSqr = dx * dx + dz * dz;

				if (lengthSqr <= 1.0e-8)
					continue;

				final double inverseLength = 1.0 / Math.sqrt(lengthSqr);

				target.push(
						dx * inverseLength * strength,
						vertical,
						dz * inverseLength * strength
				);

				target.markMovementChanged();
				affected = true;
			}

			if (!affected)
				return false;

			caster.playSound(MySound.GENERIC_EXPLODE);
			return true;
		});

		register(Relic.SPELL_ABSORPTION, (caster, spell, runtime) -> {
			final int durationTicks = (int) Math.min(
					20 * 60 * 10,
					Math.max(
							1,
							Math.round(
									RelicSpells.numberParameter(
											spell,
											"duration_ticks",
											200.0
									)
							)
					)
			);

			final int amplifier = (int) Math.min(
					255,
					Math.max(
							0,
							Math.round(
									RelicSpells.numberParameter(
											spell,
											"amplifier",
											0.0
									)
							)
					)
			);

			caster.addEffect(
					MyEffect.ABSORPTION,
					durationTicks,
					amplifier
			);

			caster.playSound(MySound.BEACON_ACTIVATE);
			return true;
		});

		register(Relic.SPELL_SKY_LAUNCH, (caster, spell, runtime) -> {
			final double range = Math.min(
					64.0,
					Math.max(
							0.0,
							RelicSpells.numberParameter(spell, "range", 0.0)
					)
			);

			final double strength = Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "strength", 0.0)
			);

			if (range <= 0.0 || strength <= 0.0)
				return false;

			boolean affected = false;

			for (final MyLivingEntity target :
					MyDamageInfo.getLivingEntitiesInRange(caster, range)) {

				if (!caster.hostileTargeted(target))
					continue;

				target.push(0.0, strength, 0.0);
				target.markMovementChanged();
				affected = true;
			}

			if (!affected)
				return false;

			caster.playSound(MySound.DRAGON_FLAP);
			return true;
		});

		register(Relic.SPELL_SHADOW_EXCHANGE, (caster, spell, runtime) -> {
			final double range = Math.min(
					64.0,
					Math.max(
							0.0,
							RelicSpells.numberParameter(spell, "range", 0.0)
					)
			);

			if (range <= 0.0)
				return false;

			final MyLivingEntity target =
					runtime.pointedLivingEntity(caster, range);

			if (target == null || caster.isAllied(target))
				return false;

			final double casterX = caster.x();
			final double casterY = caster.y();
			final double casterZ = caster.z();

			final double targetX = target.x();
			final double targetY = target.y();
			final double targetZ = target.z();

			caster.teleport(targetX, targetY, targetZ);
			target.teleport(casterX, casterY, casterZ);

			caster.playSound(MySound.TELEPORT);
			target.playSound(MySound.TELEPORT);
			return true;
		});

		register(Relic.SPELL_PHANTOM_STEP, (caster, spell, runtime) -> {
			final double range = Math.min(
					32.0,
					Math.max(
							0.0,
							RelicSpells.numberParameter(spell, "range", 0.0)
					)
			);

			final float intensity = (float) Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "intensity", 0.0)
			);

			if (range <= 0.0 || intensity <= 0.0F)
				return false;

			final MyVec3 origin =
					new MyVec3(caster.x(), caster.y(), caster.z());

			final MyVec3 destination =
					runtime.safeHorizontalDestination(caster, range);

			if (destination == null)
				return false;

			final float damage =
					caster.attackDamage() * intensity;

			for (final MyLivingEntity target :
					runtime.entitiesIntersectingMovement(
							caster,
							origin,
							destination,
							caster.width() * 0.5
					)) {

				if (!caster.hostileTargeted(target))
					continue;

				MyDamageInfo.directAttack(
						caster,
						target,
						damage,
						1
				);
			}

			caster.teleport(
					destination.x(),
					destination.y(),
					destination.z()
			);

			caster.playSound(MySound.TELEPORT);
			return true;
		});

		register(Relic.SPELL_UPGRADE_ENCHANTED_BOOK, (caster, spell, runtime) -> {
			final int experienceCost = Math.max(
					0,
					(int) Math.round(
							RelicSpells.numberParameter(
									spell,
									"experience_cost",
									0.0
							)
					)
			);

			if (!runtime.upgradeFirstStoredEnchantment(
					caster,
					experienceCost
			))
				return false;

			caster.playSound(MySound.ENCHANTMENT_TABLE_USE);
			return true;
		});
	}

	private void register(final String abilityId, final Handler handler) {
		if (handlers.putIfAbsent(abilityId, handler) != null) {
			throw new IllegalStateException(
					"Spell already registered: " + abilityId
			);
		}
	}

	public void syncSpellHud(
			final MyLivingEntity caster,
			final MyRuntimeUtils runtime) {

		final int tick = caster.serverTick();
		if (tick < 0)
			return;

		final Map<String, Relic.Spells.Info> spells =
				RelicSpells.gatherSpells(runtime.gatherRelics(caster));

		final List<String> spellIds = new ArrayList<>(spells.keySet());

		final String selected =
				Scheduler.INSTANCE().selectedSpell(
						caster.uuid(),
						spellIds,
						tick
				);

		if (selected == null) {
			runtime.clearSpellHud(caster);
			return;
		}

		syncSpellHud(caster, selected, tick, runtime);
	}

	private void syncSpellHud(
			final MyLivingEntity caster,
			final String spellId,
			final int tick,
			final MyRuntimeUtils runtime) {

		final int cooldownTicks =
				Scheduler.INSTANCE().getSpellCooldownRemaining(
						caster.uuid(), spellId, tick
				);

		runtime.syncSpellHud(caster, spellId, cooldownTicks);
	}

	public void activate(
			final MyLivingEntity caster,
			String abilityId,
			final MyRuntimeUtils runtime) {

		if (caster.isClientSide() ||
				!abilityId.matches("[a-z0-9_./-]{1,64}")) {
			return;
		}

		final int tick = caster.serverTick();
		if (tick < 0)
			return;

		final Map<String, Relic.Spells.Info> spells =
				RelicSpells.gatherSpells(runtime.gatherRelics(caster));

		final List<String> spellIds = new ArrayList<>(spells.keySet());

		if (abilityId.equals(SWITCH_SPELL_NEXT) ||
				abilityId.equals(SWITCH_SPELL_PREVIOUS)) {

			final int direction =
					abilityId.equals(SWITCH_SPELL_NEXT) ? 1 : -1;

			final String selected = Scheduler.INSTANCE().selectSpell(
					caster.uuid(),
					spellIds,
					direction,
					tick
			);

			if (selected != null) {
				syncSpellHud(caster, selected, tick, runtime);
				runtime.showSelectedSpell(caster, selected);
			}
			return;
		}

		if (!abilityId.equals(CAST_SPELL))
			return;

		abilityId = Scheduler.INSTANCE().selectedSpell(
				caster.uuid(),
				spellIds,
				tick
		);

		if (abilityId == null)
			return;

		final Relic.Spells.Info spell = spells.get(abilityId);

		if (spell == null)
			return;

		final int remainingTicks =
				Scheduler.INSTANCE().getSpellCooldownRemaining(
						caster.uuid(), abilityId, tick
				);

		if (remainingTicks > 0) {
			runtime.showSpellCooldown(
					caster,
					abilityId,
					remainingTicks
			);
			return;
		}

		final Handler handler = handlers.get(abilityId);

		if (handler != null && handler.activate(caster, spell, runtime)) {
			Scheduler.INSTANCE().setSpellCooldown(
					caster.uuid(),
					abilityId,
					tick,
					RelicSpells.cooldownTicks(spell)
			);

			syncSpellHud(caster, abilityId, tick, runtime);
		}
	}
}
