# 配置战利品

Vital Relics 支持将遗物物品加入 Minecraft 战利品表。

**警告：** 编辑生成的 JSON 后请将 `customized` 设置为 `true`；保持 `false` 时文件可能被覆盖。

## 战利品规则

规则定义遗物可以从哪里获得，包括目标战利品表与出现概率。

```json
{
  "table": "minecraft:chests/simple_dungeon",
  "chance": 0.05
}
```

此例以 5% 概率将遗物加入地牢箱子。战利品表使用 `minecraft:chests/stronghold_library`、`minecraft:entities/zombie` 等 Minecraft 资源位置。

生成战利品表时，Vital Relics 会检查配置规则并加入匹配的遗物条目。
