# 配置遗物

Vital Relics 是一个高度可配置、数据驱动的遗物系统。遗物通过 `relics.json` 定义。

**警告：编辑自动生成的 JSON 后，请将 `customized` 设置为 `true`。保持为 `false` 时文件可能被覆盖。**

配置目录为 `config/vitalrelics`. 安装 Mod 后启动游戏，配置文件会自动生成。

## 基本信息

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

## Curios 配置

`curio_slot` 选择 Curios 槽位类型；`effective_slots` 控制遗物效果在哪些位置生效。

```text
in_hotbar
in_inventory
in_curios_api_slots
in_touhou_little_maid_curios_slots
```

`in_inventory` 包含快捷栏。省略或留空 `effective_slots` 时，默认使用 Curios API 与 Touhou Little Maid Curios 槽位。

## 效果免疫

`immune_to_effects` 阻止指定效果；特殊值 `"all_negative"` 阻止全部负面效果。

```json
{ "immune_to_effects": ["poison", "blindness"] }
```

## 持续效果

`granted_effects` 持续赋予效果，数值表示效果等级。

```json
{ "granted_effects": { "night_vision": 1, "speed": 2 } }
```

## 被动能力

`passive_abilities` 定义遗物的特殊被动能力。

```text
retarget_arrow
flight
reality_severance
metal_mending
```

```json
{ "passive_abilities": { "flight": 1 } }
```

## 属性修改

属性在 `properties` 下配置。

```text
attack_damage
attack_speed
block_interaction_range
entity_interaction_range
knockback_resistance
max_health
```

每个属性支持 `add`、`mul_base` 与 `mul_total`。

```json
{ "properties": { "max_health": { "add": 10.0 } } }
```

## 周期行为

周期行为在 `ticks` 下配置。支持 `heal` 与 `feed`，并可使用 `interval_ticks`、`add`、`ratio_add`。

```json
{ "ticks": { "heal": { "interval_ticks": 20, "add": 1, "ratio_add": 0.01 } } }
```

## 回调规则

支持的回调：

```text
damage_taken
damage_dealt
invulnerable_time_taken
invulnerable_time_dealt
```

回调规则支持 `modifier`、`flat`、`minimum`、`ratio_minimum`、`maximum` 与 `ratio_maximum`。
