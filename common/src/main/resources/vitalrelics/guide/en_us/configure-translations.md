# Configure Translations

Vital Relics includes configurable translations for relic names, tooltips, spell names and system messages.

**WARNING: after editing an automatically generated JSON file, set `customized` to `true`. While it is `false`, the file may be overwritten.**

Translation files are stored in:

```text
config/vitalrelics/lang/
```

Examples: `en_us.json`, `ja_jp.json`, `zh_cn.json`, `zh_tw.json`.

# Translation Keys

Common namespaces:

```text
relic.vitalrelics.*
message.vitalrelics.*
```

Spell names use `relic.vitalrelics.spell.<spell_id>`.

# Format Parameters

Messages may contain `%s` placeholders. Keep required placeholders when translating formatted messages.

```json
{ "message.vitalrelics.selected_spell": "Selected spell: %s" }
```

# Relic Names and Tooltips

Display text can be translated without changing stable relic IDs. Changing a translation therefore does not require changing recipes, loot configuration or spell references.

# Creating Another Language

Create the corresponding JSON file under `config/vitalrelics/lang/`, set `"customized": true`, and use the same translation keys as the default language.

# Modpack Recommendations

1. Start from the default translation file.
2. Set `"customized": true`.
3. Change the desired values, not the keys.
4. Preserve required `%s` placeholders.
5. Ship the customized language file with the modpack.
