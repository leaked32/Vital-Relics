package com.example.vitalrelics.network;

import com.example.vitalrelics.MyDamageInfo;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicSpells;
import com.example.vitalrelics.common.RelicTranslations;
import com.example.vitalrelics.common.scheduled.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

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

			final int direction =
					abilityId.equals(SWITCH_SPELL_NEXT) ? 1 : -1;

			final String selected = Scheduler.INSTANCE().selectSpell(
					caster.getUUID(), spellIds, direction, tick
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

		if (!abilityId.equals(CAST_SPELL))
			return;

		abilityId = Scheduler.INSTANCE().selectedSpell(
				caster.getUUID(), spellIds, tick
		);

		if (abilityId == null)
			return;

		final Relic.Spells.Info spell = spells.get(abilityId);

		if (spell == null)
			return;

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

	static {

		/*
		BLOCK hit
			-> try center for thin blocks
			-> otherwise try above
			-> if blocked, try before the hit face

		MISS / sky
			-> teleport as far along look direction as possible
		*/
		register(Relic.SPELL_TELEPORT, (caster, spell) -> {
			final double distance = Math.min(
					Math.max(RelicSpells.numberParameter(spell, "range", 0.0), 0.0),
					256.0
			);

			if (distance <= 0.0)
				return false;

			if (!(caster.level() instanceof ServerLevel level))
				return false;

			final Vec3 origin = caster.position();
			final Vec3 rayOrigin = caster.getEyePosition();
			final Vec3 direction = caster.getLookAngle().normalize();
			final Vec3 rayEnd = rayOrigin.add(direction.scale(distance));

			/*
			 * Raycast while treating snow as transparent.
			 */
			Vec3 clipStart = rayOrigin;
			BlockHitResult hit;

			while (true) {
				hit = level.clip(new ClipContext(
						clipStart,
						rayEnd,
						ClipContext.Block.COLLIDER,
						ClipContext.Fluid.NONE,
						caster
				));

				if (hit.getType() != HitResult.Type.BLOCK)
					break;

				if (!level.getBlockState(hit.getBlockPos()).is(Blocks.SNOW))
					break;

				/*
				 * Move slightly beyond the snow collision and continue the ray.
				 */
				clipStart = hit.getLocation().add(direction.scale(0.01));

				if (clipStart.distanceToSqr(rayOrigin) >=
						rayEnd.distanceToSqr(rayOrigin)) {

					hit = BlockHitResult.miss(
							rayEnd,
							Direction.getNearest(
									direction.x,
									direction.y,
									direction.z
							),
							BlockPos.containing(rayEnd)
					);

					break;
				}
			}

			/*
			 * Sky / no block:
			 * teleport as far along the look direction as possible.
			 */
			if (hit.getType() == HitResult.Type.MISS) {
				for (double travelled = distance;
				     travelled >= 0.5;
				     travelled -= 0.25) {

					final Vec3 candidate =
							origin.add(direction.scale(travelled));

					final AABB targetBox =
							caster.getBoundingBox()
									.move(candidate.subtract(origin));

					if (!level.noCollision(caster, targetBox))
						continue;

					teleport(caster, level, candidate);
					return true;
				}

				return false;
			}

			final BlockPos target = hit.getBlockPos();
			final BlockState state = level.getBlockState(target);

			/*
			 * Thin blocks:
			 * teleport into the center of their block cell.
			 */
			final boolean centerTarget =
					state.getBlock() instanceof DoorBlock ||
							state.getBlock() instanceof TrapDoorBlock ||
							state.getBlock() instanceof IronBarsBlock ||
							state.getBlock() instanceof StainedGlassPaneBlock;

			if (centerTarget) {
				final Vec3 candidate = new Vec3(
						target.getX() + 0.5,
						target.getY(),
						target.getZ() + 0.5
				);

				teleport(caster, level, candidate);
				return true;
			}

			/*
			 * Normal block:
			 * try standing on its actual collision surface.
			 */
			final VoxelShape shape =
					state.getCollisionShape(level, target);

			if (!shape.isEmpty()) {
				final double topY =
						target.getY() + shape.max(Direction.Axis.Y);

				final Vec3 above = new Vec3(
						target.getX() + 0.5,
						topY + 1.0e-4,
						target.getZ() + 0.5
				);

				final AABB targetBox =
						caster.getBoundingBox()
								.move(above.subtract(origin));

				if (level.noCollision(caster, targetBox)) {
					teleport(caster, level, above);
					return true;
				}
			}

			/*
			 * No room above:
			 * teleport BEFORE the target block.
			 *
			 * hit.getDirection() points toward the side from which the ray
			 * entered the target block, so this is the caster-facing side.
			 */
			final Direction beforeDirection =
					hit.getDirection();

			final BlockPos before =
					target.relative(beforeDirection);

			Vec3 candidate = new Vec3(
					before.getX() + 0.5,
					before.getY(),
					before.getZ() + 0.5
			);

			/*
			 * Move a tiny amount farther away from the target block to avoid
			 * floating-point boundary overlap with its collision shape.
			 */
			candidate = candidate.add(
					beforeDirection.getStepX() * 1.0e-4,
					beforeDirection.getStepY() * 1.0e-4,
					beforeDirection.getStepZ() * 1.0e-4
			);

			final AABB targetBox =
					caster.getBoundingBox()
							.move(candidate.subtract(origin));

			if (!level.noCollision(caster, targetBox))
				return false;

			teleport(caster, level, candidate);
			return true;
		});

		register(Relic.SPELL_CURSE, (caster, spell) -> {
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
		register(Relic.SPELL_HEAL, (caster, spell) -> {
			final float amount = (float) Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "amount", 0.0)
			);

			final float ratio = (float) Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "ratio", 0.0)
			);

			final float healing =
					amount + caster.getMaxHealth() * ratio;

			if (healing <= 0.0F || caster.getHealth() >= caster.getMaxHealth())
				return false;

			caster.heal(healing);

			caster.level().playSound(
					null,
					caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.PLAYER_LEVELUP,
					SoundSource.PLAYERS,
					0.5F, 1.4F
			);
			return true;
		});
		register(Relic.SPELL_CLEANSE, (caster, spell) -> {
			final List<net.minecraft.world.effect.MobEffectInstance> harmfulEffects =
					caster.getActiveEffects().stream()
							.filter(effect ->
									effect.getEffect().getCategory() ==
											net.minecraft.world.effect.MobEffectCategory.HARMFUL
							)
							.toList();

			if (harmfulEffects.isEmpty())
				return false;

			for (final var effect : harmfulEffects) {
				caster.removeEffect(effect.getEffect());
			}

			caster.level().playSound(
					null,
					caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.AMETHYST_BLOCK_CHIME,
					SoundSource.PLAYERS,
					0.8F, 1.3F
			);
			return true;
		});
		register(Relic.SPELL_DASH, (caster, spell) -> {
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

			final Vec3 look = caster.getLookAngle();

			final Vec3 horizontal = new Vec3(
					look.x,
					0.0,
					look.z
			);

			if (horizontal.lengthSqr() <= 1.0e-8)
				return false;

			final Vec3 direction = horizontal.normalize();

			caster.setDeltaMovement(
					direction.x * strength,
					vertical,
					direction.z * strength
			);

			caster.hurtMarked = true;

			caster.level().playSound(
					null,
					caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.ENDER_DRAGON_FLAP,
					SoundSource.PLAYERS,
					0.45F, 1.7F
			);
			return true;
		});
		register(Relic.SPELL_ARC_BURST, (caster, spell) -> {
			final float intensity = (float) Math.max(
					0.0,
					RelicSpells.numberParameter(spell, "intensity", 0.0)
			);

			final int range = (int) Math.min(
					256,
					Math.max(
							0,
							Math.round(
									RelicSpells.numberParameter(spell, "range", 0.0)
							)
					)
			);

			final int count = (int) Math.min(
					64,
					Math.max(
							0,
							Math.round(
									RelicSpells.numberParameter(spell, "count", 1.0)
							)
					)
			);

			final int weaken = (int) Math.min(
					255,
					Math.max(
							0,
							Math.round(
									RelicSpells.numberParameter(spell, "weaken", 0.0)
							)
					)
			);

			if (intensity <= 0.0F || range <= 0 || count <= 0)
				return false;

			final float damage =
					(float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE) *
							intensity / 100.0F;

			if (damage <= 0.0F)
				return false;

			MyDamageInfo.directRangedAttack(caster, damage, range, count, weaken);

			caster.level().playSound(
					null,
					caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.EVOKER_CAST_SPELL,
					SoundSource.PLAYERS,
					0.8F, 0.9F
			);
			return true;
		});

		register(Relic.SPELL_REPULSE, (caster, spell) -> {
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

			for (final LivingEntity target :
					MyDamageInfo.getLivingEntitiesInRange(caster, range)) {

				if (!hostileTargeted(caster, target))
					continue;

				final Vec3 away = new Vec3(
						target.getX() - caster.getX(),
						0.0,
						target.getZ() - caster.getZ()
				);

				if (away.lengthSqr() <= 1.0e-8)
					continue;

				final Vec3 direction = away.normalize();

				target.push(
						direction.x * strength,
						vertical,
						direction.z * strength
				);

				target.hurtMarked = true;
				affected = true;
			}

			if (!affected)
				return false;

			caster.level().playSound(
					null,
					caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.GENERIC_EXPLODE,
					SoundSource.PLAYERS,
					0.5F, 1.4F
			);

			return true;
		});
		register(Relic.SPELL_ABSORPTION, (caster, spell) -> {
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

			caster.addEffect(new MobEffectInstance(
					MobEffects.ABSORPTION,
					durationTicks,
					amplifier
			));

			caster.level().playSound(
					null,
					caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.BEACON_ACTIVATE,
					SoundSource.PLAYERS,
					0.6F, 1.25F
			);

			return true;
		});
		register(Relic.SPELL_SKY_LAUNCH, (caster, spell) -> {
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

			for (final LivingEntity target :
					MyDamageInfo.getLivingEntitiesInRange(caster, range)) {

				if (!hostileTargeted(caster, target))
					continue;

				target.push(0.0, strength, 0.0);
				target.hurtMarked = true;
				affected = true;
			}

			if (!affected)
				return false;

			caster.level().playSound(
					null,
					caster.getX(), caster.getY(), caster.getZ(),
					SoundEvents.ENDER_DRAGON_FLAP,
					SoundSource.PLAYERS,
					0.65F, 0.75F
			);

			return true;
		});
	}
}