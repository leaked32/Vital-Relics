# Configuring Textures

Relic textures can be replaced without modifying the mod jar or a resource pack.

## Texture Directory

Place custom textures in:

```text
config/vitalrelics/textures
```

The `texture` value in `relics.json` is used as the filename:

```json
{ "id": "iron_heart", "texture": "my_iron_heart.png" }
```

Place the image at `config/vitalrelics/textures/my_iron_heart.png`.

## Subdirectories

Subdirectories are supported. For example, `{ "texture": "custom/iron_heart.png" }` uses:

```text
config/vitalrelics/textures/custom/iron_heart.png
```

## Fallback Behavior

Vital Relics first looks for the texture in the external texture directory. If the file is missing
or cannot be loaded, it automatically uses the bundled texture with the same filename. If neither
texture exists, Minecraft displays its missing-texture placeholder.

## Image Requirements

- Use PNG images.
- Use 16×16 pixels for the standard Minecraft-style appearance.
- Keep the `texture` value identical to the actual file path.
- Restart the game after adding or replacing a texture.

External textures override bundled textures, so the same filename can replace a default relic texture.
