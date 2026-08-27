# Configuring Relics

Vital Relics is a highly configurable, data-driven relic system. Relics are defined in `relics.json`.

**WARNING: after editing an automatically generated JSON file, set `customized` to `true`. While it is `false`, the file may be overwritten.**

The configuration directory is `config/vitalrelics`. Files are generated automatically after the game is launched with the mod installed.

## Basic Information

- `id`: unique relic identifier; new IDs may be created through configuration.
- `display_name`: optional custom display name.
- `tooltip`: relic tooltip.
- `texture`: bundled relic texture filename.
- `rarity`: relic rarity.

```json
{
  "id": "iron_heart",
  "display_name": "Super Iron Heart",
  "tooltip": "A small heart forged from stubborn iron.",
  "texture": "iron_heart.png",
  "rarity": "rare"
}
```

## Curios Configuration

`curio_slot` selects the Curios slot type. `effective_slots` controls where relic effects are active.

- `in_hotbar` --- relic is effective while inside the player's hotbar.
- `in_inventory` --- relic is effective while inside the player's inventory. This includes the hotbar.
- `in_curios_api_slots` --- relic is effective while equipped through Curios API.
- `in_touhou_little_maid_curios_slots` --- relic is effective while equipped through Touhou Little Maid accessory slots.

`in_inventory` includes the hotbar. If `effective_slots` is omitted or empty, Curios API slots and Touhou Little Maid Curios slots are used by default.

## Effect Immunity

`immune_to_effects` blocks specified potion effects. The special value `"all_negative"` blocks all negative effects.

```json
{ "immune_to_effects": ["poison", "blindness"] }
```

## Passive Effects

`granted_effects` continuously provides potion effects; each value is the effect level.

```json
{ "granted_effects": { "night_vision": 1, "speed": 2 } }
```

## Passive Skills

`passive_skills` defines special passive relic behavior.

Passive skills:
- `retarget_arrow`: Reflected arrow minimum damage = ATTACK_DAMAGE × level.
- `arrow_deflection`: Reflects one incoming arrow. Reflected arrow damage and speed
     are multiplied by level. Cooldown is `5 / level` seconds.
- `reality_severance`: level% attack-damage contribution, level-block radius,
     roughly level/4 debuff strength.
- `metal_mending`: Repairs up to level durability every 4 seconds.
- `flight`: Any level > 0 grants flight; flight speed = vanilla flight speed × level.
- `empowered_arrows`: Multiplies arrow charge, velocity, and base damage by level.
- `lifesteal`: Heals the bearer for damage dealt × level.
- `thorns`: Reflects received damage × level; reflection is limited by a cooldown.

```json
{ "passive_skills": { "arrow_deflection": 1.0 } }
```

## Attribute Modifiers

Attributes are configured under `properties`.

```text
attack_damage
attack_speed
block_interaction_range
entity_interaction_range
knockback_resistance
max_health
```

Each supports `add`, `mul_base`, and `mul_total`.

```json
{ "properties": { "max_health": { "add": 10.0 } } }
```

## Periodic Actions

Periodic actions are configured under `ticks`. Supported actions include `heal` and `feed`; entries support `interval_ticks`, `add`, and `ratio_add`.

```json
{ "ticks": { "heal": { "interval_ticks": 20, "add": 1, "ratio_add": 0.01 } } }
```

## Callback Rules

Supported callbacks:

```text
damage_taken
damage_dealt
invulnerable_time_taken
invulnerable_time_dealt
```

Callback rules support `modifier`, `flat`, `minimum`, `ratio_minimum`, `maximum`, and `ratio_maximum`.
