# Configuring Recipes

Vital Relics supports dynamic relic crafting recipes.

Recipes are defined through acquisition data and are matched
automatically during crafting.

## Supported Recipe Types

Vital Relics currently supports:

-   shaped recipes
-   shapeless recipes

## Shaped Recipes

A shaped recipe uses a pattern and key mapping.

Example:

``` json
{
  "type": "shaped",
  "pattern": [
    "ABC",
    "DEF",
    " G "
  ],
  "key": {
    "A": "minecraft:diamond",
    "B": "minecraft:gold_ingot",
    "G": "minecraft:ender_pearl"
  }
}
```

The pattern follows the normal Minecraft crafting layout rules.

## Shapeless Recipes

A shapeless recipe only checks whether the required ingredients exist.

Example:

``` json
{
  "type": "shapeless",
  "ingredients": [
    "minecraft:diamond",
    "minecraft:gold_ingot"
  ]
}
```

The ingredient order does not matter.

## Recipe Result

When a recipe matches successfully, the configured relic item is
created.

## Remarks

The dynamic crafting system currently works correctly in-game,
but crafting recipes generated from recipes.json are not displayed in JEI yet.
This is a known limitation. The recipes can still be crafted normally;
JEI integration will be added in a future update.

