"""Run with python3 scripts/validate_materials.py; no Minecraft dependencies required."""
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "common/src/main/resources/vitalrelics"
MODULES = ("forge-1.20.1", "neoforge-1.21.1", "neoforge-1.21.8", "neoforge-26.2")


def read_json(path):
	def unique_pairs(pairs):
		result = {}
		for key, value in pairs:
			# Existing locales contain unrelated duplicate keys; audit new material keys strictly.
			if path.parent.name != "lang" or "relic_core" in key or key.endswith(".material"):
				assert key not in result, f"Duplicate JSON key: {path}: {key}"
			result[key] = value
		return result
	return json.loads(path.read_text(), object_pairs_hook=unique_pairs)


def recipe_signature(recipe, mirror=False):
	if recipe["type"] == "shapeless":
		assert 1 <= len(recipe["ingredients"]) <= 9
		return ("shapeless", tuple(sorted(recipe["ingredients"])))
	rows = recipe["pattern"]
	assert 1 <= len(rows) <= 3 and 1 <= len(rows[0]) <= 3
	assert all(len(row) == len(rows[0]) for row in rows)
	used = set("".join(rows)) - {" "}
	assert used == set(recipe["key"]), "Unused or undefined shaped key"
	grid = [[recipe["key"].get(c, "") for c in row] for row in rows]
	while grid and not any(grid[0]):
		grid.pop(0)
	while grid and not any(grid[-1]):
		grid.pop()
	while grid and not any(row[0] for row in grid):
		grid = [row[1:] for row in grid]
	while grid and not any(row[-1] for row in grid):
		grid = [row[:-1] for row in grid]
	return ("shaped", tuple(tuple(reversed(row)) if mirror else tuple(row) for row in grid))


def main():
	for path in DATA.rglob("*.json"):
		read_json(path)
	materials = read_json(DATA / "materials.json")
	assert materials["_meta"]["version"] == "0.1.0"
	materials = materials["materials"]
	ids = [m["id"] for m in materials]
	assert ids == [t + "_relic_core" for t in ("common", "uncommon", "rare", "epic")]
	relics = {r["id"]: r for r in read_json(DATA / "relics.json")["relics"]}
	assert not set(ids).intersection(relics)
	acquisition = read_json(DATA / "recipes.json")
	signatures = {}
	for item_id, recipe in acquisition["recipes"].items():
		assert item_id in relics or item_id in ids
		assert recipe["count"] > 0
		ingredients = recipe.get("ingredients", list(recipe.get("key", {}).values()))
		for ingredient in ingredients:
			if ingredient.startswith("vitalrelics:"):
				assert ingredient.split(":")[1] in set(ids) | set(relics)
		if item_id in relics:
			assert "vitalrelics:" + relics[item_id].get("rarity", "common") + "_relic_core" in ingredients
		for mirrored in (False, True):
			sig = recipe_signature(recipe, mirrored)
			assert sig not in signatures or signatures[sig] == item_id, (item_id, signatures.get(sig))
			signatures[sig] = item_id
	for m in materials:
		assert m["id"] in acquisition["recipes"]
		assert bool(acquisition["loot"].get(m["id"])) == (m["rarity"] != "epic")
		png = (DATA / "textures" / m["texture"]).read_bytes()
		assert png[:8] == b"\x89PNG\r\n\x1a\n"
		assert struct.unpack(">II", png[16:24]) == (16, 16)
		for locale in ("en_us", "zh_cn", "zh_tw", "ja_jp"):
			lang = read_json(DATA / "lang" / (locale + ".json"))
			assert lang["item.vitalrelics." + m["id"]]
			assert lang["tooltip.vitalrelics." + m["id"]]
			assert lang["guide.vitalrelics.slot.material"]
	for module in MODULES:
		base = ROOT / module / "src/main/java/com/example/vitalrelics"
		main = (base / "VitalRelics.java").read_text()
		assert main.index("MaterialLoader.load(") < main.index("MaterialLoader.get().materials()")
		assert "class MaterialItem extends Item" in (base / "MaterialItem.java").read_text()
		assert "MaterialLoader.get().find(" in (base / "client/RelicRenderer.java").read_text()
		assert "Acquisition.get().data.recipes.entrySet()" in (base / "client/compat/VitalJeiPlugin.java").read_text()
		assert "Acquisition.get().data.loot.entrySet()" in (base / "acquisition/AcquisitionEvents.java").read_text()
	guide = (ROOT / "common/src/main/java/com/example/vitalrelics/common/guide/GuideBook.java").read_text()
	assert guide.index("MaterialLoader.get().materials()") < guide.index("loader.relics_")
	print(f"PASS: {len(ids)} materials, {len(acquisition['recipes'])} recipes, four locales and loaders")


if __name__ == "__main__":
	main()
