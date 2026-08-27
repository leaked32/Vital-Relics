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
			final Throwable exception) {

		if (exception instanceof ConfigurationException configuration_exception)
			return configuration_exception;

		final String reason = exception.getMessage() != null
				? exception.getMessage()
				: exception.getClass().getSimpleName();

		return new ConfigurationException(
				"Failed to load Vital Relics configuration file:\n" +
						"  " + path.toAbsolutePath() + "\n" +
						"Reason: " + reason + "\n" +
						"If you do not know how to fix this configuration, " +
						"remove the file and restart the game. " +
						"`Vital Relics` will automatically regenerate it.",
				exception
		);
	}
}
