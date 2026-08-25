# 設定翻譯

Vital Relics 支援設定遺物名稱、提示文字、法術名稱和系統訊息的翻譯。

翻譯檔案存放在：

```text
config/vitalrelics/lang/
```

例如： `en_us.json`, `ja_jp.json`, `zh_cn.json`, `zh_tw.json`.

## `customized`

`customized` 控制 Vital Relics 是否可以用更新後的預設檔案取代該檔案。自行修改前請設為 `true`。

```json
{
  "customized": true,
  "translations": {
    "relic.vitalrelics.spell.teleport": "Teleport"
  }
}
```

# 翻譯鍵

常用命名空間：

```text
relic.vitalrelics.*
message.vitalrelics.*
```

法術名稱使用 `relic.vitalrelics.spell.<spell_id>`.

# 格式參數

訊息可包含 `%s` 佔位符。翻譯格式化訊息時請保留所需佔位符。

```json
"message.vitalrelics.selected_spell": "Selected spell: %s"
```

# 遺物名稱與提示文字

不需修改穩定的遺物 ID 即可翻譯顯示文字，因此修改翻譯不需要同步修改配方、戰利品設定或法術引用。

# 建立其他語言

在 `config/vitalrelics/lang/` 下建立對應 JSON，設定 `"customized": true`，並使用與預設語言相同的翻譯鍵。

# 整合包建議

1. 從預設翻譯檔案開始。
2. 設定 `"customized": true`。
3. 修改需要的值，不要修改鍵。
4. 保留必要的 `%s` 佔位符。
5. 將自訂語言檔案隨整合包發布。
