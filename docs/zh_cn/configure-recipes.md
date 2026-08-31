# 配置合成配方

Vital Relics 支持动态的遗物（relic）合成配方，并允许将遗物物品添加到 Minecraft 的战利品表中。

**警告：** 编辑生成的 JSON 文件后，请务必将 `customized` 设置为 `true`。若该项为 `false`，文件可能会被覆盖。


## 合成配方

合成时会自动匹配配方。

- 有序合成 (shaped)
- 无序合成 (shapeless)

```json
{
  "type": "shaped",
  "pattern": ["ABC", "DEF", " G "],
  "key": {
    "A": "minecraft:diamond",
    "B": "minecraft:gold_ingot",
    "G": "minecraft:ender_pearl"
  }
}
```

```json
{
  "type": "shapeless",
  "ingredients": ["minecraft:diamond", "minecraft:gold_ingot"]
}
```

对于无序合成配方，材料的摆放顺序无关紧要。

# 战利品表

战利品规则定义了通过战利品生成机制获取遗物的途径。规则包含目标战利品表和获取几率。

示例
```json
{
  "table": "minecraft:chests/simple_dungeon",
  "chance": 0.05
}
```

这将把配置好的遗物以 5% 的几率添加到地牢宝箱的战利品中。战利品表使用 Minecraft 资源定位符（Resource Location），例如 `minecraft:chests/stronghold_library` 和 `minecraft:entities/zombie`。

当生成战利品时，Vital Relics 会检查配置的规则并添加匹配的遗物条目。