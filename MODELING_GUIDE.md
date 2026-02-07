# Creating a Custom Model for Create-Powered Healing Machine

Since you want the healing machine to look like Cobblemon's healing machine but with a cog inside and a connection point, here's how to create it:

## Option 1: Using Blockbench (Recommended - No coding needed!)

Blockbench is a free, user-friendly 3D modeling tool perfect for Minecraft models.

### Steps:

1. **Download Blockbench**: https://www.blockbench.net/
   - It's free and works on Windows, Mac, and Linux

2. **Open Blockbench**:
   - Click "New Model"
   - Select "Java Block/Item" 
   - Set dimensions to 16x16x16 (standard Minecraft block size)

3. **Create the Base (Healing Machine)**:
   - Add a cube for the main body
   - Try to match the shape of Cobblemon's healing machine
   - You can reference the healing machine in-game or look at Cobblemon's textures

4. **Add the Cog Inside**:
   - Add a smaller cube/cylinder for the cog
   - Position it in the center or visible area
   - Use Create's cog texture: `create:block/cogwheel`

5. **Add Connection Point**:
   - Add a small cube on one side (where the cog connects)
   - This represents the power input connection
   - Use Create's mechanical parts textures

6. **Export**:
   - File → Export → Java Block Model
   - Save as `create_powered_healing_machine.json`
   - Place in: `src/main/resources/assets/rubius_cobblemon_addons/models/block/`

7. **Textures**:
   - Export textures from Blockbench
   - Or use existing textures from Cobblemon and Create mods
   - Place in: `src/main/resources/assets/rubius_cobblemon_addons/textures/block/`

## Option 2: Reference Existing Models

You can try to reference Cobblemon's healing machine block directly. The block might be accessible via:

- `cobblemon:healing_machine` (block reference)
- Or extract textures from Cobblemon's JAR file

## Option 3: Extract Cobblemon Textures (Easiest for now!)

### Step-by-Step:

1. **Find the Cobblemon JAR**:
   - Go to: `run/mods/` folder in your project
   - Find the file: `Cobblemon-neoforge-*.jar`

2. **Extract Textures**:
   - JAR files are just ZIP files! Rename `.jar` to `.zip` and extract
   - Or use a tool like 7-Zip/WinRAR to open it directly
   - Navigate to: `assets/cobblemon/textures/block/`
   - Look for files like `healing_machine.png` or similar
   - Copy these texture files

3. **Add to Your Mod**:
   - Create folder: `src/main/resources/assets/rubius_cobblemon_addons/textures/block/`
   - Paste the healing machine textures there
   - Rename them if needed (e.g., `healing_machine_base.png`)

4. **Update the Model**:
   - Edit `create_powered_healing_machine.json`
   - Use Cobblemon textures for sides: `"north": "rubius_cobblemon_addons:block/healing_machine_base"`
   - Use Create cog texture for top: `"up": "create:block/cogwheel"`
   - This gives you the healing machine look with a visible cog on top!

## Option 4: Simple JSON Model (Current)

The current model uses Create's textures to give it a mechanical appearance. This is a temporary placeholder until you create a custom model.

## Quick Start with Blockbench

1. Download: https://www.blockbench.net/
2. Watch a 5-minute tutorial: Search "Blockbench Minecraft block tutorial" on YouTube
3. Create your model
4. Export and replace the current model file

The model will automatically update when you reload resources (F3+T) in-game!
