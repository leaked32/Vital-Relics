package com.example.vitalrelics.common.guide;

import com.example.vitalrelics.common.Acquisition;
import com.example.vitalrelics.common.RelicAcquisitionLoader;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GuideBook {
	private final List<Entry> entries = new ArrayList<>();
	private final Map<String, Entry> entriesById = new LinkedHashMap<>();

	public GuideBook(
			final RelicLoader relicLoader,
			final RelicAcquisitionLoader relicAcquisitionLoader) {

		if (relicLoader == null)
			throw new IllegalArgumentException("relicLoader cannot be null");
		if (relicAcquisitionLoader == null)
			throw new IllegalArgumentException("acquisitionLoader cannot be null");

		rebuild(relicLoader, relicAcquisitionLoader);
	}

	public void rebuild(
			final RelicLoader relicLoader,
			final RelicAcquisitionLoader relicAcquisitionLoader) {

		if (relicLoader == null)
			throw new IllegalArgumentException("relicLoader cannot be null");
		if (relicAcquisitionLoader == null)
			throw new IllegalArgumentException("acquisitionLoader cannot be null");

		entries.clear();
		entriesById.clear();

		final Acquisition acquisition = relicAcquisitionLoader.data;

		for (final Relic relic : relicLoader.relics_) {
			final Acquisition.Crafting recipe =
					acquisition.recipes.get(relic.id);

			final List<Acquisition.Loot> loot =
					acquisition.loot.getOrDefault(relic.id, List.of());

			final Entry entry = new Entry(
					relic,
					recipe,
					List.copyOf(loot),
					acquisition.undefined.contains(relic.id)
			);

			entries.add(entry);
			entriesById.put(relic.id, entry);
		}
	}

	public List<Entry> entries() {
		return Collections.unmodifiableList(entries);
	}

	public Entry find(final String relicId) {
		return entriesById.get(relicId);
	}

	public static class Entry {
		public final Relic relic;
		public final Acquisition.Crafting recipe;
		public final List<Acquisition.Loot> loot;
		public final boolean acquisitionUndefined;

		private Entry(
				final Relic relic,
				final Acquisition.Crafting recipe,
				final List<Acquisition.Loot> loot,
				final boolean acquisitionUndefined) {

			this.relic = relic;
			this.recipe = recipe;
			this.loot = loot;
			this.acquisitionUndefined = acquisitionUndefined;
		}

		public boolean craftable() {
			return recipe != null;
		}

		public boolean lootable() {
			return !loot.isEmpty();
		}

		public boolean hasSurvivalAcquisition() {
			return craftable() || lootable();
		}
	}
}
