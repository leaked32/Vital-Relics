# 設定戰利品

Vital Relics 支援將遺物物品加入 Minecraft 戰利品表。

**警告：** 編輯產生的 JSON 後請將 `customized` 設為 `true`；保持 `false` 時檔案可能被覆寫。

## 戰利品規則

規則定義遺物可以從哪裡取得，包括目標戰利品表與出現機率。

```json
{
  "table": "minecraft:chests/simple_dungeon",
  "chance": 0.05
}
```

此例以 5% 機率將遺物加入地牢箱子。戰利品表使用 `minecraft:chests/stronghold_library`、`minecraft:entities/zombie` 等 Minecraft 資源位置。

產生戰利品表時，Vital Relics 會檢查設定規則並加入符合的遺物條目。
