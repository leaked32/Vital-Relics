# 翻訳の設定

Vital Relics は遺物名、ツールチップ、スペル名、システムメッセージの翻訳を設定できます。

翻訳ファイルの保存先:

```text
config/vitalrelics/lang/
```

例: `en_us.json`, `ja_jp.json`, `zh_cn.json`, `zh_tw.json`.

## `customized`

`customized` は Vital Relics が更新された既定ファイルで置き換えてよいかを制御します。独自編集を始める前に `true` にしてください。

```json
{
  "customized": true,
  "translations": {
    "relic.vitalrelics.spell.teleport": "Teleport"
  }
}
```

# 翻訳キー

主な名前空間:

```text
relic.vitalrelics.*
message.vitalrelics.*
```

スペル名には `relic.vitalrelics.spell.<spell_id>`.

# 書式パラメータ

メッセージには `%s` プレースホルダーを含められます。書式付きメッセージを翻訳するときは必要なプレースホルダーを維持してください。

```json
"message.vitalrelics.selected_spell": "Selected spell: %s"
```

# 遺物名とツールチップ

安定した遺物 ID を変えずに表示テキストだけ翻訳できます。そのため翻訳変更時にレシピ、ルート設定、スペル参照を変更する必要はありません。

# 別言語の作成

`config/vitalrelics/lang/` に対応する JSON を作り、`"customized": true` にして既定言語と同じ翻訳キーを使用します。

# Modpack 向け推奨事項

1. 既定の翻訳ファイルから始めます。
2. `"customized": true` にします。
3. キーではなく値を変更します。
4. 必要な `%s` を維持します。
5. カスタム言語ファイルを Modpack に同梱します。
