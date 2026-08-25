# 設定遺物

Vital Relics 是一個高度可設定、資料驅動的遺物系統。遺物透過 `relics.json` 定義。

**警告：編輯自動產生的 JSON 後，請將 `customized` 設為 `true`。保持為 `false` 時檔案可能被覆寫。**

設定目錄為 `config/vitalrelics`. 安裝 Mod 後啟動遊戲，設定檔會自動產生。

## 基本資訊

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

## Curios 設定

`curio_slot` 選擇 Curios 欄位類型；`effective_slots` 控制遺物效果在哪些位置生效。

```text
in_hotbar
in_inventory
in_curios_api_slots
in_touhou_little_maid_curios_slots
```

`in_inventory` 包含快捷欄。省略或留空 `effective_slots` 時，預設使用 Curios API 與 Touhou Little Maid Curios 欄位。

## 效果免疫

`immune_to_effects` 阻止指定效果；特殊值 `"all_negative"` 阻止全部負面效果。

```json
{ "immune_to_effects": ["poison", "blindness"] }
```

## 持續效果

`granted_effects` 持續賦予效果，數值表示效果等級。

```json
{ "granted_effects": { "night_vision": 1, "speed": 2 } }
```

## 被動能力

`passive_abilities` 定義遺物的特殊被動能力。

```text
retarget_arrow
flight
reality_severance
metal_mending
```

```json
{ "passive_abilities": { "flight": 1 } }
```

## 屬性修改

屬性在 `properties` 下設定。

```text
attack_damage
attack_speed
block_interaction_range
entity_interaction_range
knockback_resistance
max_health
```

每個屬性支援 `add`、`mul_base` 與 `mul_total`。

```json
{ "properties": { "max_health": { "add": 10.0 } } }
```

## 週期行為

週期行為在 `ticks` 下設定。支援 `heal` 與 `feed`，並可使用 `interval_ticks`、`add`、`ratio_add`。

```json
{ "ticks": { "heal": { "interval_ticks": 20, "add": 1, "ratio_add": 0.01 } } }
```

## 回呼規則

支援的回呼：

```text
damage_taken
damage_dealt
invulnerable_time_taken
invulnerable_time_dealt
```

回呼規則支援 `modifier`、`flat`、`minimum`、`ratio_minimum`、`maximum` 與 `ratio_maximum`。
