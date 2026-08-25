# 設定配方

Vital Relics 支援動態遺物合成配方，並在合成時自動比對。

## 支援類型

- shaped（有序）
- shapeless（無序）

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

無序配方不要求材料順序。符合後會建立設定的遺物。

## 備註

動態配方可正常合成，但 `recipes.json` 產生的配方目前不會顯示在 JEI 中。
