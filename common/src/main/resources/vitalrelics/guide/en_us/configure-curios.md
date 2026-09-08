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
- `lava_swimmer`: Reserved passive-skill ID. It currently has no runtime effect.
- `iron_curtain`: super invulnerable time
- `lingering_wound`: Accumulates a portion of damage dealt as a temporary wound that
     reduces the target's effective maximum health,
     preventing healing above the remaining health limit.
     Extra damage can accumulate the wound twice,
     allowing it to receive both the original attack's accumulation
     and an additional accumulation from the extra damage.
- `grave_dominion`: Every half second, moves nearby entities downward by their height.
- `experience_convergence`: Multiplies positive experience gains by
     `1 + level × (experience needed for the next level / 7 - 1)`,
     making experience-level progression approach a linear curve.
- `healing_aura`: Shares each configured `heal` periodic action with allied living
     entities within level blocks. This skill does not produce healing by itself.
- `no_fly_zone`: Every half second, moves hostile living entities within level blocks
     onto the first collidable surface beneath them.
     The radius in blocks equals the skill level.
- `slowing_aura`: Every 20 ticks, applies Slowness III for 60 ticks to hostile
     living entities within `level` blocks.
- `tree_feller`: Any positive level enables bounded, connected-log felling with
     the player's held tool.
- `area_mining`: Mines nearby blocks within `level` blocks while preserving
     the player's held tool and normal mining context.

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
- `grave_shift`: Moves the pointed non-allied living entity beneath the ground within
     `range`. The configured range is limited to 256 blocks.
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
- `open_ender_chest`: Opens the caster's Ender Chest.
- `return_to_bed`: Teleports the caster to a safe standing position beside their
     respawn bed. The spell fails if the bed is missing or obstructed.
- `borebolt`: Fires a non-recoverable arrow that spends durability over time and
     when boring through blocks. Its block drops use an unenchanted Netherite Pickaxe.
- `compel_attack`: Forces the pointed living entity to strike its nearest eligible
     living neighbor immediately. The compelled creature and caster are excluded.

Recovery / Cooldown
```text
cooldown_seconds = 1 / recovery
```

| `recovery` | Cooldown |
|---:|---:|
| `0.25` | 4 s |
| `1` | 1 s |
| `4` | 0.25 s |

## Mining and control relics

- `slowing_aura`: Every 20 ticks, applies Slowness III (amplifier 2) for 60 ticks
  to hostile living entities within `level` blocks. The Frostbind Ring uses level 10,
  so affected targets remain slowed between refreshes.
- `tree_feller`: Any positive level enables the effect. After the player successfully
  breaks a log, connected logs are mined with the same held tool and mining context.
  The search includes diagonal connections, stays within 32 blocks of the first log,
  and breaks at most 512 additional logs. Leaves decay naturally. Sneaking disables
  the effect; changing or breaking the tool stops it. Connected log buildings can
  therefore be felled as well.
- `area_mining`: After a successful block break, mines nearby blocks of any type
  within `level` blocks, nearest first. The radius is capped at 8, and one operation
  breaks at most 512 additional blocks. It preserves the held tool, Fortune,
  Silk Touch, tool suitability, durability loss, experience drops, and block-break
  events. Sneaking disables the effect; changing or breaking the tool stops it.
  Generated breaks do not recursively start another expansion. The Quarry Ring uses
  level 2.
- `borebolt`: Fires an arrow along the caster's sight line. `speed` defaults to
  2 blocks per tick and is capped at 10; `durability` defaults to 40;
  `durability_loss_per_tick` defaults to 0.25 and may be 0; `recovery` defaults
  to 0.2. The arrow can break a block only while its remaining durability is strictly
  greater than that block's hardness, then spends durability equal to the hardness.
  Insufficient durability, an unbreakable block, or a denied interaction destroys
  the arrow. Drops are calculated as if mined with a fresh, unenchanted Netherite
  Pickaxe; the caster's held item and enchantments are never modified. Borebolt arrows
  cannot be picked up, do not survive unloads or restarts, and retain normal arrow-hit
  passive effects.
- `compel_attack`: `range` defaults to 24 and selects the pointed living creature;
  `search_range` defaults to 32 and finds the nearest living creature other than
  the compelled creature and caster; `recovery` defaults to 0.1. Both ranges are
  capped at 256. The compelled creature immediately performs one attributed melee
  strike without waiting for AI goals, allegiance checks, or normal reach. Creatures
  without an attack-damage attribute deal 1 damage; all others deal at least 1.
  Normal damage protections and events still apply, and AI goals are not rewritten.
