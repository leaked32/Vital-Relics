package com.example.vitalrelics.common.guide;

import com.example.vitalrelics.common.relics.Acquisition;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GuideBook {
	private final List<Entry> entries = new ArrayList<>();
	private final Map<String, Entry> entriesById = new LinkedHashMap<>();

	public GuideBook(
			final Loader loader,
			final Acquisition acquisition) {

		if (loader == null)
			throw new IllegalArgumentException("relicLoader cannot be null");
		if (acquisition == null)
			throw new IllegalArgumentException("acquisitionLoader cannot be null");

		rebuild(loader, acquisition);
	}

	public void rebuild(
			final Loader loader,
			final Acquisition acquisition) {

		if (loader == null)
			throw new IllegalArgumentException("relicLoader cannot be null");
		if (acquisition == null)
			throw new IllegalArgumentException("acquisitionLoader cannot be null");

		entries.clear();
		entriesById.clear();

		final Acquisition.Data data = acquisition.data;

		for (final Relic relic : loader.relics_) {
			final Acquisition.Data.Crafting recipe =
					data.recipes.get(relic.id);

			final List<Acquisition.Data.Loot> loot =
					data.loot.getOrDefault(relic.id, List.of());

			final Entry entry = new Entry(
					relic,
					recipe,
					List.copyOf(loot),
					data.undefined.contains(relic.id)
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
		public final Acquisition.Data.Crafting recipe;
		public final List<Acquisition.Data.Loot> loot;
		public final boolean acquisitionUndefined;

		private Entry(
				final Relic relic,
				final Acquisition.Data.Crafting recipe,
				final List<Acquisition.Data.Loot> loot,
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
