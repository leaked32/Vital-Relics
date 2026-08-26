package com.example.vitalrelics.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RelicText {
	public enum Source {
		LITERAL,
		EXTERNAL,
		VANILLA
	}

	public record Text(
			Source source,
			String translationKey,
			String fallback,
			List<Text> arguments) {

		public Text {
			arguments = List.copyOf(arguments);
		}
		public boolean isBlank() {
			return fallback == null || fallback.isBlank();
		}
	}

	private RelicText() {}

	public static Text literal(final String value) {
		return new Text(Source.LITERAL, null, value, List.of());
	}

	public static Text key(
			final String translationKey,
			final String fallback,
			final Text... arguments) {

		return new Text(
				Source.EXTERNAL, translationKey, fallback, List.of(arguments)
		);
	}

	private static Text vanillaKey(
			final String translationKey,
			final String fallback) {

		return new Text(Source.VANILLA, translationKey, fallback, List.of());
	}

	public static Text itemName(final Relic relic) {
		final String fallback =
				relic.display_name == null || relic.display_name.isBlank()
						? displayName(relic.id)
						: relic.display_name;

		return key(
				"item.vitalrelics." + relic.id,
				fallback
		);
	}

	public static List<Text> tooltipLines(final Relic relic) {
		final List<Text> out = new ArrayList<>();

		if (relic.tooltip != null && !relic.tooltip.isBlank()) {
			out.add(key(
					"tooltip.vitalrelics." + relic.id,
					relic.tooltip
			));
			out.add(literal(""));
		}

		for (final var entry : relic.properties.entrySet())
			addProperty(out, entry.getKey(), entry.getValue());

		for (final var entry : relic.ticks.entrySet())
			addTick(out, entry.getKey(), entry.getValue());

		for (final var entry : relic.callbacks.entrySet())
			addCallback(out, entry.getKey(), entry.getValue());

		if (relic.immune_to_effects.contains("all_negative")) {
			out.add(key(
					"tooltip.vitalrelics.immune_all_negative",
					"Immune to all negative effects"
			));
		} else {
			for (final String effect : relic.immune_to_effects) {
				final Text effectName = vanillaEffectName(effect);

				out.add(key(
						"tooltip.vitalrelics.immune_effect",
						"Immune to " + effectName.fallback(),
						effectName
				));
			}
		}

		for (final var effect : relic.granted_effects.entrySet()) {
			final Text effectName = vanillaEffectName(effect.getKey());
			final Text level = number(effect.getValue());

			out.add(key(
					"tooltip.vitalrelics.grants_effect",
					"Grants " + effectName.fallback() + " " + level.fallback(),
					effectName,
					level
			));
		}

		for (final var passive_skill : relic.passive_skills.entrySet()) {
			final Text abilityName = name("passive_skill", passive_skill.getKey());
			final Text level = number(passive_skill.getValue());

			out.add(key(
					"tooltip.vitalrelics.passive_skill",
					"Ability: " + abilityName.fallback() + " " + level.fallback(),
					abilityName,
					level
			));
		}

		for (final var spell : relic.available_spells.entrySet()) {
			final Text spellName = name("spell", spell.getKey());
			// final Text level = number(spell.getValue().parameters.get("intensity"));

			out.add(key(
					"tooltip.vitalrelics.spell",
					"Spell: " + spellName.fallback(),
					spellName
			));
		}

		while (!out.isEmpty() && out.get(out.size() - 1).isBlank())
			out.remove(out.size() - 1);

		return out;
	}

	public static String displayName(final String id) {
		final StringBuilder out = new StringBuilder();

		for (final String word : id.split("_")) {
			if (word.isEmpty())
				continue;

			if (!out.isEmpty())
				out.append(' ');

			out.append(Character.toUpperCase(word.charAt(0)))
					.append(word.substring(1));
		}

		return out.toString();
	}

	private static void addProperty(
			final List<Text> out,
			final String id,
			final Relic.Properties.Info value) {

		final Text propertyName = name("property", id);

		if (nonZero(value.add)) {
			final Text amount = signed(value.add);

			out.add(key(
					"tooltip.vitalrelics.property.add",
					amount.fallback() + " " + propertyName.fallback(),
					amount,
					propertyName
			));
		}

		if (nonZero(value.mul_base)) {
			final Text amount = signedPercent(value.mul_base);

			out.add(key(
					"tooltip.vitalrelics.property.mul_base",
					amount.fallback() + " " + propertyName.fallback() +
							" from base",
					amount,
					propertyName
			));
		}

		if (value.mul_total != null && value.mul_total != 1.0) {
			final Text amount = number(value.mul_total);

			out.add(key(
					"tooltip.vitalrelics.property.mul_total",
					"x" + amount.fallback() + " total " +
							propertyName.fallback(),
					amount,
					propertyName
			));
		}
	}

	private static void addTick(
			final List<Text> out,
			final String id,
			final Relic.Ticks.Info value) {

		final Text interval = literal(format(value.interval_ticks / 20.0) + "s");
		final Text tickName = name("tick", id);

		if (nonZero(value.add)) {
			final Text amount = signed(value.add);

			out.add(key(
					"tooltip.vitalrelics.tick.add",
					"Every " + interval.fallback() + ": " +
							amount.fallback() + " " + tickName.fallback(),
					interval,
					amount,
					tickName
			));
		}

		if (nonZero(value.ratio_add)) {
			final Text amount = signedPercent(value.ratio_add);

			out.add(key(
					"tooltip.vitalrelics.tick.ratio_add",
					"Every " + interval.fallback() + ": " +
							amount.fallback() + " Max " + tickName.fallback(),
					interval,
					amount,
					tickName
			));
		}
	}

	private static void addCallback(
			final List<Text> out,
			final String id,
			final Relic.Callbacks.Info value) {

		final Text callbackName = name("callback", id);

		if (value.modifier != null) {
			final Text amount =
					signedPercentRaw((value.modifier - 1.0) * 100.0);

			out.add(key(
					"tooltip.vitalrelics.callback.modifier",
					amount.fallback() + " " + callbackName.fallback(),
					amount,
					callbackName
			));
		}

		if (nonZero(value.flat)) {
			final Text amount = signed(value.flat);

			out.add(key(
					"tooltip.vitalrelics.callback.flat",
					amount.fallback() + " " + callbackName.fallback(),
					amount,
					callbackName
			));
		}

		if (value.minimum != null) {
			final Text amount = number(value.minimum);

			out.add(key(
					"tooltip.vitalrelics.callback.minimum",
					"Minimum " + callbackName.fallback() +
							": " + amount.fallback(),
					callbackName,
					amount
			));
		}

		if (value.ratio_minimum != null) {
			final Text amount = percent(value.ratio_minimum);

			out.add(key(
					"tooltip.vitalrelics.callback.minimum_ratio",
					"Minimum " + callbackName.fallback() +
							": " + amount.fallback() + " of reference",
					callbackName,
					amount
			));
		}

		if (value.maximum != null) {
			final Text amount = number(value.maximum);

			out.add(key(
					"tooltip.vitalrelics.callback.maximum",
					"Maximum " + callbackName.fallback() +
							": " + amount.fallback(),
					callbackName,
					amount
			));
		}

		if (value.ratio_maximum != null) {
			final Text amount = percent(value.ratio_maximum);

			out.add(key(
					"tooltip.vitalrelics.callback.maximum_ratio",
					"Maximum " + callbackName.fallback() +
							": " + amount.fallback() + " of reference",
					callbackName,
					amount
			));
		}
	}

	private static Text name(
			final String category,
			final String id) {

		return key(
				"relic.vitalrelics." + category + "." + id,
				displayName(id)
		);
	}

	private static Text vanillaEffectName(final String id) {
		return vanillaKey(
				"effect.minecraft." + id,
				displayName(id)
		);
	}

	private static Text number(final double value) {
		return literal(format(value));
	}

	private static Text signed(final double value) {
		return literal((value >= 0.0 ? "+" : "") + format(value));
	}

	private static Text percent(final double value) {
		return literal(format(value * 100.0) + "%");
	}

	private static Text signedPercent(final double value) {
		return signedPercentRaw(value * 100.0);
	}

	private static Text signedPercentRaw(final double value) {
		return literal((value >= 0.0 ? "+" : "") + format(value) + "%");
	}

	private static boolean nonZero(final Double value) {
		return value != null && value != 0.0;
	}

	private static String format(final double value) {
		if (Math.abs(value - Math.round(value)) < 0.000001)
			return Long.toString(Math.round(value));

		return String.format(Locale.ROOT, "%.2f", value)
				.replaceAll("0+$", "")
				.replaceAll("\\.$", "");
	}

	public static String render(final Text text) {
		final Object[] arguments = text.arguments().stream()
				.map(RelicText::render)
				.toArray();

		final String pattern = text.translationKey() == null
				? text.fallback()
				: RelicTranslations.INSTANCE.translate(
				text.translationKey(),
				text.fallback()
		);

		try {
			return String.format(Locale.ROOT, pattern, arguments);
		} catch (RuntimeException exception) {
			return text.fallback();
		}
	}

}