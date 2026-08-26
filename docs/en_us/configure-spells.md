# Configure Spells

Vital Relics includes a data-driven spell system. Spells are configured in a relic's `available_spells` object in `relics.json`.

```json
{
  "id": "example_relic",
  "available_spells": {
    "teleport": { "range": 64, "recovery": 2 }
  }
}
```

A relic may provide multiple spells. Spell selection and cooldowns are tracked per living entity.

## Recovery / Cooldown

```text
cooldown_seconds = 1 / recovery
```

| `recovery` | Cooldown |
|---:|---:|
| `0.25` | 4 s |
| `0.5` | 2 s |
| `1` | 1 s |
| `2` | 0.5 s |
| `4` | 0.25 s |

Cooldown is applied only after successful activation. Failed casts do not consume cooldown.

## Duplicate Spells

Multiple equipped relics may provide the same spell. `priority` selects between duplicates; without it the default score is `intensity * recovery`.

# Available Spells

## Teleport

Spell ID: `teleport`

Teleport moves the caster toward the location they are looking at.

```json
"teleport": {
  "range": 128,
  "recovery": 2
}
```

Parameters: `range`, `recovery`, optional `priority`. `range` is limited to 256 blocks.

Open-space targeting searches as far along the look direction as possible. Block targeting finds an appropriate destination around the hit block and respects collision shapes. Thin blocks such as doors, trapdoors, iron bars and glass panes use their block cell. Snow layers are ignored for targeting. If no valid destination exists, the cast fails without cooldown.

## Curse

Spell ID: `curse`

Curse attacks the LivingEntity directly targeted by the caster.

```json
"curse": {
  "intensity": 100,
  "range": 128,
  "recovery": 0.5
}
```

Parameters: `intensity`, `range`, `recovery`, optional `priority`.

```text
damage = caster attack damage * intensity / 100
```

Curse fails without cooldown when there is no valid living target, the target is out of range or allied, or `intensity` / `range` is not positive.

# Entity Support

The core spell system operates on `LivingEntity`, not only players. Compatible entities and custom mobs can therefore reuse the same relic and spell architecture.

# Adding Spells

Add `available_spells`, add the desired spell ID, then configure its parameters. Assigning an existing spell to another relic requires no Java changes.
