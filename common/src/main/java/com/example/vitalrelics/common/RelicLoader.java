package com.example.vitalrelics.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.vitalrelics.common.Util.load_external_file;

public class RelicLoader {
	public final List<Relic> relics_ = new ArrayList<>();

	public static void add_list(final List<String> target, final Object rawValue) {

		if (rawValue instanceof List<?> values) {
			for (final Object value : values) {
				if (!(value instanceof String entry))
					throw new IllegalArgumentException(
							"RelicLoader::add_list List entries must be strings"
					);

				target.add(entry);
			}
		}
	}

	public static void add_map(Map<String, Integer> target, final Object rawValue) {
		if (rawValue instanceof Map<?, ?> effects) {
			for (final var entry : effects.entrySet()) {
				if (!(entry.getKey() instanceof String effect)) {
					throw new IllegalArgumentException("RelicLoader::add_map keys must be strings");
				}
				if (!(entry.getValue() instanceof Number amplifier)) {
					throw new IllegalArgumentException("RelicLoader::add_map values must be numbers");
				}
				target.put(effect, amplifier.intValue());
			}
		}
	}

	public void load(final Path external_path) {
		if (external_path == null) {
			throw new RuntimeException("RelicLoader#load `external_path` cannot be null");
		}
		// If the target file doesn't exist, copy one
		final String internal_path = "vitalrelics/relics.json";

		final Map<String, Object> root = load_external_file(internal_path, external_path);

		final Object rawRelics = root.get("relics");

		if (!(rawRelics instanceof List<?> entries))
			throw new IllegalArgumentException("'relics' must be a JSON array");

		relics_.clear();

		for (final Object entry : entries) {
			if (!(entry instanceof Map<?, ?> rawRelic))
				throw new IllegalArgumentException("Each relic must be a JSON object");

			final Relic relic = new Relic();

			final Object id = rawRelic.get("id");
			if (!(id instanceof String))
				throw new IllegalArgumentException("Relic 'id' must be a string");

			relic.id = (String) id;

			if (rawRelic.get("curio_slot") instanceof String value)
				relic.curio_slot = value;

			if (rawRelic.get("display_name") instanceof String value)
				relic.display_name = value;

			if (rawRelic.get("tooltip") instanceof String value)
				relic.tooltip = value;

			if (rawRelic.get("texture") instanceof String value)
				relic.texture = value;

			if (rawRelic.get("rarity") instanceof String value)
				relic.rarity = value;

			final Object immunity = rawRelic.get("immune_to_effects");

			if (immunity instanceof String value) {
				relic.immune_to_effects.add(value);
			} else if (immunity instanceof List<?> values) {
				for (final Object value : values) {
					if (!(value instanceof String effect))
						throw new IllegalArgumentException(
								"immune_to_effects entries must be strings"
						);

					relic.immune_to_effects.add(effect);
				}
			} else if (immunity != null) {
				throw new IllegalArgumentException(
						"immune_to_effects must be a string or array"
				);
			}

			add_list(relic.effective_slots, rawRelic.get("effective_slots"));
			add_map(relic.passive_abilities, rawRelic.get("passive_abilities"));
			// add_map(relic.available_spells, rawRelic.get("available_spells"));
			add_map(relic.granted_effects, rawRelic.get("granted_effects"));

			if (rawRelic.get("properties") instanceof Map<?, ?> properties) {
				relic.properties.attack_damage = property(properties.get("attack_damage"));
				relic.properties.attack_speed = property(properties.get("attack_speed"));
				relic.properties.block_interaction_range = property(properties.get("block_interaction_range"));
				relic.properties.entity_interaction_range = property(properties.get("entity_interaction_range"));
				relic.properties.knockback_resistance = property(properties.get("knockback_resistance"));
				relic.properties.max_health = property(properties.get("max_health"));
			}

			if (rawRelic.get("ticks") instanceof Map<?, ?> ticks) {
				relic.ticks.heal = tick(ticks.get("heal"));
				relic.ticks.feed = tick(ticks.get("feed"));
			}

			if (rawRelic.get("callbacks") instanceof Map<?, ?> callbacks) {
				relic.callbacks.damage_taken = callback(callbacks.get("damage_taken"));
				relic.callbacks.damage_dealt = callback(callbacks.get("damage_dealt"));
				relic.callbacks.invulnerable_time_taken = callback(callbacks.get("invulnerable_time_taken"));
				relic.callbacks.invulnerable_time_dealt = callback(callbacks.get("invulnerable_time_dealt"));
			}

			if (rawRelic.get("available_spells") instanceof Map<?, ?> available_spells) {
				relic.available_spells.teleport = spell(available_spells.get("teleport"));
			}

			relics_.add(relic);
		}
	}

	private static Relic.Ticks.Info tick(final Object value) {
		if (value == null)
			return null;

		if (!(value instanceof Map<?, ?> map))
			throw new IllegalArgumentException("Property must be a JSON object");

		final Relic.Ticks.Info info = new Relic.Ticks.Info();

		if (map.containsKey("add"))
			info.add = number(map.get("add"));

		if (map.containsKey("ratio_add"))
			info.ratio_add = number(map.get("ratio_add"));

		if (map.containsKey("interval_ticks")) {
			info.interval_ticks = number(map.get("interval_ticks")).intValue();
			if (info.interval_ticks <= 0)
				throw new IllegalArgumentException("interval_ticks must be > 0");
		} else {
			throw new IllegalArgumentException("interval_ticks must set");
		}


		return info;
	}

	private static Relic.Properties.Info property(final Object value) {
		if (value == null)
			return null;

		if (!(value instanceof Map<?, ?> map))
			throw new IllegalArgumentException("Property must be a JSON object");

		final Relic.Properties.Info info = new Relic.Properties.Info();

		if (map.containsKey("add"))
			info.add = number(map.get("add"));

		if (map.containsKey("mul_base"))
			info.mul_base = number(map.get("mul_base"));

		if (map.containsKey("mul_total"))
			info.mul_total = number(map.get("mul_total"));

		return info;
	}

	private static Relic.Callbacks.Info callback(final Object value) {
		if (value == null)
			return null;

		if (!(value instanceof Map<?, ?> map)) {
			throw new IllegalArgumentException("Property must be a JSON object");
		}

		final Relic.Callbacks.Info info = new Relic.Callbacks.Info();

		if (map.containsKey("modifier"))
			info.modifier = number(map.get("modifier"));

		if (map.containsKey("flat"))
			info.flat = number(map.get("flat"));

		if (map.containsKey("minimum"))
			info.minimum = number(map.get("minimum"));

		if (map.containsKey("ratio_minimum"))
			info.ratio_minimum = number(map.get("ratio_minimum"));

		if (map.containsKey("maximum"))
			info.maximum = number(map.get("maximum"));

		if (map.containsKey("ratio_maximum"))
			info.ratio_maximum = number(map.get("ratio_maximum"));

		return info;
	}
	private static Relic.Spells.Info spell(final Object value) {
		if (value == null)
			return null;

		if (!(value instanceof Map<?, ?> map)) {
			throw new IllegalArgumentException("Property must be a JSON object");
		}

		final Relic.Spells.Info info = new Relic.Spells.Info();

		if (map.containsKey("intensity"))
			info.intensity = number(map.get("intensity"));

		if (map.containsKey("recovery"))
			info.recovery = number(map.get("recovery"));

		return info;
	}

	private static Double number(final Object value) {
		if (!(value instanceof Number number))
			throw new IllegalArgumentException("Expected number, got: " + value);

		return number.doubleValue();
	}

	public Relic find(final String id) {
		for (final Relic relic : relics_) {
			if (relic.id.equals(id))
				return relic;
		}

		return null;
	}

	public static Relic.Properties computeProperties(final List<Relic> relics) {
		final Relic.Properties result = new Relic.Properties();

		result.attack_damage = Relic.Properties.Info.basic();
		result.attack_speed = Relic.Properties.Info.basic();
		result.knockback_resistance = Relic.Properties.Info.basic();
		result.max_health = Relic.Properties.Info.basic();
		result.block_interaction_range = Relic.Properties.Info.basic();
		result.entity_interaction_range = Relic.Properties.Info.basic();

		for (final Relic relic : relics) {
			if (relic.properties == null)
				continue;

			accumulate(result.attack_damage, relic.properties.attack_damage);
			accumulate(result.attack_speed, relic.properties.attack_speed);
			accumulate(result.knockback_resistance, relic.properties.knockback_resistance);
			accumulate(result.max_health, relic.properties.max_health);
			accumulate(result.block_interaction_range, relic.properties.block_interaction_range);
			accumulate(result.entity_interaction_range, relic.properties.entity_interaction_range);
		}

		return result;
	}

	private static void accumulate(
			final Relic.Properties.Info result,
			final Relic.Properties.Info value) {
		if (value == null)
			return;

		if (value.add != null)
			result.add += value.add;

		if (value.mul_base != null)
			result.mul_base += value.mul_base;

		if (value.mul_total != null)
			result.mul_total *= value.mul_total;
	}

	public static Relic.Ticks computeTicks(final List<Relic> relics, final int current_tick) {
		final Relic.Ticks result = new Relic.Ticks();

		result.heal = Relic.Ticks.Info.basic();
		result.feed = Relic.Ticks.Info.basic();

		for (final Relic relic : relics) {
			if (relic.ticks == null)
				continue;

			accumulateTick(result.heal, relic.ticks.heal, current_tick);
			accumulateTick(result.feed, relic.ticks.feed, current_tick);
		}

		return result;
	}

	private static void accumulateTick(
			final Relic.Ticks.Info result,
			final Relic.Ticks.Info value,
			final int current_tick) {
		if (value == null) {
			return;
		}
		if (current_tick % value.interval_ticks != 0) {
			return;
		}

		if (value.add != null)
			result.add += value.add;

		if (value.ratio_add != null)
			result.ratio_add += value.ratio_add;
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

	public static int highestLevelInMap(final Map<String, Integer> map, final String key) {
		int highest_level = 0;

		for (final var entry : map.entrySet()) {
			if (entry.getKey().equals(key)) {
				highest_level = Math.max(highest_level, entry.getValue());
			}
		}

		return highest_level;
	}

	public static int levelOfSuchPassiveAbility(final List<Relic> relics, final String requiredAbility) {
		int highest_level = 0;

		for (final Relic relic : relics) {
			highest_level = highestLevelInMap(relic.passive_abilities, requiredAbility);
		}
		return highest_level;
	}


}