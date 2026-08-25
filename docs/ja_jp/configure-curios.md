# 遺物の設定

Vital Relics は高度に設定可能なデータ駆動型の遺物システムです。遺物は `relics.json` で定義します。

**警告: 自動生成された JSON を編集する場合は `customized` を `true` にしてください。`false` のままだとファイルが上書きされる可能性があります。**

設定ディレクトリは `config/vitalrelics`. Mod を導入してゲームを起動すると、設定ファイルは自動生成されます。

## 基本情報

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

## Curios の設定

`curio_slot` は Curios スロット種別を指定し、`effective_slots` は遺物効果が有効になる場所を指定します。

```text
in_hotbar
in_inventory
in_curios_api_slots
in_touhou_little_maid_curios_slots
```

`in_inventory` にはホットバーも含まれます。`effective_slots` を省略または空にすると、Curios API と Touhou Little Maid の Curios スロットが既定で使われます。

## 効果への耐性

`immune_to_effects` は指定した効果を無効化します。特殊値 `"all_negative"` はすべての悪性効果を無効化します。

```json
{ "immune_to_effects": ["poison", "blindness"] }
```

## 常時効果

`granted_effects` は継続的に効果を付与し、値は効果レベルです。

```json
{ "granted_effects": { "night_vision": 1, "speed": 2 } }
```

## パッシブ能力

`passive_abilities` は遺物の特殊なパッシブ能力を定義します。

```text
retarget_arrow
flight
reality_severance
metal_mending
```

```json
{ "passive_abilities": { "flight": 1 } }
```

## 属性補正

属性は `properties` で設定します。

```text
attack_damage
attack_speed
block_interaction_range
entity_interaction_range
knockback_resistance
max_health
```

各属性は `add`、`mul_base`、`mul_total` をサポートします。

```json
{ "properties": { "max_health": { "add": 10.0 } } }
```

## 周期処理

周期処理は `ticks` で設定します。`heal` と `feed` を利用でき、`interval_ticks`、`add`、`ratio_add` を指定できます。

```json
{ "ticks": { "heal": { "interval_ticks": 20, "add": 1, "ratio_add": 0.01 } } }
```

## コールバック規則

対応コールバック:

```text
damage_taken
damage_dealt
invulnerable_time_taken
invulnerable_time_dealt
```

コールバック規則では `modifier`、`flat`、`minimum`、`ratio_minimum`、`maximum`、`ratio_maximum` を利用できます。
