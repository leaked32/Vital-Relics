# 配置合成配方

Vital Relics 支援動態的遺物（relic）合成配方，並允許將遺物物品添加到 Minecraft 的戰利品表中。

**警告：** 編輯產生的 JSON 檔案後，請務必將 `customized` 設為 `true`。若該項為 `false`，檔案可能會被覆寫。


## 合成配方

合成時會自動搭配配方。

- 有序合成 (shaped)
- 無序合成 (shapeless)

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

對於無序合成配方，材料的擺放順序無關緊要。

# 戰利品表

戰利品規則定義了透過戰利品生成機制來獲取遺物的途徑。規則包含目標戰利品表和取得幾率。

範例
```json
{
  "table": "minecraft:chests/simple_dungeon",
  "chance": 0.05
}
```

這將把配置好的遺物以 5% 的幾率添加到地牢寶箱的戰利品中。戰利品表使用 Minecraft 資源定位符（Resource Location），例如 `minecraft:chests/stronghold_library` 和 `minecraft:entities/zombie`。

當產生戰利品時，Vital Relics 會檢查配置的規則並新增符合的遺物條目。