# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a NeoForge mod for Minecraft 1.21.1 that adds integrations between Cobblemon and Create mods. The mod is called "Rubius Cobblemon Additions" (mod ID: `rubius_cobblemon_addons`).

**Key Technologies:**
- NeoForge 21.1.215 (Minecraft modding framework)
- Java 21 (required for Minecraft 1.21.1)
- Gradle build system
- Cobblemon 1.7.1 (Pokémon mod, written in Kotlin)
- Create mod (mechanical contraptions mod)
- JEI (Just Enough Items, recipe viewer)

## Essential Commands

### Build and Development
```bash
# Build the mod
./gradlew build

# Clean build artifacts
./gradlew clean

# Refresh dependencies if libraries are missing
./gradlew --refresh-dependencies

# Run Minecraft client with the mod loaded
./gradlew runClient

# Run dedicated server
./gradlew runServer

# Run data generation (generates resources)
./gradlew runData

# Run game test server
./gradlew runGameTestServer

# Run tests
./gradlew test
```

### Dependency Setup

**Important:** Cobblemon is NOT available via Maven. You must manually obtain the JAR:

**Option 1 (Recommended):** Build from source
```bash
# If a setup-cobblemon.sh script exists, run it
./setup-cobblemon.sh
```

**Option 2:** The project uses **Maven** for Cobblemon (Impact Maven); no JAR in `libs/` is required. If the IDE reports a missing `libs/cobblemon-1.7.1-neoforge.jar`, run **Java: Clean Java Language Server Workspace** (VS Code) or refresh Gradle / invalidate caches (IntelliJ). See `libs/README.md`.

**KotlinForge Requirement:** Cobblemon requires KotlinForge 5.3+ (language provider). The build adds it via `build.gradle` and the task `copyKotlinForgeToRunMods` copies it to `run/mods/`. Running `runClient` or `runServer` runs that task first. If you add Cobblemon manually to `run/mods/`, also ensure KotlinForge is there (or run `./gradlew copyKotlinForgeToRunMods`).

## Code Architecture

### Package Structure
```
nl.streats1.rubiusaddons/
├── RubiusCobblemonAdditions.java    # Main mod class (entry point)
├── RubiusCobblemonAdditionsModClient.java  # Client-only initialization
├── Config.java                       # Mod configuration
└── item/
    └── ModItems.java                 # Item registry
```

### NeoForge Mod Architecture

**Main Entry Point:** `RubiusCobblemonAdditions` class is annotated with `@Mod` and defines:
- `MOD_ID = "rubius_cobblemon_addons"` - Must match value in `gradle.properties` and `neoforge.mods.toml`
- Event bus subscription for mod lifecycle events
- Registration of mod components (items, blocks, etc.)

**Client-Side Code:** `RubiusCobblemonAdditionsModClient` handles client-only initialization. It's annotated with `@Mod(dist = Dist.CLIENT)` to prevent loading on dedicated servers.

**Registration Pattern:** 
- Use `DeferredRegister` for registering game objects (items, blocks, entities, etc.)
- Register to the mod event bus in the main mod constructor
- Example: `ModItems.ITEMS` is a `DeferredRegister.Items` that registers all custom items

**Event Handling:**
- Mod event bus: For mod lifecycle events (e.g., `FMLCommonSetupEvent`, `BuildCreativeModeTabContentsEvent`)
- NeoForge event bus: For game events (e.g., `ServerStartingEvent`)

### Configuration System

The mod uses NeoForge's `ModConfigSpec` for configuration:
- Config defined in `Config.java`
- Registered in main mod constructor with `modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC)`
- Config file generated at runtime in `config/` folder

### Resource Generation

Data generation is configured in `build.gradle` under the `data` run configuration:
- Generated resources output to `src/generated/resources/`
- Existing resources in `src/main/resources/` are used as reference
- Run with `./gradlew runData` to regenerate

### Asset Structure

```
src/main/resources/assets/rubiusaddons/
└── lang/
    └── en_us.json    # English translations

src/main/templates/META-INF/
└── neoforge.mods.toml    # Mod metadata (uses Gradle property expansion)
```

**Important:** The `neoforge.mods.toml` file uses `${variable}` placeholders that are replaced by Gradle during build using values from `gradle.properties`.

## Development Guidelines

### Adding New Registries

When adding new registries (blocks, entities, etc.):
1. Create a new class similar to `ModItems.java`
2. Create a `DeferredRegister` for the registry type
3. Add a `register(IEventBus)` method
4. Call the register method in the main mod constructor

### Mod Dependencies

The mod declares optional dependencies on:
- **Create** - For Create-powered machines
- **Cobblemon** - For Pokémon interactions
- **JEI** - For recipe integration

Use conditional loading when accessing APIs from these mods to maintain optional dependency status.

### Java Version

The mod targets Java 21. This is mandatory for Minecraft 1.21.1. Ensure:
- `JAVA_HOME` points to JDK 21
- IDE is configured to use Java 21 language level
- No Java 17 or earlier features should be avoided

### Gradle Configuration

Key Gradle properties in `gradle.properties`:
- `minecraft_version=1.21.1`
- `neo_version=21.1.215`
- `mod_id=rubius_cobblemon_addons`
- `mod_version=0.0.1`
- `mod_group_id=nl.streats1.rubiusaddons`

When changing `mod_id`, update:
1. `gradle.properties`
2. `@Mod` annotation in main class
3. Package names (optional but recommended)
4. Asset folder names under `src/main/resources/assets/`

### IDE Setup

Recommended IDEs: IntelliJ IDEA or Eclipse

**IntelliJ IDEA:**
- Project automatically downloads sources and javadocs (configured in `build.gradle`)
- Gradle sync runs `generateModMetadata` task automatically

**If issues occur:**
- Run `./gradlew --refresh-dependencies`
- Run `./gradlew clean`
- Restart IDE

## Testing and Running

### Running in Development

Use Gradle tasks to launch Minecraft:
- Client: `./gradlew runClient` - Opens Minecraft client with mod loaded
- Server: `./gradlew runServer` - Starts dedicated server with `--nogui` flag
- Data generation: `./gradlew runData` - Generates mod resources

### Game Test Framework

NeoForge includes a game test framework:
- Tests can be written using `@GameTest` annotation
- Run tests with `./gradlew runGameTestServer`
- System property `neoforge.enabledGameTestNamespaces` is set to mod ID

### Build Output

Built mod JAR is located at: `build/libs/rubius_cobblemon_addons-<version>.jar`

This JAR can be placed in a Minecraft instance's `mods/` folder.

## Common Patterns

### Access Transformers

If you need to access private Minecraft code:
- Add transformations to `META-INF/accesstransformer.cfg`
- Uncomment the accessTransformers line in `build.gradle`

### Mixins

For more complex modifications:
- Create mixin config JSON file
- Declare in `neoforge.mods.toml` using `[[mixins]]` section
- Add Mixin dependency to `build.gradle`

### Parchment Mappings

The project uses Parchment mappings for better parameter names and javadocs:
- Configured in `build.gradle` under `neoForge.parchment`
- Version specified in `gradle.properties`
- Updates available at https://parchmentmc.org/docs/getting-started

## Troubleshooting

### Missing Libraries
Run: `./gradlew --refresh-dependencies`

### Build Issues
Run: `./gradlew clean` then rebuild

### Cobblemon Not Found / IDE "libs/cobblemon...jar" Error
Cobblemon is supplied via Maven in `build.gradle`. No file in `libs/` is needed. If the IDE still references a missing libs JAR, clean the Java language server workspace and reload, or run `./gradlew --refresh-dependencies`.

### KotlinForge Issues
Ensure KotlinForge 5.3+ JAR is in `run/mods/` folder when running the game

### Config Not Generating
Config files are only generated when the game runs, not during build

## Cobblemon Reference (Source Clone)

A local clone of the original Cobblemon project is available at **`e:\cobblemon-main`** for direct reference when implementing or matching Cobblemon behaviour (e.g. healing machine block, block entity, renderer, models, textures).

- **Where to look:** See [COBBLEMON_REFERENCE.md](COBBLEMON_REFERENCE.md) for typical paths (block, block entity, client renderer, assets). The Cobblemon repo is multi-module; the main source usually lives under `common/` and platform code under `neoforge/` (or similar). If your clone only has the root/build-logic, ensure submodules or full repo are pulled to get `common/` and platform modules.
- **Use it for:** Matching healing machine logic, block state, rendering (e.g. pokeball layout), model structure, texture keys, and Cobblemon API usage.

## Additional Resources

- NeoForge Documentation: https://docs.neoforged.net/
- NeoForge Discord: https://discord.neoforged.net/
- Mojang Mappings License: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md
