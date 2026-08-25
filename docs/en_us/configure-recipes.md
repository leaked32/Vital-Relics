# Configuring Recipes

Vital Relics supports dynamic relic crafting recipes. Recipes are matched automatically during crafting.

## Supported Recipe Types

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

Ingredient order does not matter for shapeless recipes. When a recipe matches, the configured relic is created.

## Remarks

Dynamic recipes work in-game, but recipes generated from `recipes.json` are not currently displayed in JEI. They can still be crafted normally.
