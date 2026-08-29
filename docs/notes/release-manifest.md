# Vital Relics — Release Manifest

This file defines the required verification scope for a Vital Relics release.

It is intended for human review, automated validation, and repository review tools.
A release should not be considered verified unless every applicable section below has been checked.

---

## Default Configuration Files

Verify the bundled configuration files:

* `vitalrelics/relics.json`
* `vitalrelics/recipes.json`
* `vitalrelics/lang/*.json`

For every file:

* JSON parses successfully.
* `_meta.version` matches the corresponding expected version in `Manifest.java`.
* `_meta.customized` has the intended bundled value.
* No deprecated field names remain.
* No unknown or misspelled identifiers are referenced.

---

If new relics appear, read:
[add-new-relics.md](add-new-relics.md)

---


## 6. Texture and Model Integrity

For every default relic:

* Referenced texture file exists.
* Texture path uses the correct namespace/path.
* Texture loads in-game.
* Item model resolves correctly.
* No missing-texture placeholder appears.

For newly added textures:

* Intended dimensions are preserved.
* Transparency is preserved where required.

---

## 7. Localization Integrity

Required locales:

* `en_us`
* `ja_jp`
* `zh_cn`
* `zh_tw`

Verify:

* Every shipped relic has the required display-name translation where localization is used.
* Every shipped relic has the required tooltip translation.
* Every spell name/message key exists.
* Every passive-skill description/message key exists where applicable.
* Every HUD/message translation key exists.
* No locale is missing keys present in `en_us`.
* No obsolete keys remain accidentally referenced by code.

`en_us` is the canonical manually maintained locale when translated documentation/content is generated from it.

---

## 8. Spell Integrity

For every spell referenced by a relic:

* Spell implementation exists.
* Configuration parameters are recognized.
* Cooldown calculation is valid.
* Cooldown cannot overflow or become unintentionally negative.
* Target requirements behave correctly.
* Failure messages behave correctly.
* Spell selection behaves correctly.
* HUD state synchronizes correctly.
* Non-player casting works where intended.

Test at least one relic using every newly added or modified spell.

---

## 9. Passive-Skill Integrity

For every passive skill referenced by a relic:

* Handler exists.
* Level semantics match configuration/documentation.
* Zero/negative level behavior is safe.
* Cooldowns are bounded correctly.
* Entity ownership/targeting behavior is valid.
* Player and non-player behavior is consistent where intended.

For projectile-related skills:

* Original projectile impact is correctly canceled/skipped when required.
* Reflected projectile ownership is updated correctly.
* Damage is correct.
* Velocity is correct.
* Close-range targeting behaves correctly.

---

## 10. Enemy Relic Integrity

Verify hostile-mob relic behavior:

* Eligible mobs can receive configured relics.
* Spawn probability behaves approximately as configured.
* Relics are rolled only when intended.
* Persistent tags prevent unintended rerolling.
* Enemy relics survive/save correctly where intended.
* Relic effects actually apply to mobs.
* Relic passive skills work for mobs.
* Relic spells work for mobs where supported.
* Visual identification particles render correctly.

Balance smoke check:

* Common hostile mobs remain reasonably defeatable.
* No normal early-game mob receives obviously catastrophic default combinations at excessive frequency.

---

## 11. Damage and Combat Integrity

Smoke-test:

* Melee damage.
* Projectile damage.
* Percentage/modified damage.
* Invulnerability-time manipulation.
* Damage caps/protection.
* Healing/lifesteal.
* Retargeted arrows.
* Arrow deflection.
* Hostile-target detection.
* Allied entities are not incorrectly attacked.

Check both Forge and NeoForge for event-cancellation differences.

---

## 12. Scheduler Integrity

Verify scheduled systems do not:

* retain dead/unloaded entities indefinitely,
* leak per-player/per-entity state,
* overflow tick arithmetic,
* preserve expired cooldowns,
* remove active cooldowns prematurely.

Check:

* spell cooldowns,
* protection cooldowns,
* arrow-deflection cooldowns,
* delayed tasks,
* periodic relic actions.

---

## 13. Configuration Compatibility

Test:

* Fresh installation.
* Existing uncustomized configuration.
* Existing customized configuration.
* Older supported configuration version.
* Corrupted/malformed configuration.

Expected behavior must be explicit:

* compatible files load,
* replaceable bundled defaults upgrade correctly,
* customized files are not silently destroyed,
* deprecated/incompatible files produce useful diagnostics.

---

## 14. HUD and Client Behavior

Verify:

* Spell HUD renders.
* HUD does not cover the hotbar.
* HUD does not cover held-item rendering.
* Cooldown display updates.
* Selected spell updates.
* HUD clears when appropriate.
* Keybinds work.
* Shift-scroll spell switching works.
* Cast/drop-key interception behaves correctly.

Test at common GUI scales.

---

## 15. Optional-Mod Compatibility

When applicable, verify:

* Curios integration.
* JEI integration.
* Touhou Little Maid integration.

The mod must still load correctly when optional integrations that are genuinely optional are absent.

---

## 16. Survival Acquisition Test

Before publishing a release containing new relics:

For every newly added relic, answer:

**How does a survival player obtain this item?**

Then test that route in-game.

Example verification table:

| Relic               | Recipe | Loot   | Other | Tested |
| ------------------- | ------ | ------ | ----- | ------ |
| `windward_bracelet` | Yes/No | Yes/No | —     | Yes/No |

A new relic must not be marked complete until this row has an intentional data route.

---

## 17. Runtime Smoke Test

For each supported loader:

* Start game.
* Create/load a world.
* Open creative inventory.
* Equip representative relics.
* Craft at least one dynamic relic recipe.
* Confirm JEI recipes if JEI is installed.
* Fight ordinary hostile mobs.
* Fight relic-bearing mobs.
* Cast representative spells.
* Exercise newly changed passive skills.
* Die and respawn.
* Save and reload world.
* Confirm no obvious log spam or exceptions.

For a substantial Beta release, test against representative large modpacks when practical.

---

## 18. Release-Diff Review

Before release, inspect changes since the previous published version.

For every newly added relic:

* definition
* texture
* localization
* data
* behavior implementation

For every newly added spell/passive skill:

* implementation
* configuration reference
* localization
* loader-specific integration
* runtime test

For every changed shared abstraction:

* Forge adapter checked
* NeoForge adapter checked

Do not rely solely on a general repository scan. Review each feature introduced by the release diff against this manifest.

---

## 19. Release Gate

### Critical — must pass

* Both loaders build.
* Game launches.
* Worlds load.
* Configuration loads safely.
* No known crash/corruption issue.
* Core combat/relic systems function.

### Required — should pass before normal release

* New relics are obtainable.
* New mechanics function on both loaders.
* Assets resolve.
* Localization is complete.
* Recipes/loot are valid.
* HUD/client features work.

### Balance / polish

* Spawn rates are reasonable.
* Relic power is reasonable.
* Tooltips are understandable.
* Visual presentation is acceptable.

Balance or presentation imperfections may be acceptable for a Beta release.

Correctness failures are not.

---

## Release Review Result

Version:

Commit:

Reviewer:

Forge 1.20.1: PASS / FAIL / NOT TESTED

NeoForge 1.21.1: PASS / FAIL / NOT TESTED

Configuration: PASS / FAIL / NOT TESTED

Relic integrity: PASS / FAIL / NOT TESTED

Acquisition: PASS / FAIL / NOT TESTED

Localization/assets: PASS / FAIL / NOT TESTED

Runtime smoke test: PASS / FAIL / NOT TESTED

Known issues:

Release decision: READY / NOT READY
