# 配置法术

Vital Relics 包含数据驱动的法术系统。法术在 `relics.json` 中每件遗物的 `available_spells` 对象内配置。

```json
{
  "id": "example_relic",
  "available_spells": {
    "teleport": { "range": 64, "recovery": 2 }
  }
}
```

一件遗物可以提供多个法术。选择状态与冷却按 LivingEntity 分别记录。

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

只有成功激活后才进入冷却；失败施法不消耗冷却。

## Duplicate Spells

多个已装备遗物可以提供同一个法术。重复时使用 `priority` 选择；省略时默认评分为 `intensity * recovery`。

# 可用法术

## Teleport

Spell ID: `teleport`

Teleport 将施法者向其视线方向传送。

```json
"teleport": {
  "range": 128,
  "recovery": 2
}
```

参数：`range`、`recovery`、可选 `priority`。`range` 上限为 256 方块。

指向空中时沿视线寻找尽可能远的有效位置；指向方块时根据碰撞形状寻找周围合适位置。门、活板门、铁栏杆和玻璃板等薄方块使用其方块单元；雪层在目标判定中被忽略。没有有效位置时施法失败且不进入冷却。

## Curse

Spell ID: `curse`

Curse 攻击施法者直接瞄准的 LivingEntity。

```json
"curse": {
  "intensity": 100,
  "range": 128,
  "recovery": 2
}
```

参数：`intensity`、`range`、`recovery`、可选 `priority`。

```text
damage = caster attack damage * intensity / 100
```

没有有效 LivingEntity、目标超出范围或为友方、或 `intensity` / `range` 非正数时，Curse 失败且不进入冷却。

# 实体支持

核心法术系统基于 `LivingEntity`，而非仅限玩家，因此兼容实体与自定义生物可以复用同一套遗物和法术架构。

# 添加法术

添加 `available_spells`、所需法术 ID，再配置对应参数。 把已有法术分配给其他遗物不需要修改 Java。
