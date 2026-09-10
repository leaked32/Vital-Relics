# TODO

- `borebolt` should replace fluid with air.
- `compel_attack` did the reverse, it should be done by making current hurt by the nearest living entity instead. 
- `flight` may stop working after entering a new dimension. But re-equiping the relic may fix it.

## Discovered Bugs

## New Relic Items

Mira~ Just one more work:

New rare relics:
A new ring that has the passive skill which grants all hostile living entities in level range with slowness 3 effect, triggers once per 20 ticks, effect lasts for 3 seconds.
A new ring that has the passive skill which enables the user destroy the entire tree.
A new ring that has the passive skill which enables the user to destroy multiple blocks in level range by the same data (I meant, Minecraft may record how the block is destroyed, like fortune level, which changes the drops).
A new bracelet that has a nwe spell which launches an arrow with durability, when the durability reaches 0, it disappears, how many its durability decreases per one tick, the arrow speed and total durability are all described by the relic spell. When it hits a block, if its current durability is greater than the block durability, break the block and subtract the arrow durability, otherwise, the arrow disappears. It can also trigger the passive skill of others.
A new bracelet that has a new spell which makes the target attacks the nearest living entity (except self) immediately no matter what.

Please add them with translations, and generate the textures with the art guideline of Minecraft.

Please re-generate the textures with the art guideline of Minecraft.

## Upgradable
- Update all (bracelet, ring) textures with only duplicated colored ones.
- Currently, enemies spawned with relics are too weak after you *optimized* it (lower the probability).
- Make textures loadable outside.
- Make more protective necklace/ head curios for beginner, intermediate level.
- Upgrade the configuration files. 
    we may need to create a better configuration file which should support grouping well, 
    with comments, I don't know whether `toml` is good but `toml` only allows nested keys.
    But changing it may cause users who have really changed `relics.json` adapt to it.
- Polish the presentations of enemies with these relics.
- Make crafting guides in the guide book a dedicated UI.
- Documents under `docs/` are very old, though there should be nobody reading them yet.
- Plausible playable passive skills can be added:
  - Fire Resistance: vanilla fire resistance only ignores the damage caused by fire,
      but the fire is still there (`fire_resistance_ring`).
  - Lava Swimmer: player can swim happily in lava with clear sight (`fire_resistance_ring`).
- Plausible playable spells can be added:
  - Cruel Cleanse: maybe there's a better name, remove all positive effects on the pointed target, 
      it should have longer cooldown.

## Completed
- Added spells:
    - Disenchantment: disenchant an enchantment to the book holding on the left hand (`arcane_reforging_charm`).
- Fix guide-book effect translations. It’s a discovered bug, likely small, and immediately removes visible roughness.
- Document relic stacking rules in the guide book. Very high value because players otherwise cannot reason about builds.
- Explain the stacking rules of the relic in guide book:
  - Callbacks and Properties can be stacked by equipped the same relic into different slots.
      Stacking them in the same slot does not count.
  - If the same passive skill appears in more than one equipped relic, only highest level takes effect.
  - Each spell is unique for its unique relic, so there's nothing to worry about it.

