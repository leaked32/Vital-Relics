# レシピの設定

Vital Relicsは、動的なレリック（遺物）のクラフトレシピや、Minecraftのルートテーブル（戦利品テーブル）へのレリックアイテムの追加に対応しています。

**警告:** 自動生成されたJSONファイルを編集した後は、`customized`を`true`に設定してください。`false`のままだと、ファイルが上書きされる可能性があります。

## レシピ

クラフト時にレシピが自動的に照合されます。

- 定形レシピ (`shaped`)
- 不定形レシピ (`shapeless`)

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

不定形レシピの場合、材料の並び順は関係ありません。

# ルートテーブル

ルート（戦利品）生成時にレリックを入手できる場所を定義するルールです。このルールには、対象となるルートテーブルと出現確率が含まれます。

例
```json
{
"table": "minecraft:chests/simple_dungeon",
"chance": 0.05
}
```

これにより、設定されたレリックがダンジョンのチェストの戦利品に5%の確率で追加されます。ルートテーブルには、`minecraft:chests/stronghold_library`や`minecraft:entities/zombie`といったMinecraftのリソースロケーションが使用されます。

ルートテーブルが生成される際、Vital Relicsは設定されたルールを確認し、条件に一致するレリックのエントリを追加します。