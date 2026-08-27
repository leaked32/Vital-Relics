package com.example.vitalrelics.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.example.vitalrelics.common.Util.load_external_file;

public class RelicLoader {
	public final List<Relic> relics_ = new ArrayList<>();

	public static void add_list(final List<String> target, final Object rawValue) {
		if (!(rawValue instanceof List<?> values))
			return;

		for (final Object value : values) {
			if (!(value instanceof String entry)) {
				throw new IllegalArgumentException(
						"Relic list entries must be strings"
				);
			}

			target.add(entry);
		}
	}

	public static void add_map(
			final Map<String, Integer> target,
			final Object rawValue) {

		if (!(rawValue instanceof Map<?, ?> values))
			return;

		for (final var entry : values.entrySet()) {
			if (!(entry.getKey() instanceof String key) ||
					!(entry.getValue() instanceof Number value)) {
				throw new IllegalArgumentException(
						"Ability and effect entries must map strings to numbers"
				);
			}

			target.put(key, value.intValue());
		}
	}

	public void load(final Path externalPath) {
		if (externalPath == null) {
			throw new IllegalArgumentException(
					"RelicLoader#load externalPath cannot be null"
			);
		}

		final Map<String, Object> root = load_external_file(
				Manifest.INTERNAL_PATH_TO_RELICS,
				externalPath,
				Manifest.OPT_RELICS_VER
		);

		if (!(root.get("relics") instanceof List<?> entries))
			throw new IllegalArgumentException("'relics' must be a JSON array");

		relics_.clear();

		for (final Object entry : entries) {
			if (!(entry instanceof Map<?, ?> rawRelic))
				throw new IllegalArgumentException(
						"Each relic must be a JSON object"
				);

			relics_.add(loadRelic(rawRelic));
		}
	}

	private static Relic loadRelic(final Map<?, ?> rawRelic) {
		final Relic relic = new Relic();

		if (!(rawRelic.get("id") instanceof String id)) {
			throw new IllegalArgumentException("Relic id must be a string");
		}

		relic.id = id;
		setString(rawRelic, "curio_slot", value -> relic.curio_slot = value);
		setString(rawRelic, "display_name", value -> relic.display_name = value);
		setString(rawRelic, "tooltip", value -> relic.tooltip = value);
		setString(rawRelic, "texture", value -> relic.texture = value);
		setString(rawRelic, "rarity", value -> relic.rarity = value);

		final Object immunity = rawRelic.get("immune_to_effects");

		if (immunity instanceof String effect) {
			relic.immune_to_effects.add(effect);
		} else if (immunity instanceof List<?> effects) {
			add_list(relic.immune_to_effects, effects);
		} else if (immunity != null) {
			throw new IllegalArgumentException(
					"immune_to_effects must be a string or array"
			);
		}

		add_list(relic.effective_slots, rawRelic.get("effective_slots"));
		add_map(relic.passive_skills, rawRelic.get("passive_skills"));
		add_map(relic.granted_effects, rawRelic.get("granted_effects"));

		addStructuredMap(
				relic.properties,
				rawRelic.get("properties"),
				RelicLoader::property,
				"properties"
		);

		addStructuredMap(
				relic.ticks,
				rawRelic.get("ticks"),
				RelicLoader::tick,
				"ticks"
		);

		addStructuredMap(
				relic.callbacks,
				rawRelic.get("callbacks"),
				RelicLoader::callback,
				"callbacks"
		);

		addStructuredMap(
				relic.available_spells,
				rawRelic.get("available_spells"),
				RelicLoader::spell,
				"available_spells"
		);

		return relic;
	}

	private static void setString(
			final Map<?, ?> source,
			final String key,
			final java.util.function.Consumer<String> consumer) {

		final Object value = source.get(key);

		if (value instanceof String string)
			consumer.accept(string);
		else if (value != null)
			throw new IllegalArgumentException(key + " must be a string");
	}

	private static <T> void addStructuredMap(
			final Map<String, T> target,
			final Object rawValue,
			final Function<Object, T> parser,
			final String fieldName) {

		if (rawValue == null)
			return;

		if (!(rawValue instanceof Map<?, ?> values)) {
			throw new IllegalArgumentException(
					fieldName + " must be a JSON object"
			);
		}

		for (final var entry : values.entrySet()) {
			if (!(entry.getKey() instanceof String id)) {
				throw new IllegalArgumentException(
						fieldName + " keys must be strings"
				);
			}

			target.put(id, parser.apply(entry.getValue()));
		}
	}

	private static Relic.Ticks.Info tick(final Object value) {
		final Map<?, ?> map = object(value, "Tick");

		if (!map.containsKey("interval_ticks")) {
			throw new IllegalArgumentException(
					"Tick interval_ticks must be set"
			);
		}

		final Relic.Ticks.Info result = new Relic.Ticks.Info();
		result.interval_ticks = number(map.get("interval_ticks")).intValue();

		if (result.interval_ticks <= 0)
			throw new IllegalArgumentException(
					"Tick interval_ticks must be > 0"
			);

		if (map.containsKey("add"))
			result.add = number(map.get("add"));
		if (map.containsKey("ratio_add"))
			result.ratio_add = number(map.get("ratio_add"));

		return result;
	}

	private static Relic.Properties.Info property(final Object value) {
		final Map<?, ?> map = object(value, "Property");
		final Relic.Properties.Info result = new Relic.Properties.Info();

		if (map.containsKey("add"))
			result.add = number(map.get("add"));
		if (map.containsKey("mul_base"))
			result.mul_base = number(map.get("mul_base"));
		if (map.containsKey("mul_total"))
			result.mul_total = number(map.get("mul_total"));

		return result;
	}

	private static Relic.Callbacks.Info callback(final Object value) {
		final Map<?, ?> map = object(value, "Callback");
		final Relic.Callbacks.Info result = new Relic.Callbacks.Info();

		if (map.containsKey("modifier"))
			result.modifier = number(map.get("modifier"));
		if (map.containsKey("flat"))
			result.flat = number(map.get("flat"));
		if (map.containsKey("minimum"))
			result.minimum = number(map.get("minimum"));
		if (map.containsKey("ratio_minimum"))
			result.ratio_minimum = number(map.get("ratio_minimum"));
		if (map.containsKey("maximum"))
			result.maximum = number(map.get("maximum"));
		if (map.containsKey("ratio_maximum"))
			result.ratio_maximum = number(map.get("ratio_maximum"));

		return result;
	}

	private static Relic.Spells.Info spell(final Object value) {
		final Map<?, ?> map = object(value, "Spell");
		final Relic.Spells.Info result = new Relic.Spells.Info();

		for (final var entry : map.entrySet()) {
			if (!(entry.getKey() instanceof String key)) {
				throw new IllegalArgumentException(
						"Spell parameter names must be strings"
				);
			}

			result.parameters.put(key, entry.getValue());
		}

		return result;
	}

	private static Map<?, ?> object(
			final Object value,
			final String typeName) {

		if (!(value instanceof Map<?, ?> map))
			throw new IllegalArgumentException(
					typeName + " must be a JSON object"
			);

		return map;
	}

	private static Double number(final Object value) {
		if (!(value instanceof Number number))
			throw new IllegalArgumentException(
					"Expected number, got: " + value
			);

		return number.doubleValue();
	}

	public Relic find(final String id) {
		for (final Relic relic : relics_) {
			if (relic.id.equals(id))
				return relic;
		}

		return null;
	}

	public static Map<String, Relic.Properties.Info> computeProperties(
			final List<Relic> relics) {

		final Map<String, Relic.Properties.Info> result =
				new LinkedHashMap<>();

		for (final Relic relic : relics) {
			for (final var entry : relic.properties.entrySet()) {
				accumulate(
						result.computeIfAbsent(
								entry.getKey(),
								unused -> Relic.Properties.Info.basic()
						),
						entry.getValue()
				);
			}
		}

		return result;
	}

	private static void accumulate(
			final Relic.Properties.Info result,
			final Relic.Properties.Info value) {

		if (value.add != null)
			result.add += value.add;
		if (value.mul_base != null)
			result.mul_base += value.mul_base;
		if (value.mul_total != null)
			result.mul_total *= value.mul_total;
	}

	public static Map<String, Relic.Ticks.Info> computeTicks(
			final List<Relic> relics,
			final int currentTick) {

		final Map<String, Relic.Ticks.Info> result =
				new LinkedHashMap<>();

		for (final Relic relic : relics) {
			for (final var entry : relic.ticks.entrySet()) {
				final Relic.Ticks.Info value = entry.getValue();

				if (currentTick % value.interval_ticks != 0)
					continue;

				final Relic.Ticks.Info total = result.computeIfAbsent(
						entry.getKey(),
						unused -> Relic.Ticks.Info.basic()
				);

				if (value.add != null)
					total.add += value.add;
				if (value.ratio_add != null)
					total.ratio_add += value.ratio_add;
			}
		}

		return result;
	}

	public static double applyCallback(
			final List<Relic> relics,
			final String callbackId,
			final double value,
			final double reference) {

		double result = value;

		for (final Relic relic : relics) {
			final Relic.Callbacks.Info callback =
					relic.callbacks.get(callbackId);

			if (callback != null)
				result = callback.process(result, reference);
		}

		return result;
	}

	public static boolean isImmuneToEffect(
			final List<Relic> relics,
			final String effectId,
			final boolean negative) {

		for (final Relic relic : relics) {
			if (relic.isImmuneToEffect(effectId, negative))
				return true;
		}

		return false;
	}

	public static int highestLevelInMap(
			final Map<String, Integer> values,
			final String id) {

		return values.getOrDefault(id, 0);
	}

	public static int levelOfSuchPassiveSkill(
			final List<Relic> relics,
			final String requiredPassiveSkill) {

		int highestLevel = 0;

		for (final Relic relic : relics) {
			highestLevel = Math.max(
					highestLevel,
					highestLevelInMap(
							relic.passive_skills,
							requiredPassiveSkill
					)
			);
		}

		return highestLevel;
	}
}
