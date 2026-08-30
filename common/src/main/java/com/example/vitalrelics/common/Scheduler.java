package com.example.vitalrelics.common;

import com.example.vitalrelics.common.platform.MyRuntimeUtils;
import com.example.vitalrelics.common.utils.MyMap;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class Scheduler {

	public static class DelayTask {
		final int task_delay_ticks;
		final int alternative_task_delay_ticks;
		int currentTickCount;
		final Runnable task;

		public DelayTask(int task_delay_ticks, int alternative_task_delay_ticks, Runnable task) {
			this.task_delay_ticks = task_delay_ticks;
			this.alternative_task_delay_ticks = alternative_task_delay_ticks;
			this.currentTickCount = 0;
			this.task = task;
		}
	}



	public static class MyList<TValue> {
		private final List<TValue> list = new ArrayList<>();
		private final ReentrantLock lock = new ReentrantLock();

		public int size() {
			try {
				lock.lock();
				return list.size();
			} finally {
				lock.unlock();
			}
		}

		public void add(TValue e) {
			try {
				lock.lock();
				list.add(e);
			} finally {
				lock.unlock();
			}
		}

		public List<TValue> snapshot() {
			lock.lock();
			try {
				return new ArrayList<>(list);
			} finally {
				lock.unlock();
			}
		}

		public boolean remove(final TValue value) {
			lock.lock();
			try {
				return list.remove(value);
			} finally {
				lock.unlock();
			}
		}
	}


	public static final class SpellState {
		private String selectedSpellId = null;
		private final Map<String, Integer> cooldownEndTicks =
				new HashMap<>();
	}

	public static final class FlightState {
		public final boolean grantedByVitalRelics;
		public final double level;

		public FlightState(
				final boolean grantedByVitalRelics,
				final double level) {
			this.grantedByVitalRelics = grantedByVitalRelics;
			this.level = level;
		}
	}

	public enum FlightAction {NONE, GRANT, UPDATE, REMOVE}

	public record FlightUpdate(FlightAction action, double level) {}

	private final MyMap<MyList<DelayTask>> DELAYED_TASK_LIST = new MyMap<>();
	private final MyMap<Float> HEAL_PREVENTION_LIST = new MyMap<>(320);
	private final MyMap<Integer> PROTECTED_PLAYER_LIST = new MyMap<>();

	private final MyMap<SpellState> SPELL_STATE_LIST = new MyMap<>(0);
	private final MyMap<FlightState> FLIGHT_STATE_LIST = new MyMap<>(0);
	private final MyMap<Integer> THORNS_COOLDOWN_LIST = new MyMap<>();
	private final MyMap<Integer> ARROW_DEFLECTION_COOLDOWN_LIST = new MyMap<>();

	private static final int DELAYED_TASK_LIST_MAX = 12;

	/*
	Scheduler Management
	 */

	// Call it for all ticks
	public void serverTick(final int currentTickCount) {
		final MyRuntimeUtils runtime = MyRuntime.getRuntimeUtils();

		for (final UUID uuid : DELAYED_TASK_LIST.keySet()) {
			final MyList<DelayTask> tasks = DELAYED_TASK_LIST.get(uuid);

			if (tasks == null)
				continue;

			final List<DelayTask> snapshot = tasks.snapshot();
			final boolean useAlternativeDelay =
					snapshot.size() >= DELAYED_TASK_LIST_MAX / 2;

			for (final DelayTask task : snapshot) {
				task.currentTickCount++;

				final int requiredTicks = useAlternativeDelay
						? task.alternative_task_delay_ticks
						: task.task_delay_ticks;

				if (task.currentTickCount < requiredTicks)
					continue;

				try {
					task.task.run();
				} catch (Exception exception) {
					throw new RuntimeException(exception);
				} finally {
					tasks.remove(task);
				}
			}
		}

		if (currentTickCount % 40 == 0) {
			DELAYED_TASK_LIST.cleanUp(currentTickCount, runtime::isEntityValid);
			HEAL_PREVENTION_LIST.cleanUp(currentTickCount, runtime::isEntityValid);
			PROTECTED_PLAYER_LIST.cleanUp(currentTickCount, runtime::isEntityValid);
			THORNS_COOLDOWN_LIST.cleanUp(currentTickCount, runtime::isEntityValid);
			ARROW_DEFLECTION_COOLDOWN_LIST.cleanUp(currentTickCount, runtime::isEntityValid);
			FLIGHT_STATE_LIST.cleanUp(currentTickCount, runtime::isEntityValid);
		}
	}

	// Entity Lifecycle
	// Call it once the player exits.
	public void clearEntity(final UUID uuid) {
		DELAYED_TASK_LIST.remove(uuid);
		HEAL_PREVENTION_LIST.remove(uuid);
		PROTECTED_PLAYER_LIST.remove(uuid);
		SPELL_STATE_LIST.remove(uuid);
		THORNS_COOLDOWN_LIST.remove(uuid);
		ARROW_DEFLECTION_COOLDOWN_LIST.remove(uuid);
		FLIGHT_STATE_LIST.remove(uuid);
	}

	// Call it once the server ends.
	public void clear() {
		DELAYED_TASK_LIST.clear();
		HEAL_PREVENTION_LIST.clear();
		PROTECTED_PLAYER_LIST.clear();
		SPELL_STATE_LIST.clear();
		THORNS_COOLDOWN_LIST.clear();
		ARROW_DEFLECTION_COOLDOWN_LIST.clear();
		FLIGHT_STATE_LIST.clear();
	}

	/*
	Spells
	 */

	private SpellState spellState(
			final UUID uuid,
			final int currentTickCount) {

		SpellState state = SPELL_STATE_LIST.get(uuid);

		if (state == null) {
			state = new SpellState();
			SPELL_STATE_LIST.put(uuid, currentTickCount, state);
		}

		return state;
	}

	public String selectSpell(
			final UUID uuid,
			final List<String> spellIds,
			final int direction,
			final int currentTickCount) {

		final SpellState state = spellState(uuid, currentTickCount);

		if (spellIds.isEmpty()) {
			state.selectedSpellId = null;
			return null;
		}

		final int current = Math.max(
				0,
				spellIds.indexOf(state.selectedSpellId)
		);

		state.selectedSpellId = spellIds.get(Math.floorMod(
				current + direction,
				spellIds.size()
		));

		SPELL_STATE_LIST.put(uuid, currentTickCount, state);
		return state.selectedSpellId;
	}

	public String selectedSpell(
			final UUID uuid,
			final List<String> spellIds,
			final int currentTickCount) {

		final SpellState state = spellState(uuid, currentTickCount);

		if (spellIds.isEmpty()) {
			state.selectedSpellId = null;
			return null;
		}

		if (!spellIds.contains(state.selectedSpellId))
			state.selectedSpellId = spellIds.get(0);

		SPELL_STATE_LIST.put(uuid, currentTickCount, state);
		return state.selectedSpellId;
	}

	public int getSpellCooldownRemaining(
			final UUID uuid,
			final String spellId,
			final int currentTickCount) {

		final SpellState state = SPELL_STATE_LIST.get(uuid);

		if (state == null)
			return 0;

		return Math.max(0, state.cooldownEndTicks.getOrDefault(spellId, 0) - currentTickCount);
	}

	public void setSpellCooldown(
			final UUID uuid,
			final String spellId,
			final int currentTickCount,
			final int durationTicks) {

		final SpellState state = spellState(uuid, currentTickCount);

		state.cooldownEndTicks.put(
				spellId,
				currentTickCount + durationTicks
		);

		SPELL_STATE_LIST.put(uuid, currentTickCount, state);
	}

	/*
	Delayed Task
	 */

	public void addDelayedTask(UUID uuid, DelayTask task, int current_tick_count) {
		final int DELAYED_TASK_LIST_MAX = 12;

		try {
			DELAYED_TASK_LIST.get_lock().lock();
			if (!DELAYED_TASK_LIST.containsKey(uuid)) {
				DELAYED_TASK_LIST.put(uuid, current_tick_count, new MyList<>());
			}

			// .get(uuid) should not return null as it's locked and containsKey check passed
			if (DELAYED_TASK_LIST.get(uuid).size() >= DELAYED_TASK_LIST_MAX) {
				return;
			}

			DELAYED_TASK_LIST.get(uuid).add(task);
			DELAYED_TASK_LIST.set_last_tick(uuid, current_tick_count);
		} finally {
			DELAYED_TASK_LIST.get_lock().unlock();
		}
	}

	/*
	Healing Prevention
	 */

	public float addHealingPrevention(
			final UUID uuid,
			final int currentTick,
			final float amount) {

		final float total =
				HEAL_PREVENTION_LIST.getOrDefault(uuid, 0.0F) + amount;

		HEAL_PREVENTION_LIST.put(uuid, currentTick, total);
		return total;
	}

	/*
	Protection
	 */

	public boolean acquireProtection(
			final UUID uuid,
			final int currentTick,
			final int invulnerableTicks) {

		final int protectionEnd =
				PROTECTED_PLAYER_LIST.getOrDefault(uuid, 0);

		if (currentTick < protectionEnd)
			return false;

		PROTECTED_PLAYER_LIST.put(
				uuid,
				currentTick,
				currentTick + Math.max(0, invulnerableTicks)
		);
		return true;
	}

	/*
	Thorns Cooldown
	 */

	public boolean acquireThorns(
			final UUID uuid,
			final int currentTick,
			final int cooldownTicks) {

		final int cooldownEnd =
				THORNS_COOLDOWN_LIST.getOrDefault(uuid, 0);

		if (currentTick < cooldownEnd)
			return false;

		THORNS_COOLDOWN_LIST.put(
				uuid,
				currentTick,
				currentTick + cooldownTicks
		);

		return true;
	}

	/*
	Arrow Deflection Cooldown
	 */

	public boolean acquireArrowDeflection(
			final UUID uuid,
			final int currentTick,
			final int cooldownTicks) {

		final int cooldownEnd =
				ARROW_DEFLECTION_COOLDOWN_LIST.getOrDefault(uuid, 0);

		if (currentTick < cooldownEnd)
			return false;

		ARROW_DEFLECTION_COOLDOWN_LIST.put(
				uuid,
				currentTick,
				currentTick + Math.max(1, cooldownTicks)
		);

		return true;
	}

	/*
	Static Methods
	 */

	private static final Scheduler INSTANCE = new Scheduler();

	public static Scheduler INSTANCE() {
		return INSTANCE;
	}

	/*
	Flight
	 */
	public FlightUpdate updateFlight(
			final UUID uuid,
			final int currentTick,
			final double flightLevel,
			final boolean mayFly) {

		final FlightState previous = FLIGHT_STATE_LIST.get(uuid);
		final boolean hasFlight = flightLevel > 0.0;

		if (hasFlight) {
			final boolean grantedByVitalRelics =
					previous != null ? previous.grantedByVitalRelics : !mayFly;

			FLIGHT_STATE_LIST.put(
					uuid,
					currentTick,
					new FlightState(grantedByVitalRelics, flightLevel)
			);

			if (!mayFly)
				return new FlightUpdate(FlightAction.GRANT, flightLevel);

			if (previous != null &&
					previous.grantedByVitalRelics &&
					Double.compare(previous.level, flightLevel) != 0)
				return new FlightUpdate(FlightAction.UPDATE, flightLevel);

			return new FlightUpdate(FlightAction.NONE, flightLevel);
		}

		if (previous == null)
			return new FlightUpdate(FlightAction.NONE, 0.0);

		FLIGHT_STATE_LIST.remove(uuid);

		if (previous.grantedByVitalRelics)
			return new FlightUpdate(FlightAction.REMOVE, previous.level);

		return new FlightUpdate(FlightAction.NONE, 0.0);
	}
}
