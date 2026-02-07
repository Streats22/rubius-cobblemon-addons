# Cobblemon Source Reference

This project uses the Cobblemon source clone at **`e:\cobblemon-main`** as a direct reference for matching behaviour, models, and APIs.

## Repository layout

Cobblemon is a multi-module repo. Typical structure (when fully cloned, including submodules):

| Path | Contents |
|------|----------|
| `e:\cobblemon-main\common\` | Shared Kotlin code, block/entity logic, APIs |
| `e:\cobblemon-main\neoforge\` | NeoForge-specific registration and client code |
| `e:\cobblemon-main\fabric\` | Fabric-specific code |

If you only see root files (e.g. `build.gradle.kts`, `changelogs/`, `settings.gradle.kts`) and no `common/` or `neoforge/`, pull submodules or clone the full repo so these modules exist.

## Healing machine – where to look

Use these as search targets when you have the full source:

| What | Typical location (relative to repo root) |
|------|------------------------------------------|
| **Block** | `common/src/main/kotlin/.../block/HealingMachineBlock.kt` or similar |
| **Block entity** | `common/src/main/kotlin/.../block/entity/HealingMachineBlockEntity.kt` |
| **Client renderer** | `neoforge/src/client/kotlin/.../render/block/HealingMachineBlockEntityRenderer.kt` (or under `client/`) |
| **Blockstates** | `common/src/main/resources/assets/cobblemon/blockstates/healing_machine.json` |
| **Block models** | `common/src/main/resources/assets/cobblemon/models/block/` (e.g. `healing_machine.json`) |
| **Textures** | `common/src/main/resources/assets/cobblemon/textures/block/` (e.g. `functional/healing_machine*.png`) |
| **Registries** | Search for `healing_machine` or `HealingMachine` in `common/` and `neoforge/` |

## Changelog hints (this repo)

From Cobblemon changelogs (under `e:\cobblemon-main\changelogs\`):

- **1.3.0:** Healing Machine (and PC) models/bounding boxes updated; charge level shown in 6 stages; comparator output; pokeball positioning fix in renderer.
- **1.2.0:** Healer advancements and healing machine mechanics; dropped item forms for Healing Machine.

Use these to find the right commits/files for model format, renderer behaviour, and block logic.

## Using the reference in this addon

- **Create-powered healing machine:** Implement block/block entity to mirror Cobblemon’s healing machine where relevant (state, charge, tray), then add Create power and our models.
- **Rendering:** Match Cobblemon’s pokeball layout and tray positions (see `CreatePoweredHealingMachineRenderer` and Cobblemon’s `HealingMachineBlockEntityRenderer`).
- **Models/textures:** Align texture keys and model structure with Cobblemon’s `healing_machine` assets so behaviour and look stay consistent.
