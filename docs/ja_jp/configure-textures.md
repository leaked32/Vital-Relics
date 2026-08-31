# テクスチャの設定

Mod の jar ファイルやリソースパックを変更せずに、遺物のテクスチャを差し替えられます。

## テクスチャの保存先

カスタムテクスチャを次の場所に配置します。

```text
config/vitalrelics/textures
```

`relics.json` の `texture` の値がファイル名として使用されます。

```json
{ "id": "iron_heart", "texture": "my_iron_heart.png" }
```

画像は `config/vitalrelics/textures/my_iron_heart.png` に配置します。

## サブディレクトリ

サブディレクトリも使用できます。`{ "texture": "custom/iron_heart.png" }` の場合は、次の場所です。

```text
config/vitalrelics/textures/custom/iron_heart.png
```

## フォールバック

Vital Relics はまず外部テクスチャディレクトリから画像を読み込みます。ファイルがない、または
読み込めない場合は、同じファイル名の内蔵テクスチャを自動的に使用します。どちらもない場合は、
Minecraft の欠落テクスチャが表示されます。

## 画像の要件

- PNG 画像を使用してください。
- 標準的な Minecraft 風の見た目には 16×16 ピクセルを使用してください。
- `texture` の値と実際のファイルパスを一致させてください。
- 画像を追加または置き換えた後はゲームを再起動してください。

外部テクスチャは内蔵テクスチャより優先されるため、同じファイル名で既定の遺物を置き換えられます。
