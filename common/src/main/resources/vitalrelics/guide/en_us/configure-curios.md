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
- `slowing_aura`: Grants all hostile living entities Slowness 3 in level range.
- `tree_feller`: Level does not matter.
- `area_mining`: Ranged mining with mining context (fortune level, silk touch) preserved.

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
- `borebolt`: Launches an arrow along the visual sight, the arrow destroys the blocks blocked it,
     arrow disappears after the durability drains.
- `compel_attack`: Make the target living entity attack the nearest living entity
     (excluding the caster).

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

- `slowing_aura`: every 20 ticks, applies Slowness III (amplifier 2) for 60 ticks to hostile living entities within `level` blocks. Frostbind Ring: level 10.
- `tree_feller`: positive level enables connected-log felling after a successful log break. Timberheart Ring: level 1. Leaves decay naturally. The search is bounded to 32 blocks and 512 additional logs; touching log structures can also be felled.
- `area_mining`: after a successful break, mines blocks of any type within `level` blocks of that block. Quarry Ring: level 2. Radius is capped at 8, and one operation breaks at most 512 additional blocks. Both mining skills use the actual held tool and player context (Fortune, Silk Touch, tool suitability, durability, XP and protection events). Sneaking disables expansion; changing or breaking the tool stops it. They do not recursively expand their own generated breaks.
- `borebolt`: `speed` (default 2 blocks/tick, maximum 10), `durability` (40), `durability_loss_per_tick` (0.25), `recovery` (0.2). Remaining durability must be strictly greater than block hardness; successful breaks subtract hardness. Unbreakable or protected blocks stop the arrow. Zero tick loss is allowed. Spell arrows cannot be picked up and expire on unload/restart. Normal arrow-hit passives remain active; block breaks can trigger the owner's mining passives using their currently held tool.
- `compel_attack`: `range` (24) selects the pointed living entity; `search_range` (32) finds its nearest other living entity; `recovery` (0.1). Both ranges cap at 256. The target immediately makes an attributed melee strike without an AI, allegiance, or reach check. The caster can be hit. Creatures without an attack-damage attribute deal 1 health; other targets use at least 1 health of attack damage. Normal damage protections/events still apply; this does not rewrite AI goals.
