# add-new-relics

### Acquisition invariant

Every default player-facing relic must have at least one legitimate survival acquisition route:

* crafting recipe, or
* loot-table acquisition, or
* another explicitly documented acquisition mechanism.

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
