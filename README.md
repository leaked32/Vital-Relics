# Vital Relics

A highly configurable relic mod for Minecraft.

Vital Relics adds equippable relics ranging from small utility charms to extremely powerful
end-game artifacts. Unlike many relic mods, most relic behavior is **data-driven**: relic IDs,
textures, rarity, attributes, passive effects, regeneration, damage rules, equipment locations,
and other behavior can be configured through `relics.json`.

The built-in relics are only the default configuration. You can modify them, remove them, or
define your own relics without changing the mod's Java code.

## Supported Versions

| Minecraft | Loader |
| --- | --- |
| 1.20.1 | Forge |
| 1.21.1 | NeoForge |

Vital Relics also supports **Curios API** and optional **Touhou Little Maid** integration.

## Screenshots

<img width="1920" height="1104" alt="2026-08-20_14 58 39" src="https://github.com/user-attachments/assets/a3e49454-14e9-4b9c-894c-727dfe9d1050" />

<img width="1920" height="1104" alt="2026-08-20_14 59 20" src="https://github.com/user-attachments/assets/aabf218f-aa11-4787-9083-5bf32bb71a3e" />

<img width="1920" height="1104" alt="2026-08-20_14 59 25" src="https://github.com/user-attachments/assets/c931d595-af92-4843-b82f-299709a0f4a0" />

<img width="1920" height="1104" alt="2026-08-20_17 28 07" src="https://github.com/user-attachments/assets/5a2aa56e-6b22-4ba3-9126-f28d97e0796d" />

## Acquisition System

Vital Relics separates **relic definitions** from **how players obtain them**.

Relic behavior is defined in `relics.json`, while acquisition methods are defined separately in `recipes.json`.

This allows modpack creators to freely decide how each relic should enter their world without modifying the mod itself.

---

## Crafting Recipes

Crafting recipes are configured through `recipes.json`.

Example:

```json
{
  "recipes": {
    "iron_heart": {
      "type": "shaped",
      "pattern": [
        " I ",
        "IRI",
        " I "
      ],
      "key": {
        "I": "minecraft:iron_ingot",
        "R": "minecraft:redstone"
      }
    }
  }
}

## Configuration

After the first launch, Vital Relics creates:

```text
config/vitalrelics/relics.json
```

This file contains the relic definitions used by the mod.

You are free to edit it.

A relic can define its own:

- ID and texture
- Tooltip and rarity
- Curios slot
- Effective inventory/equipment locations
- Attribute modifiers
- Passive potion effects
- Effect immunities
- Periodic healing and feeding
- Damage dealt/taken modification
- Invulnerability-time modification
- Special abilities

Relic IDs are **not hardcoded**. You can create entirely new relic IDs through configuration.

## Example Relic

```json
{
  "id": "cherry_cross",
  "tooltip": "The pure power of cherry trees.",
  "texture": "cherry_cross.png",
  "rarity": "epic",
  "curio_slot": "charm",

  "immune_to_effects": [
    "darkness",
    "wither",
    "blindness",
    "mining_fatigue"
  ],

  "add_effects": {
    "night_vision": 1,
    "speed": 3
  },

  "properties": {
    "max_health": {
      "add": 20.0
    },
    "knockback_resistance": {
      "add": 1.0
    }
  },

  "ticks": {
    "heal": {
      "interval_ticks": 20,
      "add": 1,
      "ratio_add": 0.01
    },
    "feed": {
      "interval_ticks": 160,
      "add": 1
    }
  }
}
```

This relic gives additional health and knockback resistance, grants passive effects, blocks
several negative effects, periodically restores health, and periodically restores hunger.

## Attribute Modifiers

Relics can modify supported attributes using three operations:

```json
"attack_damage": {
  "add": 4.0,
  "mul_base": 0.5,
  "mul_total": 1.25
}
```

- `add` — adds a flat amount.
- `mul_base` — adds a multiplier based on the base attribute value.
- `mul_total` — multiplies the resulting attribute value.

Supported configurable properties include:

```text
attack_damage
attack_speed
max_health
knockback_resistance
block_interaction_range
entity_interaction_range
```

You only need to specify the properties you want to change.

## Passive Effects

A relic can continuously provide status effects:

```json
"add_effects": {
  "night_vision": 1,
  "speed": 3
}
```

The number represents the visible effect level, so `1` means level I and `3` means level III.

## Effect Immunity

Individual effects can be blocked:

```json
"immune_to_effects": [
  "poison",
  "blindness",
  "weakness"
]
```

Relics can also use:

```json
"immune_to_effects": "all_negative"
```

to provide immunity against negative effects.

## Periodic Effects

Relics can perform actions at configurable intervals.

For example:

```json
"ticks": {
  "heal": {
    "interval_ticks": 20,
    "add": 2,
    "ratio_add": 0.01
  }
}
```

This heals every 20 ticks using both a fixed amount and a percentage of maximum health.

Feeding works similarly:

```json
"feed": {
  "interval_ticks": 160,
  "add": 1
}
```

## Damage Rules

Relics can modify incoming and outgoing damage:

```json
"callbacks": {
  "damage_dealt": {
    "modifier": 2.0,
    "minimum": 20,
    "ratio_minimum": 0.1
  },
  "damage_taken": {
    "modifier": 0.1,
    "maximum": 4,
    "ratio_maximum": 0.1
  }
}
```

Available controls include:

```text
modifier
flat
minimum
ratio_minimum
maximum
ratio_maximum
```

This allows relics to range from subtle stat bonuses to deliberately absurd end-game equipment.

## Where Relics Are Effective

`effective_slots` controls where a relic needs to be located before its effects become active.

```json
"effective_slots": [
  "in_curios_api_slots"
]
```

Supported locations include:

```text
in_hotbar
in_inventory
in_curios_api_slots
in_touhou_little_maid_curios_slots
```

`in_inventory` includes the player's hotbar.

If `effective_slots` is omitted or empty, the relic uses the normal equipped-relic behavior:

```text
in_curios_api_slots
in_touhou_little_maid_curios_slots
```

You can combine locations:

```json
"effective_slots": [
  "in_inventory",
  "in_curios_api_slots"
]
```

## Curios Support

Vital Relics integrates with **Curios API**.

The Curios slot can be selected per relic:

```json
"curio_slot": "charm"
```

The relic ID itself does not need to be known by Vital Relics ahead of time. Dynamically configured
relics are resolved at runtime and validated against their configured Curios slot.

## Touhou Little Maid Support

When Touhou Little Maid is installed, compatible relics can also function in maid accessory slots.

Use:

```json
"effective_slots": [
  "in_touhou_little_maid_curios_slots"
]
```

The same relic system is used for players and maids, so attributes, regeneration, effects and other
compatible relic mechanics do not need to separate relic definitions.

Touhou Little Maid remains an optional integration.

## Creating Your Own Relic

The easiest way to create one is to copy an existing entry in `relics.json` and change it:
Notice: Currently, the mod only supports using the bundled textures. You can inspect them under `common/src/main/resources/assets/vitalrelics/textures/item`.

```json
{
  "id": "my_relic",
  "tooltip": "My custom relic.",
  "texture": "my_relic.png",
  "rarity": "rare",
  "curio_slot": "charm",

  "properties": {
    "max_health": {
      "add": 10.0
    }
  }
}
```

Then provide the corresponding texture in the mod's available resource namespace/resource pack.

You do **not** need to modify Vital Relics' source code simply to invent a different relic ID or
combine existing configurable mechanics.

## Design Philosophy

Vital Relics deliberately separates the **relic engine** from the **relic definitions**.

The Java side implements reusable mechanics. `relics.json` describes how those mechanics are
combined.

That means the default relic set is not intended to be the only way to play the mod. Modpack
authors and users can rebalance the defaults or build substantially different relic sets on top of
the same engine.

Want weaker survival-oriented relics? Configure them.

Want powerful RPG equipment? Configure it.

Want completely ridiculous end-game artifacts capable of bullying a Warden?

Well... the configuration system isn't going to stop you.

## License

MIT
