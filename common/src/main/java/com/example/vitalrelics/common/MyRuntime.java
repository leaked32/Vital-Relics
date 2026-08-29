package com.example.vitalrelics.common;

import com.example.vitalrelics.common.platform.MyRuntimeUtils;

public final class MyRuntime {
	private static MyRuntimeUtils instance;

	public static synchronized void initialize(final MyRuntimeUtils runtime) {
		if (runtime == null)
			throw new IllegalArgumentException("runtime cannot be null");

		if (instance != null && instance != runtime)
			throw new IllegalStateException("MyRuntime is already initialized");

		instance = runtime;
	}

	public static MyRuntimeUtils getRuntimeUtils() {
		if (instance == null)
			throw new IllegalStateException("MyRuntime has not been initialized");

		return instance;
	}

	private MyRuntime() {}
}
