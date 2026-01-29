# Healing Machine Models for Blockbench

This folder contains the Create-powered healing machine block models in **Minecraft Java Block Model** format, ready to open in Blockbench.

## How to open in Blockbench

1. Open **Blockbench**.
2. Go to **File → Open** (or **File → Open Project**).
3. Choose **Minecraft Block Model** (or **Minecraft Java Block**).
4. Select one of the `.json` files in this folder:
   - **create_powered_healing_machine_base.json** – main model (base + shaft inserters)
   - **create_powered_healing_machine_powered.json** – powered state (yellow indicator)
   - **create_powered_healing_machine_active.json** – active/healing state

## Loading textures

The models reference these texture slots:

| Slot   | Used for                    | Suggested texture (from mod)        |
|--------|-----------------------------|-------------------------------------|
| `#all` | Main body, edges, sides     | `healing_machine.png` or `healing_machine_create_MD.png` |
| `#tray`| Top tray surface           | Cobblemon tray or your own          |
| `#shaft`| Shaft inserter cubes      | `minecraft:block/iron_block` or custom |
| `#active`| (active model only)       | `healing_machine_active.png`        |

To have Blockbench find your mod’s textures:

1. In Blockbench: **File → Project Settings** (or **Preferences**).
2. Set the **Minecraft Assets** or **Resource pack path** to your mod’s assets folder, e.g.  
   `src/main/resources/assets/`  
   so that paths like `rubius_cobblemon_additions:block/healing_machine` resolve.
3. Or add textures manually: **Texture tab → Add Texture** and assign your PNGs to the slots above.

Texture files are in:  
`src/main/resources/assets/rubius_cobblemon_additions/textures/block/`

## Model structure (base)

- **Base** (0,0,0)–(16,10,16): main grey body  
- **Front/back edges** and **left/right sides**: tray frame  
- **Top tray**: recessed top with tray texture  
- **Indicator strips** (tintindex 1): north/south strips (red when unpowered, blue when powered in-game)  
- **Shaft inserters**: left (west), right (east), bottom – small boxes for Create shaft connections  

Coordinates are in **block space** (0–16 per axis). You can edit cubes and UVs in Blockbench and re-export for the mod.

## Raw voxel mesh (OBJ)

**healing_machine.obj** is the same geometry as a single **Wavefront OBJ** mesh for editing in Blockbench or any 3D app.

**Your style (grid blocks):** In this file, **1 unit = 1 of your grid blocks**. So:
- **Stub** = **1 block × 1 block** (1×1 units) — the central shaft
- **Indent** = **1 block** (1 unit) around the stub — the sunken ring
- **Ring hole** = **3×3 blocks** (1 + 1 + 1 on each side)

- **Coordinates**: Minecraft model space, 0–16 per axis (Y = up). Full block = 16 units.
- **Contents**: Base body, tray, indicators; **left, right, bottom** each have a **4×4 block stub** with a **1-block** sunken ring around it. Marker dots (red/blue).
- **No UVs**: OBJ has no UV data; re-unwrap in your editor or use the JSON models for textured export.
