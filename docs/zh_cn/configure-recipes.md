# 配置配方

Vital Relics 支持动态遗物合成配方，并在合成时自动匹配。

## 支持类型

- shaped（有序）
- shapeless（无序）

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

无序配方不要求材料顺序。匹配成功后会创建配置的遗物。

## 备注

动态配方可正常合成，但 `recipes.json` 生成的配方目前不会显示在 JEI 中。
