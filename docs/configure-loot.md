# Configuring Loot

Vital Relics supports adding relic items to Minecraft loot tables.


WARNING: `customized` MUST BE SET TO `true`, if the `json` is correctly loaded,
while it's `false`, it'll be OVERWRITTEN.

## Loot Rules

A loot rule defines where a relic can be obtained through loot
generation.

A rule contains:

-   The target loot table
-   The relic item
-   The chance of appearing

## Example

``` json
{
  "table": "minecraft:chests/simple_dungeon",
  "chance": 0.05
}
```

This example adds the configured relic to dungeon chest loot with a 5%
chance.

## Loot Tables

Loot tables use Minecraft resource locations.

Examples:

-   `minecraft:chests/simple_dungeon`
-   `minecraft:chests/stronghold_library`
-   `minecraft:entities/zombie`

## Configuration Behavior

When a loot table is generated, Vital Relics checks configured loot
rules and adds matching relic entries to the loot table.
