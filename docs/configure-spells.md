# Configure Spells

Vital Relics includes a data-driven spell system that allows equipped relics to provide active abilities.

Spells are configured inside a relic's `available_spells` object in `relics.json`.

A relic may provide one or more spells, and the same spell implementation can be reused by different relics with different parameters.

## Basic Format

```json
{
  "id": "example_relic",
  "available_spells": {
    "teleport": {
      "range": 64,
      "recovery": 2
    }
  }
}
```

The key inside `available_spells` is the spell ID.

```json
"teleport"
```

Everything inside that spell object is passed to the spell implementation as parameters.

Spell parameters are intentionally open-ended. Different spell types may define different parameters without requiring a fixed JSON schema for every spell.

## Multiple Spells

A relic may provide multiple spells:

```json
{
  "id": "example_relic",
  "available_spells": {
    "teleport": {
      "range": 64,
      "recovery": 2
    },
    "curse": {
      "intensity": 75,
      "range": 32,
      "recovery": 1
    }
  }
}
```

Available spells can be switched during gameplay and the currently selected spell can then be cast.

Spell selection and cooldowns are tracked per living entity.

## Recovery and Cooldown

Most spells use the `recovery` parameter to determine their cooldown.

```json
"recovery": 2
```

The cooldown is calculated as:

```text
cooldown_seconds = 1 / recovery
```

For example:

| `recovery` |     Cooldown |
| ---------: | -----------: |
|     `0.25` |    4 seconds |
|      `0.5` |    2 seconds |
|        `1` |     1 second |
|        `2` |  0.5 seconds |
|        `4` | 0.25 seconds |

A higher recovery value therefore means the spell can be used more frequently.

A spell with a recovery value of `0` or less effectively cannot recover normally.

Cooldown is only applied after a spell successfully activates. A failed cast, such as attempting to use a targeted spell without a valid target, does not consume the spell cooldown.

## Duplicate Spells

Multiple equipped relics may provide the same spell.

Vital Relics combines the available spells and chooses one configuration for each spell ID.

A spell can explicitly define its selection priority:

```json
"teleport": {
  "range": 128,
  "recovery": 2,
  "priority": 10
}
```

When `priority` is present, it is used to compare duplicate instances of that spell.

If `priority` is not provided, the default score is:

```text
intensity * recovery
```

This allows stronger versions of a spell to replace weaker versions when several relics provide the same spell.

For spells that do not naturally use `intensity`, adding an explicit `priority` is recommended when duplicate versions need deterministic strength ordering.

# Available Spells

## Teleport

Spell ID:

```text
teleport
```

Teleport moves the caster toward the location they are looking at.

Example:

```json
"available_spells": {
  "teleport": {
    "range": 128,
    "recovery": 2
  }
}
```

### Parameters

| Parameter  | Description                                 |
| ---------- | ------------------------------------------- |
| `range`    | Maximum teleport distance in blocks         |
| `recovery` | Cooldown recovery rate                      |
| `priority` | Optional duplicate-spell selection priority |

`range` is limited to a maximum of 256 blocks.

### Targeting

When the caster points into open space, the spell attempts to teleport as far along the look direction as possible while finding a valid destination.

When the caster points at a block, the spell attempts to place the caster at an appropriate position around that block.

The teleport implementation takes block collision shapes into account, including partial-height blocks such as slabs.

Thin blocks such as doors, trapdoors, iron bars, and glass panes use their block cell as the teleport destination.

Snow layers are ignored for spell targeting so that small amounts of snow do not unexpectedly intercept the teleport ray.

If there is no room above the targeted block, teleport attempts to use the space immediately before the face that the caster targeted.

Teleportation fails without consuming its cooldown when no valid destination can be found.

## Curse

Spell ID:

```text
curse
```

Curse attacks the living entity directly targeted by the caster.

Example:

```json
"available_spells": {
  "curse": {
    "intensity": 100,
    "range": 128,
    "recovery": 2
  }
}
```

### Parameters

| Parameter   | Description                                                   |
| ----------- | ------------------------------------------------------------- |
| `intensity` | Damage strength as a percentage of the caster's attack damage |
| `range`     | Maximum targeting range in blocks                             |
| `recovery`  | Cooldown recovery rate                                        |
| `priority`  | Optional duplicate-spell selection priority                   |

`range` is limited to a maximum of 256 blocks.

The damage calculation is:

```text
damage = caster attack damage * intensity / 100
```

For example, with:

```json
"intensity": 150
```

the spell deals damage equal to 150% of the caster's attack damage.

Curse requires a valid living target.

It will fail if:

* no living entity is being targeted;
* the target is outside the configured range;
* the target is allied with the caster;
* `intensity` or `range` is not positive.

A failed cast does not consume the spell cooldown.

# Example Relics

The default `celestial_wings` relic provides teleport:

```json
{
  "id": "celestial_wings",
  "texture": "celestial_wings.png",
  "rarity": "epic",
  "passive_abilities": {
    "flight": 1
  },
  "available_spells": {
    "teleport": {
      "range": 128,
      "recovery": 2
    }
  }
}
```

The default `cursed_crimson_rune` provides curse:

```json
{
  "id": "cursed_crimson_rune",
  "texture": "cursed_crimson_rune.png",
  "rarity": "epic",
  "available_spells": {
    "curse": {
      "intensity": 100,
      "range": 128,
      "recovery": 2
    }
  }
}
```

# Entity Support

The spell system operates on `LivingEntity` rather than being restricted to players.

This is intentional.

Any supported living entity that can provide equipped relics may use the same spell definitions and spell implementations. Player-specific behavior, such as displaying spell-selection or cooldown messages, is added separately and does not define the core spell system.

This allows the same relic architecture to be reused by players, compatible entities, and custom mobs.

# Adding Spells to Custom Relics

To give a custom relic a spell:

1. Add an `available_spells` object to the relic.
2. Add the desired spell ID.
3. Configure the parameters supported by that spell.

For example:

```json
{
  "id": "wanderers_charm",
  "display_name": "Wanderer's Charm",
  "tooltip": "Space seems slightly less restrictive around its bearer.",
  "texture": "wanderers_charm.png",
  "rarity": "rare",

  "available_spells": {
    "teleport": {
      "range": 48,
      "recovery": 0.5
    }
  }
}
```

This creates a relic with a 48-block teleport and a two-second cooldown.

No Java changes are required when assigning an existing spell to another relic.
