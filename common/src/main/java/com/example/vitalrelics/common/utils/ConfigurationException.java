package com.example.vitalrelics.common.utils;

import java.nio.file.Path;

public final class ConfigurationException extends RuntimeException {
	public ConfigurationException(
			final String message,
			final Throwable cause) {

		super(message, cause);
	}


	public static ConfigurationException configuration_error(
			final Path path,
			final Throwable cause) {

		if (cause instanceof ConfigurationException configuration)
			return configuration;

		final String reason = cause.getMessage() != null
				? cause.getMessage()
				: cause.getClass().getSimpleName();

		return new ConfigurationException(
				"Failed to load Vital Relics configuration '" +
						path.toAbsolutePath() + "': " + reason,
				cause
		);
	}
}
