# Configuring Loot

Vital Relics supports adding relic items to Minecraft loot tables.

**WARNING:** Set `customized` to `true` after editing generated JSON. While it is `false`, the file may be overwritten.

## Loot Rules

A loot rule defines where a relic can be obtained through loot generation. A rule contains the target loot table and chance.

```json
{
  "table": "minecraft:chests/simple_dungeon",
  "chance": 0.05
}
```

This adds the configured relic to dungeon chest loot with a 5% chance. Loot tables use Minecraft resource locations such as `minecraft:chests/stronghold_library` and `minecraft:entities/zombie`.

When a loot table is generated, Vital Relics checks configured rules and adds matching relic entries.
