# Configuring Relics

Vital Relics is a highly configurable, data-driven relic system. Relics are defined in `relics.json`.

**WARNING: after editing an automatically generated JSON file, set `customized` to `true`. While it is `false`, the file may be overwritten.**

The configuration directory is `config/vitalrelics`. Files are generated automatically after the game is launched with the mod installed.

## Basic Information

- `id`: unique relic identifier; new IDs may be created through configuration.
- `display_name`: optional custom display name.
- `tooltip`: fallback relic tooltip.
- `texture`
    Vital Relics first looks for the texture in the external directory `config/vitalrelics/textures`.
    If the file is missing or cannot be loaded,
    it automatically uses the bundled texture with the same filename.
    If neither texture exists, Minecraft displays its missing-texture placeholder.
    For example,
    `{ "id": "iron_heart", "texture": "my_iron_heart.png" }`
    Place the image at `config/vitalrelics/textures/my_iron_heart.png`.
    Subdirectories are supported.
- `rarity`: relic rarity, supports `common`, `uncommon`, `rare`, `epic`.

Example
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

## Granted Effects

`granted_effects` continuously provides potion effects; each value is the effect level.

```json
{ "granted_effects": { "night_vision": 1, "speed": 2 } }
```

## Passive Skills

`passive_skills` defines special passive relic behavior.

- Passive skills activates automatically on condition.
- Passive skill level cannot be stacked or summed, only the highest level counts.

Available Passive skills:
- `retarget_arrow`: Reflected arrow minimum damage = ATTACK_DAMAGE × level
- `arrow_deflection`: Reflects one incoming arrow; reflected damage and speed are
     multiplied by level, and cooldown is 5 / level seconds
- `reality_severance`: level% attack-damage contribution, level-block radius,
     roughly level/4 debuff strength
- `metal_mending`: Repairs up to level durability every 4 seconds
- `flight`: Any level > 0 grants flight; flight speed = vanilla flight speed × level,
     does not change the speed if the level is 1.0
- `empowered_arrows`: Multiplies arrow charge, velocity, and base damage by level
- `lifesteal`: Heals the bearer for damage dealt × level
- `thorns`: Reflects received damage × level; reflection is limited by a cooldown
- `fire_resistance`: Extinguish fire.
- `iron_curtain`: super invulnerable time
- `lingering_wound`: Accumulates a portion of damage dealt as a temporary wound that
     reduces the target's effective maximum health,
     preventing healing above the remaining health limit.
     Extra damage can accumulate the wound twice,
     allowing it to receive both the original attack's accumulation
     and an additional accumulation from the extra damage.

Example
```json
{ "passive_skills": { "arrow_deflection": 1.0 } }
```

## Properties

Attributes are configured under `properties`.

Features
- can be stacked by equipped the same relic into different slots.
- stacking them in the same slot does not count.

Each map is intentionally open-ended. Adding a new configuration entry
no longer requires adding a field to this class or a parser branch.
- `attack_damage`
- `attack_speed`
- `block_interaction_range`
- `entity_interaction_range`
- `knockback_resistance`
- `max_health`
- `armor`
- `armor_toughness`

Each supports `add`, `mul_base`, and `mul_total`.

Example
```json
{ "properties": { "max_health": { "add": 10.0 } } }
```

## Periodic Actions

Periodic actions are configured under `ticks`.
Supported actions include `heal` and `feed`; entries support `interval_ticks`, `add`, and `ratio_add`.

```json
{ "ticks": { "heal": { "interval_ticks": 20, "add": 1, "ratio_add": 0.01 } } }
```

## Callback Rules

Features
- can be stacked by equipped the same relic into different slots.
- stacking them in the same slot does not count.

- `damage_dealt`
- `damage_taken`
- `invulnerable_time_taken`
- `invulnerable_time_dealt`


```text
damage_taken
damage_dealt
invulnerable_time_taken
invulnerable_time_dealt
```

Callback rules support `modifier`, `flat`, `minimum`, `ratio_minimum`, `maximum`, and `ratio_maximum`.


## Spells

Features
- Each spell is unique for its unique relic, so there's nothing to worry about it.

Available Spells
- `teleport`:
    BLOCK hit: center for thin blocks; try above, if blocked, try before the hit face
    MISS / sky -> teleport as far along look direction as possible
    Parameters: `range`, `recovery`, optional `priority`. `range` is limited to 256 blocks.
- `curse`: Calls `directAttack` with the pointed living entity
- `heal`: Restores `amount` health plus `ratio` of the caster's maximum health
- `healing_ray`: Heals the pointed living entity within `range`
     for `intensity` times the caster's attack damage
- `cleanse`: Removes all negative effects from the caster
- `dash`: Launches the caster forward by `strength`, with optional `vertical` velocity
- `arc_burst`: Repeatedly damages hostile targets within `range`;
     each hit deals `intensity`% attack damage, repeated `count` times,
     with optional `weaken` debuff strength
- `repulse`: Pushes hostile living entities within `range` away from the caster
     using `strength`, with optional `vertical` lift
- `absorption`: Grants Absorption for `duration_ticks` with the configured `amplifier`
- `sky_launch`: Launches hostile living entities within `range` upward by `strength`
- `shadow_exchange`: Swaps positions with the pointed hostile living entity within `range`
- `phantom_step`: Instantly moves forward up to `range` blocks and damages hostile
     living entities crossed for `intensity`% attack damage
- `upgrade_enchanted_book`: Upgrades the first non-max-level enchantment
     on the enchanted book held in the main hand by one level,
     consuming experience_cost experience levels. Creative players do not pay the cost.
- `enchantment_ascension`: Upgrades the first non-max-level enchantment
     on any enchanted item held in the main hand by one level,
     consuming experience_cost experience levels. Creative players do not pay the cost.
- `purify_curse`: Removes the first curse from the item held in the main hand;
- `purify_penalty`: Resets its anvil repair-cost penalty to zero instead.
     Consumes experience_cost experience levels; creative players do not pay the cost.
- `disenchantment`: Removes the first enchantment from the item held in the
     main hand and transfers it at the same level to a book held in the off hand.

Recovery / Cooldown
```text
cooldown_seconds = 1 / recovery
```

| `recovery` | Cooldown |
|---:|---:|
| `0.25` | 4 s |
| `1` | 1 s |
| `4` | 0.25 s |
