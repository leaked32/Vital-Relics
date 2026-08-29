# add-new-relics

Every default player-facing relic must have at least one legitimate survival data route:

* crafting recipe, or
* loot-table data, or
* another explicitly documented data mechanism.

A relic intentionally unavailable in normal survival must be explicitly marked/documented as such.

No default relic may accidentally exist only in the creative inventory.

Epic relics are usually not loot-table.


### Recipe Integrity

For every entry in `recipes.json`:

* Recipe target refers to an existing relic.
* Recipe type is supported.
* Shaped recipe patterns are valid.
* Every pattern symbol has a corresponding key.
* No unused key symbols remain.
* Ingredient item IDs are valid.
* Output count is positive.

Cross-check in both files:

* Every recipe references an existing relic.
* Every relic intended to be craftable has a recipe.


### Loot Integrity

For every configured loot data:

* Relic ID exists.
* Loot-table ID is valid.
* Chance is within the accepted range.
* The loot table is reachable in normal gameplay.

Cross-check:

* No loot entry references a removed relic.
* Relics intended to be loot-exclusive actually have loot entries.


### Relic Integrity

For **every relic defined in `relics.json`**, verify:

* `id` is present.
* `id` is unique.
* Texture exists.
* Rarity is valid.
* Curio slot, when specified, is valid.
* All properties are recognized.
* All granted effects are recognized.
* All immunity effect IDs are valid.
* All callback IDs are implemented.
* All tick-action IDs are implemented.
* All passive-skill IDs are implemented.
* All spell IDs are implemented.
* Enemy-spawn entity IDs are valid.

