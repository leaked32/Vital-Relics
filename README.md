# Vital Relics

[English](README.md) \| [日本語](docs/ja_jp/README.md) \|
[简体中文](docs/zh_cn/README.md) \| [繁體中文](docs/zh_tw/README.md)

**A highly configurable, data-driven relic mod for Minecraft.**

Vital Relics adds a growing collection of equippable relics, from
practical early-game accessories to powerful late-game artifacts. Relics
can grant attributes, passive abilities, regeneration, defensive
mechanics, combat effects, and active spells.

The mod is designed around a data-driven relic engine: the built-in
relics are the default content, not the limit of the system. Relic
definitions can be modified or extended through configuration without
rewriting the mod's Java code.

## Highlights

-   **44+ built-in relics** spanning utility, combat, defense, mobility,
    and late-game equipment.
-   **Active spell system** with spell selection, cooldowns, targeting,
    sounds, and HUD feedback.
-   **Enchantment utility spells** that can upgrade enchantments, remove
    curses, reset anvil penalties, and transfer enchantments.
-   **Powerful passive abilities** including specialized damage,
    defense, regeneration, projectile interactions, and mobility
    effects.
-   **Relics work on non-player living entities**, allowing naturally
    dangerous relic-bearing enemies and modpack encounters.
-   **Highly configurable content** through external JSON definitions.
-   **Curios integration** for normal accessory-slot gameplay.
-   **Optional Touhou Little Maid integration**.
-   **In-game guide book** for discovering relics and their mechanics.

## Active Spells

Some relics provide active abilities instead of only passive bonuses.
Players can select between available spells and cast them directly
during gameplay.

Current spell mechanics include combat attacks, healing, cleansing,
movement, teleportation, crowd control, defensive effects, and utility
abilities.

### Enchantment Utility

Enchantment-related spells are especially useful for long-term equipment
progression. Depending on the relic and spell, they can:

-   **Upgrade Enchanted Book** --- upgrades an eligible enchantment on
    an enchanted book at an experience cost.
-   **Enchantment Ascension** --- upgrades an eligible enchantment
    directly on enchanted equipment or other enchanted items.
-   **Purify Curse** --- removes a curse from the held item when
    possible.
-   **Purify Penalty** --- resets the held item's accumulated anvil
    repair penalty.
-   **Disenchantment** --- transfers an enchantment from the held item
    to a suitable book in the offhand.

These abilities make enchantment management part of relic progression
rather than limiting it to vanilla enchanting and anvil workflows.

## Data-Driven Relics

Most relic behavior is defined through configuration. Relic definitions
can control features such as:

-   IDs, names, textures, and rarity
-   Curios/equipment locations
-   Attribute modifiers
-   Passive skills
-   Active spells
-   Regeneration and periodic effects
-   Damage and defensive mechanics
-   Acquisition, recipes, and loot behavior

The built-in relic set can therefore be modified, removed, rebalanced,
or extended for custom modpacks.

## Supported Versions

Minecraft   Loader
  ----------- ----------
1.20.1      Forge
1.21.1      NeoForge

## Screenshots

`<img width="1920" height="1104" alt="Vital Relics gameplay screenshot" src="https://github.com/user-attachments/assets/a3e49454-14e9-4b9c-894c-727dfe9d1050" />`{=html}

`<img width="1920" height="1104" alt="Vital Relics gameplay screenshot" src="https://github.com/user-attachments/assets/aabf218f-aa11-4787-9083-5bf32bb71a3e" />`{=html}

`<img width="1920" height="1104" alt="Vital Relics gameplay screenshot" src="https://github.com/user-attachments/assets/c931d595-af92-4843-b82f-299709a0f4a0" />`{=html}

`<img width="1920" height="1104" alt="Vital Relics gameplay screenshot" src="https://github.com/user-attachments/assets/5a2aa56e-6b22-4ba3-b82f-299709a0f4a0" />`{=html}

## For Modpack Authors

Vital Relics is intended to be useful as both a standalone content mod
and a configurable foundation for modpacks. You can rebalance the
default relics, change how they are obtained, or build your own
progression around the relic engine.

Because relic mechanics are shared by players and other living entities,
relics can also be used to create stronger enemies and unusual
encounters instead of functioning only as player equipment.

## Documentation

Detailed configuration and usage documentation is available in
[the documentation index](https://github.com/leaked32/Vital-Relics/blob/main/docs/index.md).

Topics include relic configuration, spells, Curios integration, recipes,
loot, translations, and creating custom relics.

## License

MIT
