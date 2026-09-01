# Configuring Recipes

Vital Relics supports dynamic relic crafting recipes and adding relic items to Minecraft loot tables.

**WARNING:** Set `customized` to `true` after editing generated JSON. While it is `false`, the file may be overwritten.

## Recipes

Recipes are matched automatically during crafting.

- shaped
- shapeless

```json
{
  "type": "shaped",
  "pattern": ["ABC", "DEF", " G "],
  "key": {
    "A": "minecraft:diamond",
    "B": "minecraft:gold_ingot",
    "G": "minecraft:ender_pearl"
  }
}
```

```json
{
  "type": "shapeless",
  "ingredients": ["minecraft:diamond", "minecraft:gold_ingot"]
}
```

Ingredient order does not matter for shapeless recipes.

# Loot-table

A loot rule defines where a relic can be obtained through loot generation. A rule contains the target loot table and chance.

Example
```json
{
  "table": "minecraft:chests/simple_dungeon",
  "chance": 0.05
}
```

This adds the configured relic to dungeon chest loot with a 5% chance. Loot tables use Minecraft resource locations such as `minecraft:chests/stronghold_library` and `minecraft:entities/zombie`.

When a loot table is generated, Vital Relics checks configured rules and adds matching relic entries.
