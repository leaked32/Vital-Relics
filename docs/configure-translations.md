# Configure Translations

Vital Relics includes a configurable translation system for relic names, tooltips, spell names, and system messages.

Translation files are stored in:

```text
config/vitalrelics/lang/
```

Each language uses its own JSON file.

For example:

```text
en_us.json
zh_cn.json
ja_jp.json
```

## Basic Format

A translation file contains a `customized` flag and a collection of translation entries.

Example:

```json
{
  "customized": false,
  "translations": {
    "relic.vitalrelics.spell.teleport": "Teleport",
    "relic.vitalrelics.spell.curse": "Curse",
    "message.vitalrelics.selected_spell": "Selected spell: %s",
    "message.vitalrelics.spell_cooldown": "%s cooldown: %s",
    "message.vitalrelics.curse_requires_target": "Curse requires a target."
  }
}
```

The translation key identifies the text used by Vital Relics, while its value is the text displayed to the player.

# The `customized` Flag

The `customized` field controls whether Vital Relics is allowed to replace the translation file with an updated default version.

## Default Files

```json
"customized": false
```

A file with `customized` set to `false` is considered managed by Vital Relics.

When the mod provides an updated default version, the existing file may be overwritten.

This allows new translation keys, corrected text, and other default translation changes to appear automatically after updating the mod.

## Customized Files

If you want to edit a translation file manually, change:

```json
"customized": false
```

to:

```json
"customized": true
```

before making your changes.

Vital Relics will then preserve that file instead of replacing it with the bundled default version.

For example:

```json
{
  "customized": true,
  "translations": {
    "relic.vitalrelics.spell.teleport": "Spatial Leap",
    "relic.vitalrelics.spell.curse": "Crimson Curse"
  }
}
```

This is especially useful for modpacks that want their own terminology, flavor text, or localization.

> If you customize a translation file, always set `"customized": true`.
>
> Otherwise a future version of Vital Relics may overwrite your changes.

# Translation Keys

Vital Relics uses namespaced translation keys.

Common categories include:

```text
relic.vitalrelics.*
message.vitalrelics.*
```

## Spell Names

Spell names use:

```text
relic.vitalrelics.spell.<spell_id>
```

For example:

```json
{
  "relic.vitalrelics.spell.teleport": "Teleport",
  "relic.vitalrelics.spell.curse": "Curse"
}
```

The spell:

```text
teleport
```

therefore uses:

```text
relic.vitalrelics.spell.teleport
```

This translation is used when Vital Relics needs to display the spell name to a player.

## System Messages

System messages use keys such as:

```json
{
  "message.vitalrelics.selected_spell": "Selected spell: %s",
  "message.vitalrelics.spell_cooldown": "%s cooldown: %s",
  "message.vitalrelics.curse_requires_target": "Curse requires a target."
}
```

These messages are used for feedback from systems such as spell selection and casting.

# Format Parameters

Some translations contain placeholders:

```text
%s
```

These are replaced with values when the message is displayed.

For example:

```json
"message.vitalrelics.selected_spell": "Selected spell: %s"
```

may become:

```text
Selected spell: Teleport
```

Likewise:

```json
"message.vitalrelics.spell_cooldown": "%s cooldown: %s"
```

may become:

```text
Teleport cooldown: 0.8s
```

When translating these messages, keep the required placeholders unless you intentionally do not want the corresponding value displayed.

For example, this is valid:

```json
"message.vitalrelics.selected_spell": "Current ability: %s"
```

The surrounding text may change freely while `%s` remains the location where the spell name is inserted.

# Relic Names and Tooltips

Relic display text can also be translated rather than being permanently tied to the text in the default relic configuration.

This allows a modpack to change presentation without changing the actual relic ID.

For example, the internal ID:

```text
celestial_wings
```

can remain unchanged while its displayed name is translated or renamed.

This distinction is important:

```text
ID           -> stable identifier used by configuration/code
Translation  -> text shown to the player
```

Changing a translation therefore does not require changing recipes, loot configuration, spell references, or other systems that refer to the relic by ID.

# Creating Another Language

To provide another language, create the corresponding language file in:

```text
config/vitalrelics/lang/
```

For example:

```text
config/vitalrelics/lang/ja_jp.json
```

A customized language file should normally begin with:

```json
{
  "customized": true,
  "translations": {
  }
}
```

Then add translations using the same keys as the default language.

For example:

```json
{
  "customized": true,
  "translations": {
    "relic.vitalrelics.spell.teleport": "テレポート",
    "relic.vitalrelics.spell.curse": "呪い",
    "message.vitalrelics.selected_spell": "選択中のスペル: %s",
    "message.vitalrelics.spell_cooldown": "%s クールダウン: %s"
  }
}
```

# Modpack Recommendations

For a modpack that changes Vital Relics' terminology or localization:

1. Start from the default translation file.
2. Set `"customized": true`.
3. Change the desired translations.
4. Keep the translation keys unchanged.
5. Keep required `%s` placeholders in formatted messages.
6. Ship the customized language file with the modpack.

The IDs used by relics and spells do not need to match the wording shown to players.

This means a modpack can completely change the presentation of Vital Relics while keeping its underlying configuration stable.
