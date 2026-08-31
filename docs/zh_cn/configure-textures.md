# 配置纹理

无需修改 Mod 的 jar 文件或资源包，就可以替换遗物纹理。

## 纹理目录

将自定义纹理放入：

```text
config/vitalrelics/textures
```

`relics.json` 中的 `texture` 值会被用作文件名：

```json
{ "id": "iron_heart", "texture": "my_iron_heart.png" }
```

将图片放在 `config/vitalrelics/textures/my_iron_heart.png`。

## 子目录

支持使用子目录。例如，`{ "texture": "custom/iron_heart.png" }` 使用：

```text
config/vitalrelics/textures/custom/iron_heart.png
```

## 回退行为

Vital Relics 会首先从外部纹理目录读取文件。如果文件不存在或无法加载，会自动使用相同文件名的
内置纹理。如果两种纹理都不存在，Minecraft 会显示缺失纹理占位图。

## 图片要求

- 使用 PNG 图片。
- 为保持标准 Minecraft 风格，请使用 16×16 像素。
- `texture` 的值必须与实际文件路径一致。
- 添加或替换纹理后，请重启游戏。

外部纹理的优先级高于内置纹理，因此可以使用相同文件名替换默认遗物纹理。
