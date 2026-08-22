# Configuring Relics

Vital Relics is a highly configurable relic system. Relic definitions
are data-driven and are configured through `relics.json`.

A relic can define:

-   identity and display information
-   equipment locations
-   Curios integration
-   passive effects
-   immunities
-   attributes
-   periodic actions
-   damage rules
-   special abilities

**WARNING**: `customized` MUST BE SET TO `true`, if the `json` is correctly loaded,
while it's `false`, it'll be **OVERWRITTEN**.

The configuration file is located under `config/vitalrelics`.
For example:
- Isolated case: `minecraft/.minecraft/versions/Closing Song 1.6.5/config/vitalrelics/relics.json`
- Default case: `minecraft/.minecraft/config/vitalrelics/relics.json`

The configuration files should be automatically generated after you launch the game with
this mod installed. So, you don't need to copy them on your own.



## Basic Information

### `id`

The unique identifier of the relic.

Example:

``` json
{
  "id": "iron_heart"
}
```

Relic IDs are not hardcoded. New relic IDs can be created through
configuration.

### `display_name`

Optional custom display name.

``` json
{
  "display_name": "Super Iron Heart"
}
```

### `tooltip`

The relic tooltip text.

``` json
{
  "tooltip": "A small heart forged from stubborn iron."
}
```

### `texture`

The relic texture file.

``` json
{
  "texture": "iron_heart.png"
}
```

### `rarity`

The relic rarity.

Example:

``` json
{
  "rarity": "rare"
}
```

## Curios Configuration

### `curio_slot`

Defines the Curios slot type used when the relic is equipped.

``` json
{
  "curio_slot": "charm"
}
```

The available slot types depend on installed mods and Curios
configuration.

### `effective_slots`

Defines where the relic effects are active.

``` json
{
  "effective_slots": [
    "in_curios_api_slots"
  ]
}
```

Available locations:

``` text
in_hotbar
in_inventory
in_curios_api_slots
in_touhou_little_maid_curios_slots
```

`in_inventory` includes the hotbar.

If `effective_slots` is omitted or empty, the default behavior is:

``` text
in_curios_api_slots
in_touhou_little_maid_curios_slots
```

Multiple locations can be combined.

When Touhou Little Maid versions with maid Curios support are installed,
relics in `in_touhou_little_maid_curios_slots` function as Curios for
maids as well.

## Effect Immunity

### `immune_to_effects`

Blocks specified potion effects.

``` json
{
  "immune_to_effects": [
    "poison",
    "blindness"
  ]
}
```

Special value:

``` json
{
  "immune_to_effects": "all_negative"
}
```

provides immunity against all negative effects.

## Passive Effects

### `add_effects`

Provides continuous potion effects.

``` json
{
  "add_effects": {
    "night_vision": 1,
    "speed": 2
  }
}
```

The value represents the effect level.

## Special Abilities

### `special_abilities`

Defines special relic abilities.

```text
"retarget_arrow": anti-skeleton, new arrow will have at least [attribution damage * level] as damage.
"flight": grants flight ability
"reality_severance": all hostile living entities in range [level] cannot be invulnerable,
		receive constant damage [attribution damage * (level / 100)]
 		and receive constant negative effects with level [level / 4].
```

``` json
{
  "special_abilities": {
    "retarget_arrow": 2
  }
}
```

## Attribute Modifiers

Attributes are configured under `properties`.

Supported attributes:

``` text
attack_damage
attack_speed
block_interaction_range
entity_interaction_range
knockback_resistance
max_health
```

Each attribute supports:

``` json
{
  "add": 4.0,
  "mul_base": 0.5,
  "mul_total": 1.25
}
```

-   `add`: Adds a flat amount.
-   `mul_base`: Adds a multiplier based on the base attribute value.
-   `mul_total`: Multiplies the final attribute value.

Example:

``` json
{
  "properties": {
    "max_health": {
      "add": 10.0
    }
  }
}
```

## Periodic Actions

Periodic actions are configured under `ticks`.

Supported actions:

``` text
heal
feed
```

Each action supports:

``` json
{
  "interval_ticks": 20,
  "add": 1,
  "ratio_add": 0.01
}
```

-   `interval_ticks`: Time between executions.
-   `add`: Flat amount added.
-   `ratio_add`: Amount based on maximum value.

Example:

``` json
{
  "ticks": {
    "heal": {
      "interval_ticks": 20,
      "add": 1,
      "ratio_add": 0.01
    }
  }
}
```

## Callback Rules

Callbacks modify events such as damage and invulnerability time.

Supported callbacks:

``` text
damage_taken
damage_dealt
invulnerable_time_taken
invulnerable_time_dealt
```

Each callback supports:

``` json
{
  "modifier": 0.5,
  "flat": 2,
  "minimum": 10,
  "ratio_minimum": 0.1,
  "maximum": 20,
  "ratio_maximum": 0.5
}
```

-   `modifier`: Multiplies the value.
-   `flat`: Adds a flat amount.
-   `minimum`: Absolute lower bound.
-   `ratio_minimum`: Lower bound relative to reference value.
-   `maximum`: Absolute upper bound.
-   `ratio_maximum`: Upper bound relative to reference value.

Example:

``` json
{
  "callbacks": {
    "damage_taken": {
      "modifier": 0.1,
      "maximum": 4,
      "ratio_maximum": 0.1
    }
  }
}
```

## Example Relic

``` json
{
  "id": "cherry_cross",
  "tooltip": "The pure power of cherry trees.",
  "texture": "cherry_cross.png",
  "rarity": "epic",
  "curio_slot": "necklace",

  "effective_slots": [
    "in_curios_api_slots",
    "in_touhou_little_maid_curios_slots"
  ],

  "immune_to_effects": [
    "darkness",
    "wither"
  ],

  "add_effects": {
    "night_vision": 1
  },

  "properties": {
    "max_health": {
      "add": 20.0
    }
  }
}
```
