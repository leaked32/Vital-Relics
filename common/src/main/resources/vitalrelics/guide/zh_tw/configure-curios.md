# 設定遺物

Vital Relics 是一個高度可設定、基於數據的遺物系統。遺物在 `relics.json` 檔案中定義。

**警告：在編輯自動產生的 JSON 檔案後，請將 `customized` 設為 `true`。若該項為 `false`，檔案可能會被覆寫。 **

設定檔目錄為 `config/vitalrelics`。安裝模組並啟動遊戲後，系統會自動產生相關檔案。

## 基礎訊息

- `id`：遺物的唯一識別碼；可透過設定建立新的 ID。
- `display_name`：可選的自訂顯示名稱。
- `tooltip`：備用遺物提示文字（Tooltip）。
- `texture`（紋理）
  Vital Relics 會優先在外部目錄 `config/vitalrelics/textures` 中尋找紋理。
  如果文件缺失或無法加載，
  系統會自動使用同名的內建紋理。
  如果兩者都不存在，Minecraft 將顯示「紋理缺失」的佔位符。
  例如：
  `{ "id": "iron_heart", "texture": "my_iron_heart.png" }`
  請將圖片放置在 `config/vitalrelics/textures/my_iron_heart.png`。
  支援使用子目錄。
- `rarity`：遺物稀有度，支援 `common`（普通）、`uncommon`（罕見）、`rare`（稀有）、`epic`（史詩）。

範例
```json
{
"id": "iron_heart",
"display_name": "超級鐵心",
"tooltip": "一顆由堅硬鐵塊鍛造而成的小心臟。",
"texture": "iron_heart.png",
"rarity": "rare"
}
```

## Curios 配置

`curio_slot` 用於選擇 Curios 插槽類型。 `effective_slots` 用來控制遺物效果生效的位置。

- `in_hotbar` --- 遺物位於玩家快速列（hotbar）時生效。
- `in_inventory` --- 遺物位於玩家背包（inventory）時生效。這包括快捷欄。
- `in_curios_api_slots` --- 遺物透過 Curios API 裝備時生效。 - `in_touhou_little_maid_curios_slots` --- 當裝備於「東方小女僕（Touhou Little Maid）」的飾品槽位時，該聖物生效。

`in_inventory` 包含快捷欄。若省略 `effective_slots` 或其為空，則預設使用 Curios API 槽位及「東方小女僕」的 Curios 飾品槽位。

## 效果免疫

`immune_to_effects` 用於阻擋指定的藥水效果。特殊值 `"all_negative"` 可阻擋所有負面效果。

```json
{ "immune_to_effects": ["poison", "blindness"] }
```

## 賦予效果

`granted_effects` 持續提供藥水效果；每個數值代表效果等級。

```json
{ "granted_effects": { "night_vision": 1, "speed": 2 } }
```

## 被動技能

`passive_skills` 定義了聖物的特殊被動行為。

- 被動技能在滿足條件時自動啟動。
- 被動技能等級不可疊加或相加，僅最高等級生效。

可用被動技能：
- `retarget_arrow`：反彈箭矢的最小傷害 = 攻擊傷害 × 等級
- `arrow_deflection`：反彈一枚來襲箭矢；反彈傷害與速度乘以等級，冷卻時間為 5 / 等級 秒
- `reality_severance`：等級% 的攻擊傷害貢獻，等級數值的格擋半徑，
  約為 等級/4 的減益效果強度
- `metal_mending`：每 4 秒修復最多等於等級數值的耐久度
- `flight`：等級 > 0 即可飛行；飛行速度 = 原版飛行速度 × 等級，
  若等級為 1.0 則不改變速度
- `empowered_arrows`：將箭矢蓄力值、速度及基礎傷害乘以等級
- `lifesteal`：依照造成的傷害 × 等級來治療持有者
- `thorns`：反彈受到的傷害 × 等級；反彈受冷卻時間限制
- `fire_resistance`：熄滅火焰。
- `lava_swimmer`：預留的被動技能 ID，目前沒有執行時效果。
- `iron_curtain`：超強無敵時間
- `lingering_wound`：將造成的部分傷害累積為臨時傷口，
  降低目標的有效最大生命值，
  並阻止生命值恢復超過剩餘生命值上限。額外傷害可使傷口累積值增加兩次，
  即同時獲得原始攻擊所產生的累積值
  以及由額外傷害帶來的額外累積值。
- `grave_dominion`：每半秒將附近實體向下移動其自身高度。
  作用半徑的格數等於技能等級。
- `experience_convergence`：將正經驗值獲取量乘以
  `1 + 等級 ×（升至下一級所需經驗值 / 7 - 1）`，
  使經驗等級增長趨近線性。
- `healing_aura`：將每次設定的 `heal` 週期性動作分享給等級數值格範圍內的
  友方生物。此技能本身不會產生治療。

範例
```json
{ "passive_skills": { "arrow_deflection": 1.0 } }
```

## 屬性 (Properties)

屬性在 `properties` 下進行配置。

特性
- 在不同槽位裝備相同的遺物可疊加效果。
- 在同一插槽重複裝備不計入疊加。

每項配置均採用開放式設計。新增配置項目時，
無需修改此類程式碼或新增解析邏輯分支。
- `attack_damage`（攻擊傷害）
- `attack_speed`（攻擊速度）
- `block_interaction_range`（方塊互動範圍）
- `entity_interaction_range`（實體互動範圍）
- `knockback_resistance`（擊退抗性）
- `max_health`（最大生命值）
- `armor`（護甲值）
- `armor_toughness`（護甲韌性）

各項均支援 `add`（加法）、`mul_base`（基礎乘法）和 `mul_total`（總值乘法）。

範例
```json
{ "properties": { "max_health": { "add": 10.0 } } }
```

## 週期性動作 (Periodic Actions)

週期性動作在 `ticks` 下進行配置。
支持的動作包括 `heal`（治療）和 `feed`（進食）；配置項支持 `interval_ticks`（間隔刻數）、`add`（加法）和 `ratio_add`（比例加法）。

```json
{ "ticks": { "heal": { "interval_ticks": 20, "add": 1, "ratio_add": 0.01 } } }
```

## 回呼規則 (Callback Rules)

特性
- 在不同槽位裝備相同的遺物可疊加效果。
- 在同一插槽重複裝備不計入疊加。

- `damage_dealt`（造成的傷害）
- `damage_taken`（受到的傷害）
- `invulnerable_time_taken`（獲得的無敵時間）
- `invulnerable_time_dealt`


```text
damage_taken
damage_dealt
invulnerable_time_taken
invulnerable_time_dealt
```

回呼規則支援 `modifier`（修飾符）、`flat`（固定值）、`minimum`（最小值）、`ratio_minimum`（比例最小值）、`maximum`（最大值）和 `ratio_maximum`（比例最大值）。


## 法術

特性
- 每個法術都對應其專屬遺物，因此無需擔心衝突問題。

可用法術
- `teleport`（傳送）：
  針對方塊阻擋（BLOCK）：以薄方塊中心為目標；先嘗試上方位置，若受阻，則嘗試目標面之前的位置
  未命中（MISS）/ 空中（sky） -> 沿著視線方向盡可能遠地傳送
  參數：`range`（範圍）、`recovery`（復原時間）、可選參數 `priority`（優先權）。 `range` 上限為 256 格。
- `curse`（詛咒）：對指向的生物調用 `directAttack`（直接攻擊）
- `heal`（治療）：恢復 `amount` 點生命值，外加施法者最大生命值一定比例（`ratio`）的生命值
- `healing_ray`（治療射線）：治療 `range` 範圍內指向的生物
  治療量為施法者攻擊傷害的 `intensity` 倍
- `cleanse`（淨化）：移除施法者身上的所有負面效果
- `dash`（衝刺）：將施法者向前推進 `strength` 距離，可選垂直（`vertical`）速度
- `arc_burst`（弧光爆發）：對 `range` 範圍內的敵對目標造成多次傷害；
  每次命中造成攻擊傷害 `intensity`% 的傷害，重複 `count` 次，
  可選 `weaken`（虛弱）減益強度
- `repulse`（排斥）：將 `range` 範圍內的敵對生物推離施法者
  推力為 `strength`，可選垂直（`vertical`）抬升效果
- `absorption`（吸收）：賦予「吸收」效果，持續 `duration_ticks` 刻，強度為配置的 `amplifier`
- `sky_launch`（升空）：將 `range` 範圍內的敵對生物向上拋起 `strength` 距離
- `shadow_exchange`（暗影互換）：與 `range` 範圍內指向的敵對生物互換位置
- `grave_shift`（葬地位移）：將 `range` 範圍內指向的非友方生物移入地下。
  設定範圍上限為 256 格。
- `phantom_step`（幻影步）：瞬間向前移動最多 `range` 格，並對路徑上的敵對
  生物造成攻擊傷害 `intensity`% 的傷害
- `upgrade_enchanted_book`（升級附魔書）：升級主手所持附魔書上的
  第一個未達最高等級的附魔，提升一級，
  消耗 `experience_cost` 級經驗。創造模式玩家無需支付此消耗。
- `enchantment_ascension`（附魔提升）：將主手物品上的第一個非滿級附魔提升一級，
  消耗 `experience_cost` 數量的經驗等級。創造模式玩家無需支付此消耗。
- `purify_curse`（淨化詛咒）：移除主手物品上的第一個詛咒；
- `purify_penalty`（淨化懲罰）：將鐵砧修復消耗懲罰重設為零。
  消耗 `experience_cost` 數量的經驗等級；創造模式玩家無需支付此消耗。
- `disenchantment`（附魔剝離）：移除主手物品上的第一個附魔，
  並將其以相同等級轉移至副手持有的書本上。
- `open_ender_chest`（開啟終界箱）：開啟施法者自己的終界箱。
- `return_to_bed`（返回床邊）：將施法者傳送至重生床旁安全的站立位置。
  若床已消失或周圍被阻擋，施法將失敗。

恢復 / 冷卻時間
```text
冷卻時間（秒） = 1 / 恢復速度
```

| `recovery`（恢復速度） | 冷卻時間 |
|---:|---:|
| `0.25` | 4 秒 |
| `1` | 1 秒 |
| `4` | 0.25 秒 |
