# 配置遗物

Vital Relics 是一个高度可配置、基于数据的遗物系统。遗物在 `relics.json` 文件中定义。

**警告：在编辑自动生成的 JSON 文件后，请将 `customized` 设为 `true`。若该项为 `false`，文件可能会被覆盖。**

配置文件目录为 `config/vitalrelics`。安装模组并启动游戏后，系统会自动生成相关文件。

## 基础信息

- `id`：遗物的唯一标识符；可通过配置创建新的 ID。
- `display_name`：可选的自定义显示名称。
- `tooltip`：备用遗物提示文本（Tooltip）。
- `texture`（纹理）
  Vital Relics 会优先在外部目录 `config/vitalrelics/textures` 中查找纹理。
  如果文件缺失或无法加载，
  系统会自动使用同名的内置纹理。
  如果两者都不存在，Minecraft 将显示“纹理缺失”的占位符。
  例如：
  `{ "id": "iron_heart", "texture": "my_iron_heart.png" }`
  请将图片放置在 `config/vitalrelics/textures/my_iron_heart.png`。
  支持使用子目录。
- `rarity`：遗物稀有度，支持 `common`（普通）、`uncommon`（罕见）、`rare`（稀有）、`epic`（史诗）。

示例
```json
{
"id": "iron_heart",
"display_name": "超级铁心",
"tooltip": "一颗由坚硬铁块锻造而成的小心脏。",
"texture": "iron_heart.png",
"rarity": "rare"
}
```

## Curios 配置

`curio_slot` 用于选择 Curios 槽位类型。`effective_slots` 用于控制遗物效果生效的位置。

- `in_hotbar` --- 遗物位于玩家快捷栏（hotbar）时生效。
- `in_inventory` --- 遗物位于玩家背包（inventory）时生效。这包括快捷栏。
- `in_curios_api_slots` --- 遗物通过 Curios API 装备时生效。 - `in_touhou_little_maid_curios_slots` --- 当装备于“东方小女仆（Touhou Little Maid）”的饰品槽位时，该圣物生效。

`in_inventory` 包含快捷栏。若省略 `effective_slots` 或其为空，则默认使用 Curios API 槽位及“东方小女仆”的 Curios 饰品槽位。

## 效果免疫

`immune_to_effects` 用于阻挡指定的药水效果。特殊值 `"all_negative"` 可阻挡所有负面效果。

```json
{ "immune_to_effects": ["poison", "blindness"] }
```

## 赋予效果

`granted_effects` 持续提供药水效果；每个值代表效果等级。

```json
{ "granted_effects": { "night_vision": 1, "speed": 2 } }
```

## 被动技能

`passive_skills` 定义了圣物的特殊被动行为。

- 被动技能在满足条件时自动激活。
- 被动技能等级不可叠加或相加，仅最高等级生效。

可用被动技能：
- `retarget_arrow`：反弹箭矢的最小伤害 = 攻击伤害 × 等级
- `arrow_deflection`：反弹一枚来袭箭矢；反弹伤害与速度乘以等级，冷却时间为 5 / 等级 秒
- `reality_severance`：等级% 的攻击伤害贡献，等级数值的格挡半径，
  约为 等级/4 的减益效果强度
- `metal_mending`：每 4 秒修复最多等于等级数值的耐久度
- `flight`：等级 > 0 即可飞行；飞行速度 = 原版飞行速度 × 等级，
  若等级为 1.0 则不改变速度
- `empowered_arrows`：将箭矢蓄力值、速度及基础伤害乘以等级
- `lifesteal`：根据造成的伤害 × 等级来治疗持有者
- `thorns`：反弹受到的伤害 × 等级；反弹受冷却时间限制
- `fire_resistance`：熄灭火焰。
- `iron_curtain`：超强无敌时间
- `lingering_wound`：将造成的部分伤害累积为临时伤口，
  降低目标的有效最大生命值，
  并阻止生命值恢复超过剩余生命值上限。额外伤害可使伤口累积值增加两次，
  即同时获得原始攻击产生的累积值
  以及由额外伤害带来的额外累积值。

示例
```json
{ "passive_skills": { "arrow_deflection": 1.0 } }
```

## 属性 (Properties)

属性在 `properties` 下进行配置。

特性
- 在不同槽位装备相同的遗物可叠加效果。
- 在同一槽位重复装备不计入叠加。

每项配置均采用开放式设计。新增配置项时，
无需修改此类代码或添加解析逻辑分支。
- `attack_damage`（攻击伤害）
- `attack_speed`（攻击速度）
- `block_interaction_range`（方块交互范围）
- `entity_interaction_range`（实体交互范围）
- `knockback_resistance`（击退抗性）
- `max_health`（最大生命值）
- `armor`（护甲值）
- `armor_toughness`（护甲韧性）

各项均支持 `add`（加法）、`mul_base`（基础乘法）和 `mul_total`（总值乘法）。

示例
```json
{ "properties": { "max_health": { "add": 10.0 } } }
```

## 周期性动作 (Periodic Actions)

周期性动作在 `ticks` 下进行配置。
支持的动作包括 `heal`（治疗）和 `feed`（进食）；配置项支持 `interval_ticks`（间隔刻数）、`add`（加法）和 `ratio_add`（比例加法）。

```json
{ "ticks": { "heal": { "interval_ticks": 20, "add": 1, "ratio_add": 0.01 } } }
```

## 回调规则 (Callback Rules)

特性
- 在不同槽位装备相同的遗物可叠加效果。
- 在同一槽位重复装备不计入叠加。

- `damage_dealt`（造成的伤害）
- `damage_taken`（受到的伤害）
- `invulnerable_time_taken`（获得的无敌时间）
- `invulnerable_time_dealt`


```text
damage_taken
damage_dealt
invulnerable_time_taken
invulnerable_time_dealt
```

回调规则支持 `modifier`（修饰符）、`flat`（固定值）、`minimum`（最小值）、`ratio_minimum`（比例最小值）、`maximum`（最大值）和 `ratio_maximum`（比例最大值）。


## 法术

特性
- 每个法术都对应其专属遗物，因此无需担心冲突问题。

可用法术
- `teleport`（传送）：
  针对方块阻挡（BLOCK）：以薄方块中心为目标；先尝试上方位置，若受阻，则尝试目标面之前的位置
  未命中（MISS）/ 空中（sky） -> 沿视线方向尽可能远地传送
  参数：`range`（范围）、`recovery`（恢复时间）、可选参数 `priority`（优先级）。`range` 上限为 256 格。
- `curse`（诅咒）：对指向的生物调用 `directAttack`（直接攻击）
- `heal`（治疗）：恢复 `amount` 点生命值，外加施法者最大生命值一定比例（`ratio`）的生命值
- `healing_ray`（治疗射线）：治疗 `range` 范围内指向的生物
  治疗量为施法者攻击伤害的 `intensity` 倍
- `cleanse`（净化）：移除施法者身上的所有负面效果
- `dash`（冲刺）：将施法者向前推进 `strength` 距离，可选垂直（`vertical`）速度
- `arc_burst`（弧光爆发）：对 `range` 范围内的敌对目标造成多次伤害；
  每次命中造成攻击伤害 `intensity`% 的伤害，重复 `count` 次，
  可选 `weaken`（虚弱）减益强度
- `repulse`（排斥）：将 `range` 范围内的敌对生物推离施法者
  推力为 `strength`，可选垂直（`vertical`）抬升效果
- `absorption`（吸收）：赋予“吸收”效果，持续 `duration_ticks` 刻，强度为配置的 `amplifier`
- `sky_launch`（升空）：将 `range` 范围内的敌对生物向上抛起 `strength` 距离
- `shadow_exchange`（暗影互换）：与 `range` 范围内指向的敌对生物互换位置
- `phantom_step`（幻影步）：瞬间向前移动最多 `range` 格，并对路径上的敌对
  生物造成攻击伤害 `intensity`% 的伤害
- `upgrade_enchanted_book`（升级附魔书）：升级主手所持附魔书上的
  第一个未达最高等级的附魔，提升一级，
  消耗 `experience_cost` 级经验。创造模式玩家无需支付此消耗。
- `enchantment_ascension`（附魔提升）：将主手物品上的第一个非满级附魔提升一级，
  消耗 `experience_cost` 数量的经验等级。创造模式玩家无需支付此消耗。
- `purify_curse`（净化诅咒）：移除主手物品上的第一个诅咒；
- `purify_penalty`（净化惩罚）：将铁砧修复消耗惩罚重置为零。
  消耗 `experience_cost` 数量的经验等级；创造模式玩家无需支付此消耗。
- `disenchantment`（附魔剥离）：移除主手物品上的第一个附魔，
  并将其以相同等级转移至副手持有的书本上。

恢复 / 冷却时间
```text
冷却时间（秒） = 1 / 恢复速度
```

| `recovery`（恢复速度） | 冷却时间 |
|---:|---:|
| `0.25` | 4 秒 |
| `1` | 1 秒 |
| `4` | 0.25 秒 |