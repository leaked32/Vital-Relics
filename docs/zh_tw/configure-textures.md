# 設定材質

無需修改 Mod 的 jar 檔案或資源包，就可以替換遺物材質。

## 材質目錄

將自訂材質放入：

```text
config/vitalrelics/textures
```

`relics.json` 中的 `texture` 值會被用作檔名：

```json
{ "id": "iron_heart", "texture": "my_iron_heart.png" }
```

將圖片放在 `config/vitalrelics/textures/my_iron_heart.png`。

## 子目錄

支援使用子目錄。例如，`{ "texture": "custom/iron_heart.png" }` 使用：

```text
config/vitalrelics/textures/custom/iron_heart.png
```

## 回退行為

Vital Relics 會先從外部材質目錄讀取檔案。如果檔案不存在或無法載入，會自動使用相同檔名的內建
材質。如果兩種材質都不存在，Minecraft 會顯示遺失材質佔位圖。

## 圖片要求

- 使用 PNG 圖片。
- 為保持標準 Minecraft 風格，請使用 16×16 像素。
- `texture` 的值必須與實際檔案路徑一致。
- 新增或替換材質後，請重新啟動遊戲。

外部材質的優先級高於內建材質，因此可以使用相同檔名替換預設遺物材質。
