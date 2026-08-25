# レシピの設定

Vital Relics は動的な遺物クラフトレシピをサポートし、クラフト時に自動照合します。

## 対応レシピ

- shaped（定形）
- shapeless（不定形）

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

shapeless では材料順序は関係ありません。レシピが一致すると設定された遺物が作成されます。

## 備考

動的レシピはゲーム内で動作しますが、`recipes.json` から生成されたレシピは現在 JEI に表示されません。通常のクラフトは可能です。
