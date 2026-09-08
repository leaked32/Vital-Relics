# Rare relic texture sources

Generated using the built-in image generation tool. Final files are in `common/src/main/resources/vitalrelics/textures/`.

| File | Subject | Footprint |
| --- | --- | --- |
| frostbind_ring.png | silver ring with pale blue ice gem | 8×8 on a transparent 16×16 canvas |
| timberheart_ring.png | brown wooden ring with tiny green leaf gem | 8×8 on a transparent 16×16 canvas |
| quarry_ring.png | dark iron ring with amber stone gem | 8×8 on a transparent 16×16 canvas |
| borebolt_bracelet.png | copper bracelet with small steel arrowhead clasp | 10×10 on a transparent 16×16 canvas |
| discord_bracelet.png | dark purple bracelet with split crimson gem | 10×10 on a transparent 16×16 canvas |

Each asset was generated separately with this exact prompt, replacing SUBJECT with the corresponding table entry:

> Use case: stylized-concept. Asset type: Minecraft inventory item texture. One SUBJECT. Authentic vanilla Minecraft 16x16 pixel art enlarged with nearest neighbor: very simple chunky square pixel clusters, limited 6-color palette, hard edges, subtle top-left highlight, dark colored outline. Ring occupies centered 8x8 pixels or bracelet centered 10x10 pixels within transparent 16x16 canvas. Actual transparent background. No glow, no gradients, no antialiasing, no text, no scene, no detailed illustration.

Integration: threshold alpha at 50%, trim transparent padding, nearest-neighbor sample to the stated footprint, and center on a transparent 16×16 RGBA canvas. Existing assets were retained.

# Validation

Compile all common Java sources with Java 17, then compile and run `scripts/RareRelicRulesTest.java` against those classes. Run `python3 scripts/validate_materials.py` for recipes/materials and `python3 scripts/validate_rare_relics.py` for the new content.

Remaining in-game checks across all four loaders: enchantment-specific block drops, protected/canceled block breaks, connected trees, arrow wall traversal and reflected arrows, and forced attacks on passive creatures. Full Gradle compilation was blocked by the environment's network access to services.gradle.org.
