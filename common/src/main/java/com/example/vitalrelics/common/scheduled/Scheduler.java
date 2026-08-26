package com.example.vitalrelics.common.scheduled;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

	// TODO, make all of them private.
	private final MyMap<MyList<DelayTask>> DELAYED_TASK_LIST = new MyMap<>();
	private final MyMap<Float> HEAL_PREVENTION_LIST = new MyMap<>();
	private final MyMap<Integer> PROTECTED_PLAYER_LIST = new MyMap<>();

	private final MyMap<SpellState> SPELL_STATE_LIST = new MyMap<>(0);
	private final MyMap<Integer> THORNS_COOLDOWN_LIST = new MyMap<>();

	private static final int DELAYED_TASK_LIST_MAX = 12;

	public void serverTick(
			final int currentTickCount,
			final Function<UUID, Boolean> isEntityValid) {

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
			DELAYED_TASK_LIST.cleanUp(currentTickCount, isEntityValid);
			HEAL_PREVENTION_LIST.cleanUp(currentTickCount, isEntityValid);
			PROTECTED_PLAYER_LIST.cleanUp(currentTickCount, isEntityValid);
			THORNS_COOLDOWN_LIST.cleanUp(currentTickCount, isEntityValid);
		}
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

		return Math.max(
				0,
				state.cooldownEndTicks.getOrDefault(spellId, 0) -
						currentTickCount
		);
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

		if (PROTECTED_PLAYER_LIST.getOrDefault(uuid, 0) != 0)
			return false;

		PROTECTED_PLAYER_LIST.put(uuid, currentTick, invulnerableTicks);
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
	Static Methods
	 */

	private static final Scheduler INSTANCE = new Scheduler();

	public static Scheduler INSTANCE() {
		return INSTANCE;
	}

}
