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


	public static class MyMap<TValue> {

		class Detail {
			int last_tick;
			TValue value;

			public Detail(int currentTick, TValue value) {
				this.last_tick = currentTick;
				this.value = value;
			}
		}

		private final Map<UUID, Detail> map = new HashMap<>();
		private final ReentrantLock lock = new ReentrantLock();
		private final int max_ticks = 160;

		public final ReentrantLock get_lock() {
			return lock;
		}

		/*public Map<UUID, TValue> getMap() {
			synchronized (map) {
				return map;
			}
		}*/
		public boolean containsKey(UUID key) {
			try {
				lock.lock();
				boolean b = map.containsKey(key);
				return b;
			} finally { lock.unlock(); }
		}

		public void put(UUID key, int currentTick, TValue value) {
			try {
				lock.lock();
				map.put(key, new Detail(currentTick, value));
			} finally { lock.unlock(); }
		}

		public Set<UUID> keySet() {
			lock.lock();
			try {
				return Set.copyOf(map.keySet());
			} finally {
				lock.unlock();
			}
		}

		public TValue get(UUID key) {
			try {
				lock.lock();
				if (!map.containsKey(key)) {
					return null;
				}
				TValue value1 = map.get(key).value;
				return value1;
			} finally { lock.unlock(); }
		}

		public TValue getOrDefault(UUID key, TValue default_value) {

			try {
				lock.lock();
				if (!map.containsKey(key)) {
					return default_value;
				}
				TValue value1 = map.get(key).value;
				return value1;
			} finally { lock.unlock(); }
		}

		public void set_last_tick(UUID key, int last_tick) {
			try {
				lock.lock();
				map.get(key).last_tick = last_tick;
			} finally { lock.unlock(); }
		}

		public void cleanUp(int current_tick_count, Function<UUID, Boolean> is_entity_valid) {
			try {
				// MinecraftServer server = event.getServer();
				lock.lock();
				if (!map.isEmpty()) {
					List<UUID> toRemove = new ArrayList<>();

					for (UUID uuid : map.keySet()) {
						boolean entityIsValid = false;

						if (current_tick_count - map.get(uuid).last_tick > max_ticks) {
							entityIsValid = false;
						} else {
							entityIsValid = is_entity_valid.apply(uuid);
						}

						if (!entityIsValid) {
							toRemove.add(uuid);
						}
					}

					for (UUID uuid : toRemove) {
						map.remove(uuid);
					}
				}
			} finally { lock.unlock(); }
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



	public final MyMap<MyList<DelayTask>> DELAYED_TASK_LIST = new MyMap<>();
	public final MyMap<Float> HEAL_PREVENTION_LIST = new MyMap<>();
	public final MyMap<Integer> PROTECTED_PLAYER_LIST = new MyMap<>();

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
		}
	}

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

	private static final Scheduler INSTANCE = new Scheduler();

	public static Scheduler INSTANCE() {
		return INSTANCE;
	}

}
