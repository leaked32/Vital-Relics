# Vital Relics

A lightweight, data-driven relic system for Minecraft modpacks.

**Vital Relics is designed primarily for modpack makers.** Relics can be defined through JSON with
custom attributes, regeneration, hunger restoration, damage processing, effect immunities, and
special abilities—without creating a new Java item class for every relic.

Players can also use the built-in relics directly, so the mod works perfectly well on its own.

## Features

- Data-driven relic definitions
- Dynamically registered relic items
- Custom textures and rarities
- Attribute modifiers
    - Attack damage
    - Attack speed
    - Maximum health
    - Knockback resistance
    - Block interaction range
    - Entity interaction range
- Periodic effects
    - Healing
    - Feeding
- Damage callbacks
    - Modify damage dealt
    - Modify damage taken
    - Modify invulnerability time
- Status-effect immunity
- Special abilities for mechanics that go beyond ordinary numerical modifiers
- Built-in relics ready for normal gameplay

## For Modpack Makers

Relics are described in JSON rather than hard-coded individually.

A relic can look like this:

```json
{
  "id": "cherry_cross",
  "tooltip": "The pure power of cherry trees.",
  "texture": "cherry_cross.png",
  "rarity": "rare",

  "immune_to_effects": [
    "darkness",
    "wither",
    "blindness",
    "mining_fatigue"
  ],

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
      "interval_ticks": 10,
      "add": 1,
      "ratio_add": 0.01
    }
  }
}
```

This makes Vital Relics suitable for progression-heavy modpacks where relics need to be balanced
around the rest of the pack rather than around one fixed set of built-in values.

## Effect Model

Vital Relics separates effects into a few reusable systems.

### Properties

Persistent player attributes:

```json
"attack_damage": {
  "add": 9.0,
  "mul_base": 2.0,
  "mul_total": 2.0
}
```

### Ticks

Effects executed periodically:

```json
"heal": {
  "interval_ticks": 20,
  "add": 2.0,
  "ratio_add": 0.01
}
```

### Callbacks

Transform values produced by gameplay events:

```json
"damage_taken": {
  "modifier": 0.5,
  "maximum": 10.0,
  "ratio_maximum": 0.1
}
```

### Special Abilities

Some mechanics do not naturally fit into numerical properties.

```json
"special_abilities": [
  "retarget_arrow"
]
```

These provide dedicated behavior for unusual relics while keeping ordinary relic configuration
simple.

## Built-in Relics

Vital Relics includes built-in items for players who simply want to install the mod and play.

Current relics include:

- **Cherry Cross** — defensive and restorative relic with increased health and status-effect immunity.
- **Cursed Torturing** — offensive relic focused on increased damage.
- **Reality Piercing** — an extremely powerful endgame relic demonstrating advanced mechanics and
  special abilities.

## Project Goals

Vital Relics aims to remain:

- **Modpack-friendly** — configuration should be more important than Java code.
- **Extensible** — common mechanics should be reusable across many relics.
- **Lightweight** — avoid requiring a huge framework for a small relic system.
- **Playable standalone** — built-in relics should still make the mod enjoyable without customization.

## Requirements

- Minecraft 1.21.1
- NeoForge
- Java 21

## Status

Vital Relics is under active development.

The configuration format and available effects may change while the project is young.

## License

See `LICENSE.txt` for details.

