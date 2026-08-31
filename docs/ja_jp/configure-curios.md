# レリックの設定

Vital Relicsは、設定変更の自由度が高く、データ駆動型のレリックシステムです。レリックは `relics.json` で定義されます。

**警告: 自動生成されたJSONファイルを編集した後は、`customized` を `true` に設定してください。`false` のままだと、ファイルが上書きされる可能性があります。**

設定ディレクトリは `config/vitalrelics` です。このMODを導入してゲームを起動すると、ファイルが自動的に生成されます。

## 基本情報

- `id`: レリック固有の識別子。設定によって新しいIDを作成できます。
- `display_name`: 任意の表示名。
- `tooltip`: レリックのツールチップ（代替用）。
- `texture`
  Vital Relicsはまず、外部ディレクトリ `config/vitalrelics/textures` 内でテクスチャを探します。
  ファイルが存在しない、または読み込めない場合は、
  同じファイル名を持つ同梱のテクスチャが自動的に使用されます。
  どちらのテクスチャも存在しない場合、Minecraftの「テクスチャ未検出」用プレースホルダーが表示されます。
  例:
  `{ "id": "iron_heart", "texture": "my_iron_heart.png" }`
  画像は `config/vitalrelics/textures/my_iron_heart.png` に配置してください。
  サブディレクトリも使用可能です。
- `rarity`: レリックのレアリティ。`common`、`uncommon`、`rare`、`epic` が指定可能です。

例
```json
{
"id": "iron_heart",
"display_name": "Super Iron Heart",
"tooltip": "A small heart forged from stubborn iron.",
"texture": "iron_heart.png",
"rarity": "rare"
}
```

## Curiosの設定

`curio_slot` はCuriosのスロットタイプを選択します。`effective_slots` はレリックの効果が有効になる場所を制御します。

- `in_hotbar` --- プレイヤーのホットバー内にある間、レリックの効果が有効になります。
- `in_inventory` --- プレイヤーのインベントリ内にある間、レリックの効果が有効になります（ホットバーも含まれます）。
- `in_curios_api_slots` --- Curios APIを通じて装備されている間、レリックの効果が有効になります。 - `in_touhou_little_maid_curios_slots` --- Touhou Little Maidのアクセサリースロットに装備している間、レリックの効果が有効になります。

`in_inventory`にはホットバーも含まれます。`effective_slots`が省略されているか空の場合、デフォルトでCurios APIのスロットとTouhou Little MaidのCuriosスロットが使用されます。

## 効果の無効化

`immune_to_effects`は、指定されたポーション効果を無効化します。特殊な値である`"all_negative"`は、すべてのマイナス効果を無効化します。

```json
{ "immune_to_effects": ["poison", "blindness"] }
```

## 付与される効果

`granted_effects`はポーション効果を継続的に付与します。各値は効果のレベルを表します。

```json
{ "granted_effects": { "night_vision": 1, "speed": 2 } }
```

## パッシブスキル

`passive_skills`は、レリックの特別なパッシブ動作を定義します。

- パッシブスキルは条件を満たすと自動的に発動します。
- パッシブスキルのレベルは累積や合算はされず、最も高いレベルのみが適用されます。

利用可能なパッシブスキル:
- `retarget_arrow`: 跳ね返した矢の最小ダメージ = 攻撃力 × レベル
- `arrow_deflection`: 飛来する矢を1本跳ね返します。跳ね返した矢のダメージと速度はレベル倍され、クールダウンは 5 / レベル 秒となります。
- `reality_severance`: 攻撃力のレベル%分をダメージに加算、レベルに等しい半径の範囲攻撃、およそレベル/4のデバフ強度。
- `metal_mending`: 4秒ごとに最大レベル分の耐久値を修復します。
- `flight`: レベルが0より大きい場合、飛行能力を付与します。飛行速度 = バニラの飛行速度 × レベル（レベルが1.0の場合は速度変化なし）。
- `empowered_arrows`: 矢のチャージ、速度、基本ダメージをレベル倍します。
- `lifesteal`: 与えたダメージ × レベル分だけ、所持者の体力を回復します。
- `thorns`: 受けたダメージ × レベル分を反射します。反射にはクールダウンが適用されます。
- `fire_resistance`: 炎を消火します。
- `iron_curtain`: 超無敵時間を付与します。
- `lingering_wound`: 与えたダメージの一部を一時的な「傷」として蓄積します。この傷は対象の実質的な最大体力を減少させ、残りの体力上限を超える回復を妨げます。追加ダメージによって「傷（wound）」の蓄積値を2回分加算できます。
  これにより、元の攻撃による蓄積と、追加ダメージによる蓄積の両方が適用されます。

例
```json
{ "passive_skills": { "arrow_deflection": 1.0 } }
```

## プロパティ (Properties)

属性は `properties` の下に設定します。

特徴
- 同じレリックを異なるスロットに装備することで効果を重複（スタック）させることができます。
- 同じスロットに重複して装備してもカウントされません。

各マップ設定は意図的に拡張可能な構造になっています。新しい設定項目を追加する際、
このクラスへのフィールド追加やパーサー（解析処理）の分岐追加は不要です。
- `attack_damage`
- `attack_speed`
- `block_interaction_range`
- `entity_interaction_range`
- `knockback_resistance`
- `max_health`
- `armor`
- `armor_toughness`

それぞれ `add`（加算）、`mul_base`（基本値への乗算）、`mul_total`（合計値への乗算）をサポートしています。

例
```json
{ "properties": { "max_health": { "add": 10.0 } } }
```

## 定期的なアクション (Periodic Actions)

定期的なアクションは `ticks` の下に設定します。
サポートされているアクションには `heal`（回復）や `feed`（給餌/空腹度回復）があり、各項目は `interval_ticks`、`add`、`ratio_add` をサポートしています。

```json
{ "ticks": { "heal": { "interval_ticks": 20, "add": 1, "ratio_add": 0.01 } } }
```

## コールバックルール (Callback Rules)

特徴
- 同じレリックを異なるスロットに装備することで効果を重複（スタック）させることができます。
- 同じスロットに重複して装備してもカウントされません。

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

コールバックのルールでは、`modifier`、`flat`、`minimum`、`ratio_minimum`、`maximum`、`ratio_maximum` がサポートされています。


## 呪文 (Spells)

特徴
- 各呪文は特定のレリック（遺物）専用であるため、競合などを気にする必要はありません。

利用可能な呪文
- `teleport`:
  ブロックに当たった場合: 薄いブロックならその中心へ移動を試みる。まず上側を試し、ブロックされる場合は衝突面の手前を試す。
  MISS / 空中 -> 視線方向に可能な限り遠くまでテレポートする。
  パラメータ: `range`（範囲）、`recovery`（回復時間）、オプションの `priority`（優先度）。`range` は最大256ブロックに制限されます。
- `curse`: 指定した生物（living entity）に対して `directAttack` を実行する。
- `heal`: `amount`（固定値）の体力を回復し、さらに術者の最大体力の `ratio`（割合）分を回復する。
- `healing_ray`: `range` 内の指定した生物を回復する。
  回復量は術者の攻撃ダメージの `intensity` 倍。
- `cleanse`: 術者からすべてのマイナス効果（デバフ）を除去する。
- `dash`: 術者を前方に `strength`（強さ）の勢いで移動させる。オプションで `vertical`（垂直方向）の速度も指定可能。
- `arc_burst`: `range` 内の敵対的ターゲットに繰り返しダメージを与える。
  各ヒットは攻撃ダメージの `intensity`% を与え、`count` 回繰り返される。
  オプションで `weaken`（弱体化）デバフの強度を指定可能。
- `repulse`: `range` 内の敵対的な生物を術者から遠ざけるように押し出す。
  `strength` を使用し、オプションで `vertical`（垂直方向）への浮き上がりも指定可能。
- `absorption`: 設定された `amplifier`（強度）で、`duration_ticks`（持続時間）の間「吸収 (Absorption)」効果を付与する。
- `sky_launch`: `range` 内の敵対的な生物を `strength` の勢いで上空へ打ち上げる。
- `shadow_exchange`: `range` 内の指定した敵対的な生物と位置を入れ替える。
- `phantom_step`: 前方に最大 `range` ブロック分瞬時に移動し、
  通過した敵対的な生物に攻撃ダメージの `intensity`% のダメージを与える。
- `upgrade_enchanted_book`: メインハンドに持っているエンチャント本のうち、
  最大レベルに達していない最初のエンチャントを1レベル強化する。
  その際、`experience_cost` 分の経験値レベルを消費する。クリエイティブモードのプレイヤーはコストを支払いません。
- `enchantment_ascension`: メインハンドに持っているエンチャント付きアイテムについて、
  最大レベルに達していない最初のエンチャントを1レベル強化します。
  その際、`experience_cost`分の経験値レベルを消費します。クリエイティブモードのプレイヤーはコストを支払いません。
- `purify_curse`: メインハンドに持っているアイテムから、最初の「呪い」を取り除きます。
- `purify_penalty`: 代わりに、金床での修理コストペナルティをゼロにリセットします。
  `experience_cost`分の経験値レベルを消費します。クリエイティブモードのプレイヤーはコストを支払いません。
- `disenchantment`: メインハンドに持っているアイテムから最初のエンチャントを取り除き、
  そのエンチャントを同じレベルのまま、オフハンドに持っている本に移します。

回復 / クールダウン
```text
cooldown_seconds = 1 / recovery
```

| `recovery` | クールダウン |
|---:|---:|
| `0.25` | 4秒 |
| `1` | 1秒 |
| `4` | 0.25秒 |