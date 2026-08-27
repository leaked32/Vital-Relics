package com.example.vitalrelics.common.utils;


import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class MyMap<TValue> {

	class Detail {
		int last_tick;
		TValue value;

		public Detail(int currentTick, TValue value) {
			this.last_tick = currentTick;
			this.value = value;
		}
	}

	public MyMap(final int max_ticks) {
		this.max_ticks = max_ticks;
	}

	public MyMap() {
		this.max_ticks = 160;
	}

	private final Map<UUID, Detail> map = new HashMap<>();
	private final ReentrantLock lock = new ReentrantLock();
	private final int max_ticks;

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

					if (max_ticks > 0) {
						if (current_tick_count - map.get(uuid).last_tick > max_ticks) {
							entityIsValid = false;
						} else {
							entityIsValid = is_entity_valid.apply(uuid);
						}
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
