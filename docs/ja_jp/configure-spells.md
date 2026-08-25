# スペルの設定

Vital Relics にはデータ駆動型スペルシステムがあります。スペルは `relics.json` の各遺物にある `available_spells` で設定します。

```json
{
  "id": "example_relic",
  "available_spells": {
    "teleport": { "range": 64, "recovery": 2 }
  }
}
```

1つの遺物は複数スペルを提供できます。選択状態とクールダウンは LivingEntity ごとに管理されます。

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

クールダウンは成功時だけ適用され、失敗した詠唱では消費されません。

## Duplicate Spells

複数の装備中遺物が同じスペルを提供できます。重複時は `priority` で選択し、省略時の既定スコアは `intensity * recovery` です。

# 利用可能なスペル

## Teleport

Spell ID: `teleport`

Teleport は視線方向へ術者をテレポートさせます。

```json
"teleport": {
  "range": 128,
  "recovery": 2
}
```

パラメータは `range`、`recovery`、任意の `priority`。`range` の上限は256ブロックです。

空間を狙うと視線方向のできるだけ遠い有効位置を探します。ブロックを狙うと衝突形状を考慮して周囲の適切な位置を探します。ドア、トラップドア、鉄格子、ガラス板などの薄いブロックはセル中央を使い、雪層はターゲット判定で無視します。有効位置がなければクールダウンなしで失敗します。

## Curse

Spell ID: `curse`

Curse は術者が直接狙っている LivingEntity を攻撃します。

```json
"curse": {
  "intensity": 100,
  "range": 128,
  "recovery": 2
}
```

パラメータは `intensity`、`range`、`recovery`、任意の `priority`。

```text
damage = caster attack damage * intensity / 100
```

有効な LivingEntity がいない、範囲外、味方、または `intensity` / `range` が正でない場合、Curse はクールダウンなしで失敗します。

# エンティティ対応

コアのスペルシステムはプレイヤー専用ではなく `LivingEntity` を対象にします。そのため互換エンティティやカスタム Mob も同じ遺物・スペル構造を再利用できます。

# スペルの追加

`available_spells` を追加し、スペル ID と対応パラメータを設定します。 既存スペルを別の遺物へ割り当てるだけなら Java の変更は不要です。
