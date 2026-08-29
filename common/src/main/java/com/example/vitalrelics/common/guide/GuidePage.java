package com.example.vitalrelics.common.guide;

import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Acquisition;
import com.example.vitalrelics.common.relics.Translations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class GuidePage {
	public final String id;
	public final String title;
	public final String rarity;
	public final String slot;
	public final String description;

	public final List<Section> sections = new ArrayList<>();

	private GuidePage(
			final String id, final String title, final String rarity,
			final String slot, final String description) {

		this.id = id;
		this.title = title;
		this.rarity = rarity;
		this.slot = slot;
		this.description = description;
	}

	public static GuidePage from(
			final GuideBook.Entry entry,
			final Function<String, String> ingredientName) {

		if (entry == null)
			throw new IllegalArgumentException("entry cannot be null");
		if (ingredientName == null)
			throw new IllegalArgumentException("ingredientName cannot be null");

		final Relic relic = entry.relic;

		final GuidePage page = new GuidePage(
				relic.id,
				tr("item.vitalrelics." + relic.id, displayName(relic)),
				relic.rarity,
				relic.curio_slot,
				tr("tooltip.vitalrelics." + relic.id, relic.tooltip)
		);

		addEffectiveSlots(page, relic);
		addProperties(page, relic);
		addGrantedEffects(page, relic);
		addImmunities(page, relic);
		addPassiveSkills(page, relic);
		addSpells(page, relic);
		addTicks(page, relic);
		addCallbacks(page, relic);
		addEnemySpawn(page, relic);
		addAcquisition(page, entry, ingredientName);

		return page;
	}

	private static String displayName(final Relic relic) {
		if (relic.display_name != null && !relic.display_name.isBlank())
			return relic.display_name;

		return humanize(relic.id);
	}

	private static void addEffectiveSlots(final GuidePage page, final Relic relic) {
		if (relic.effective_slots.isEmpty())
			return;

		final Section section = new Section(
				tr("guide.vitalrelics.section.effective_slots", "Effective Slots")
		);

		for (final String slot : relic.effective_slots)
			section.lines.add(humanize(slot));

		page.sections.add(section);
	}

	private static void addProperties(final GuidePage page, final Relic relic) {
		if (relic.properties.isEmpty())
			return;

		final Section section = new Section(
				tr("guide.vitalrelics.section.properties", "Properties")
		);

		for (final Map.Entry<String, Relic.Properties.Info> entry : relic.properties.entrySet()) {
			final Relic.Properties.Info info = entry.getValue();
			final String name = translatedProperty(entry.getKey());

			if (info.add != null) {
				section.lines.add(trf(
						"guide.vitalrelics.property.add", "%s: +%s",
						name, format(info.add)
				));
			}

			if (info.mul_base != null) {
				section.lines.add(trf(
						"guide.vitalrelics.property.mul_base", "%s base: ×%s",
						name, format(info.mul_base)
				));
			}

			if (info.mul_total != null) {
				section.lines.add(trf(
						"guide.vitalrelics.property.mul_total", "%s total: ×%s",
						name, format(info.mul_total)
				));
			}
		}

		if (!section.lines.isEmpty())
			page.sections.add(section);
	}

	private static void addGrantedEffects(final GuidePage page, final Relic relic) {
		if (relic.granted_effects.isEmpty())
			return;

		final Section section = new Section(
				tr("guide.vitalrelics.section.granted_effects", "Granted Effects")
		);

		for (final var entry : relic.granted_effects.entrySet()) {
			section.lines.add(trf(
					"guide.vitalrelics.effect.granted", "%s %s",
					humanizeIdentifier(entry.getKey()),
					romanNumeral(entry.getValue() + 1)
			));
		}

		page.sections.add(section);
	}

	private static void addImmunities(final GuidePage page, final Relic relic) {
		if (relic.immune_to_effects.isEmpty())
			return;

		final Section section = new Section(
				tr("guide.vitalrelics.section.immunities", "Immunities")
		);

		for (final String effect : relic.immune_to_effects)
			section.lines.add(humanizeIdentifier(effect));

		page.sections.add(section);
	}

	private static void addPassiveSkills(final GuidePage page, final Relic relic) {
		if (relic.passive_skills.isEmpty())
			return;

		final Section section = new Section(
				tr("guide.vitalrelics.section.passive_skills", "Passive Skills")
		);

		for (final var entry : relic.passive_skills.entrySet()) {
			section.lines.add(trf(
					"guide.vitalrelics.passive_skill", "%s: %s",
					translatedPassiveSkill(entry.getKey()),
					format(entry.getValue())
			));
		}

		page.sections.add(section);
	}

	private static void addSpells(final GuidePage page, final Relic relic) {
		if (relic.available_spells.isEmpty())
			return;

		final Section section = new Section(
				tr("guide.vitalrelics.section.spells", "Spells")
		);

		for (final var spellEntry : relic.available_spells.entrySet()) {
			section.lines.add(translatedSpell(spellEntry.getKey()));

			for (final var parameter : spellEntry.getValue().parameters.entrySet()) {
				section.lines.add(trf(
						"guide.vitalrelics.parameter", "  %s: %s",
						humanize(parameter.getKey()),
						formatObject(parameter.getValue())
				));
			}
		}

		page.sections.add(section);
	}

	private static void addTicks(final GuidePage page, final Relic relic) {
		if (relic.ticks.isEmpty())
			return;

		final Section section = new Section(
				tr("guide.vitalrelics.section.periodic_effects", "Periodic Effects")
		);

		for (final var entry : relic.ticks.entrySet()) {
			final Relic.Ticks.Info info = entry.getValue();

			section.lines.add(tr(
					"relic.vitalrelics.tick." + entry.getKey(),
					humanize(entry.getKey())
			));

			section.lines.add(trf(
					"guide.vitalrelics.tick.interval", "  Interval: %s",
					formatTicks(info.interval_ticks)
			));

			if (info.add != null) {
				section.lines.add(trf(
						"guide.vitalrelics.tick.add", "  Add: %s",
						format(info.add)
				));
			}

			if (info.ratio_add != null) {
				section.lines.add(trf(
						"guide.vitalrelics.tick.ratio", "  Ratio: %s",
						formatPercent(info.ratio_add)
				));
			}
		}

		page.sections.add(section);
	}

	private static void addCallbacks(final GuidePage page, final Relic relic) {
		if (relic.callbacks.isEmpty())
			return;

		final Section section = new Section(
				tr("guide.vitalrelics.section.callbacks", "Callbacks")
		);

		for (final var entry : relic.callbacks.entrySet()) {
			final Relic.Callbacks.Info info = entry.getValue();

			section.lines.add(tr(
					"relic.vitalrelics.callback." + entry.getKey(),
					humanize(entry.getKey())
			));

			if (info.modifier != null) {
				section.lines.add(trf(
						"guide.vitalrelics.callback.modifier", "  Modifier: ×%s",
						format(info.modifier)
				));
			}

			if (info.flat != null) {
				section.lines.add(trf(
						"guide.vitalrelics.callback.flat", "  Flat: %s",
						signed(info.flat)
				));
			}

			if (info.minimum != null) {
				section.lines.add(trf(
						"guide.vitalrelics.callback.minimum", "  Minimum: %s",
						format(info.minimum)
				));
			}

			if (info.ratio_minimum != null) {
				section.lines.add(trf(
						"guide.vitalrelics.callback.ratio_minimum", "  Ratio minimum: %s",
						formatPercent(info.ratio_minimum)
				));
			}

			if (info.maximum != null) {
				section.lines.add(trf(
						"guide.vitalrelics.callback.maximum", "  Maximum: %s",
						format(info.maximum)
				));
			}

			if (info.ratio_maximum != null) {
				section.lines.add(trf(
						"guide.vitalrelics.callback.ratio_maximum", "  Ratio maximum: %s",
						formatPercent(info.ratio_maximum)
				));
			}
		}

		page.sections.add(section);
	}

	private static void addEnemySpawn(final GuidePage page, final Relic relic) {
		if (relic.enemy_spawn.isEmpty())
			return;

		final Section section = new Section(
				tr("guide.vitalrelics.section.enemy_spawn", "Enemy Spawn")
		);

		for (final var entry : relic.enemy_spawn.entrySet()) {
			section.lines.add(trf(
					"guide.vitalrelics.enemy_spawn", "%s: %s",
					humanizeIdentifier(entry.getKey()),
					formatPercent(entry.getValue())
			));
		}

		page.sections.add(section);
	}

	private static void addAcquisition(
			final GuidePage page,
			final GuideBook.Entry entry,
			final Function<String, String> ingredientName) {
		final Section section = new Section(
				tr("guide.vitalrelics.section.acquisition", "Acquisition")
		);

		if (entry.acquisitionUndefined) {
			section.lines.add(tr(
					"guide.vitalrelics.acquisition.unavailable",
					"Not available through normal survival acquisition"
			));
			page.sections.add(section);
			return;
		}

		if (entry.recipe != null)
			addRecipe(section, entry.recipe, ingredientName);

		for (final Acquisition.Data.Loot loot : entry.loot) {
			section.lines.add(trf(
					"guide.vitalrelics.acquisition.loot", "Loot: %s (%s)",
					humanizeIdentifier(loot.table),
					formatPercent(loot.chance)
			));
		}

		if (section.lines.isEmpty()) {
			section.lines.add(tr(
					"guide.vitalrelics.acquisition.none",
					"No configured acquisition method"
			));
		}

		page.sections.add(section);
	}

	private static void addRecipe(
			final Section section,
			final Acquisition.Data.Crafting recipe,
			final Function<String, String> ingredientName) {

		section.lines.add(trf(
				"guide.vitalrelics.acquisition.crafting", "Crafting: %s",
				humanize(recipe.type)
		));

		if ("shaped".equals(recipe.type)) {
			for (final String row : recipe.pattern)
				section.lines.add("  " + row);

			for (final var entry : recipe.key.entrySet()) {
				section.lines.add(
						"  " + entry.getKey() + " = " +
								resolveIngredientName(entry.getValue(), ingredientName)
				);
			}
		} else if ("shapeless".equals(recipe.type)) {
			for (final String ingredient : recipe.ingredients)
				section.lines.add("  " + resolveIngredientName(ingredient, ingredientName));
		}

		if (recipe.count != 1) {
			section.lines.add(trf(
					"guide.vitalrelics.acquisition.output", "  Output: %s",
					recipe.count
			));
		}
	}

	private static String resolveIngredientName(
			final String id,
			final Function<String, String> ingredientName) {

		final String translated = ingredientName.apply(id);

		if (translated == null || translated.isBlank())
			return humanizeIdentifier(id);

		return translated;
	}

	private static String translatedProperty(final String id) {
		return tr("relic.vitalrelics.property." + id, humanize(id));
	}

	private static String translatedPassiveSkill(final String id) {
		return tr("relic.vitalrelics.passive_skill." + id, humanize(id));
	}

	private static String translatedSpell(final String id) {
		return tr("relic.vitalrelics.spell." + id, humanize(id));
	}

	private static String tr(final String key, final String fallback) {
		return Translations.get().translate(key, fallback);
	}

	private static String trf(
			final String key, final String fallback,
			final Object... arguments) {

		return String.format(
				Locale.ROOT,
				tr(key, fallback),
				arguments
		);
	}

	private static String formatObject(final Object value) {
		if (value instanceof Number number)
			return format(number.doubleValue());

		return String.valueOf(value);
	}

	private static String format(final double value) {
		if (Math.rint(value) == value)
			return Long.toString((long) value);

		return String.format(Locale.ROOT, "%.3f", value)
				.replaceAll("0+$", "")
				.replaceAll("\\.$", "");
	}

	private static String signed(final double value) {
		return value > 0.0 ? "+" + format(value) : format(value);
	}

	private static String formatPercent(final double value) {
		return format(value * 100.0) + "%";
	}

	private static String formatTicks(final int ticks) {
		if (ticks % 20 == 0)
			return format(ticks / 20.0) + "s";

		return ticks + " ticks";
	}

	private static String humanizeIdentifier(final String id) {
		final int separator = id.indexOf(':');

		if (separator >= 0 && separator + 1 < id.length())
			return humanize(id.substring(separator + 1));

		return humanize(id);
	}

	private static String humanize(final String value) {
		if (value == null || value.isBlank())
			return "";

		final String normalized = value.replace('_', ' ').replace('-', ' ');
		final String[] words = normalized.split("\\s+");
		final StringBuilder result = new StringBuilder();

		for (final String word : words) {
			if (word.isEmpty())
				continue;

			if (!result.isEmpty())
				result.append(' ');

			result.append(Character.toUpperCase(word.charAt(0)));

			if (word.length() > 1)
				result.append(word.substring(1));
		}

		return result.toString();
	}

	private static String romanNumeral(final int value) {
		return switch (value) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			case 6 -> "VI";
			case 7 -> "VII";
			case 8 -> "VIII";
			case 9 -> "IX";
			case 10 -> "X";
			default -> Integer.toString(value);
		};
	}

	public static class Section {
		public final String title;
		public final List<String> lines = new ArrayList<>();

		public Section(final String title) {
			this.title = title;
		}
	}
}
