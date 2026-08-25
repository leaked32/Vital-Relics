# 設定法術

Vital Relics 包含資料驅動的法術系統。法術在 `relics.json` 中每件遺物的 `available_spells` 物件內設定。

```json
{
  "id": "example_relic",
  "available_spells": {
    "teleport": { "range": 64, "recovery": 2 }
  }
}
```

一件遺物可以提供多個法術。選擇狀態與冷卻按 LivingEntity 分別記錄。

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

只有成功啟動後才進入冷卻；失敗施法不消耗冷卻。

## Duplicate Spells

多個已裝備遺物可以提供同一個法術。重複時使用 `priority` 選擇；省略時預設評分為 `intensity * recovery`。

# 可用法術

## Teleport

Spell ID: `teleport`

Teleport 將施法者向其視線方向傳送。

```json
"teleport": {
  "range": 128,
  "recovery": 2
}
```

參數：`range`、`recovery`、可選 `priority`。`range` 上限為 256 方塊。

指向空中時沿視線尋找盡可能遠的有效位置；指向方塊時根據碰撞形狀尋找周圍合適位置。門、地板門、鐵柵欄和玻璃片等薄方塊使用其方塊單元；雪層在目標判定中被忽略。沒有有效位置時施法失敗且不進入冷卻。

## Curse

Spell ID: `curse`

Curse 攻擊施法者直接瞄準的 LivingEntity。

```json
"curse": {
  "intensity": 100,
  "range": 128,
  "recovery": 2
}
```

參數：`intensity`、`range`、`recovery`、可選 `priority`。

```text
damage = caster attack damage * intensity / 100
```

沒有有效 LivingEntity、目標超出範圍或為友方、或 `intensity` / `range` 非正數時，Curse 失敗且不進入冷卻。

# 實體支援

核心法術系統基於 `LivingEntity`，而非僅限玩家，因此相容實體與自訂生物可以重複使用同一套遺物和法術架構。

# 加入法術

加入 `available_spells`、所需法術 ID，再設定對應參數。 把既有法術分配給其他遺物不需要修改 Java。
