# 配置翻译

Vital Relics 支持配置遗物名称、提示文本、法术名称和系统消息的翻译。

翻译文件存放在：

```text
config/vitalrelics/lang/
```

例如： `en_us.json`, `ja_jp.json`, `zh_cn.json`, `zh_tw.json`.

## `customized`

`customized` 控制 Vital Relics 是否可以用更新后的默认文件替换该文件。自行修改前请设置为 `true`。

```json
{
  "customized": true,
  "translations": {
    "relic.vitalrelics.spell.teleport": "Teleport"
  }
}
```

# 翻译键

常用命名空间：

```text
relic.vitalrelics.*
message.vitalrelics.*
```

法术名称使用 `relic.vitalrelics.spell.<spell_id>`.

# 格式参数

消息可包含 `%s` 占位符。翻译格式化消息时请保留所需占位符。

```json
"message.vitalrelics.selected_spell": "Selected spell: %s"
```

# 遗物名称与提示文本

无需修改稳定的遗物 ID 即可翻译显示文本，因此修改翻译不需要同步修改配方、战利品配置或法术引用。

# 创建其他语言

在 `config/vitalrelics/lang/` 下创建对应 JSON，设置 `"customized": true`，并使用与默认语言相同的翻译键。

# 整合包建议

1. 从默认翻译文件开始。
2. 设置 `"customized": true`。
3. 修改需要的值，不要修改键。
4. 保留必要的 `%s` 占位符。
5. 将自定义语言文件随整合包分发。
