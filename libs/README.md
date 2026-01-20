# libs/ Folder - Cobblemon Dependency

## Quick Setup Instructions

1. **Download Cobblemon 1.7.1**:
   - Go to https://www.curseforge.com/minecraft/mc-mods/cobblemon/files
   - OR https://modrinth.com/mod/cobblemon
   - Download the **NeoForge** version for **Minecraft 1.21.1**
   - Look for version **1.7.1** (or latest 1.7.x)

2. **Place the JAR file here**:
   - Rename it to: `cobblemon-1.7.1-neoforge.jar`
   - Or keep the original name, but update `build.gradle` to match

3. **Dependency is already configured**:
   - The dependency lines in `build.gradle` are already uncommented
   - Just make sure the filename matches: `cobblemon-1.7.1-neoforge.jar`
   - If your JAR has a different name, update `build.gradle` to match

4. **Sync Gradle**:
   - In your IDE, click "Sync Gradle" or "Reload Gradle Project"
   - Or run: `./gradlew build --refresh-dependencies`

5. **Verify**:
   - After syncing, you should see Cobblemon classes available in your IDE
   - The build should complete without "Could not find" errors for Cobblemon

## Alternative: CurseForge Maven

If you prefer using CurseForge Maven instead:

1. Go to https://www.curseforge.com/minecraft/mc-mods/cobblemon/files
2. Find Cobblemon 1.7.1 NeoForge for MC 1.21.1
3. Click on the file to see its details
4. Look for the "Maven" snippet - it will show the file ID
5. In `build.gradle`, uncomment and update:
   ```groovy
   implementation "curse.maven:cobblemon-687131:XXXXXX" // Replace XXXXXX with file ID
   ```

## Troubleshooting

- **"Could not find" error**: Make sure the JAR filename in `build.gradle` matches the actual filename in `libs/`
- **"Class not found" errors**: Make sure you uncommented BOTH `compileOnly` and `localRuntime` lines
- **IDE doesn't see Cobblemon**: Try "Invalidate Caches / Restart" in your IDE, then sync Gradle again
