# ルートの設定

Vital Relics は遺物アイテムを Minecraft のルートテーブルへ追加できます。

**警告:** 生成された JSON を編集したら `customized` を `true` にしてください。`false` のままだと上書きされる可能性があります。

## ルート規則

ルート規則は遺物をどこから入手できるかを定義します。対象ルートテーブルと出現確率を指定します。

```json
{
  "table": "minecraft:chests/simple_dungeon",
  "chance": 0.05
}
```

この例では設定した遺物をダンジョンのチェストへ 5% の確率で追加します。ルートテーブルには `minecraft:chests/stronghold_library` や `minecraft:entities/zombie` のような Minecraft リソースロケーションを使用します。

ルートテーブル生成時、Vital Relics は設定された規則を確認して一致する遺物エントリを追加します。
