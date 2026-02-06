# Healing Machine Models for Blockbench

This folder contains the Create-powered healing machine block models in **Minecraft Java Block Model** format, ready to open in Blockbench.

---

## Which file is the “right” block? (48×43×48 and OBJ)

You have three related files:

| File | Format | Use in Blockbench |
|------|--------|--------------------|
| **healing_machine_create.obj** | Wavefront OBJ (3D mesh) | **This is the one to load.** File → **Import** → **Import OBJ**. |
| **healing_machine_create_48x43x48.json** | Voxel list (47×42×47 voxels + RGB) | Not a Blockbench project. Raw voxel data – don’t open as “Minecraft Block” or “Generic Model”. |
| **healing_machine_create_48x43x48(1).json** | Same as above (duplicate copy) | Same as above. |

So: **use the .obj** when you want to load the 3D model into Blockbench. The two JSONs are voxel exports (list of coloured cubes), not Blockbench/Minecraft model JSON.

### If the OBJ “doesn’t look right” in Blockbench

- **Scale**  
  The name “48x43x48” means the original voxel grid was 48×43×48. In Minecraft one block = 16 units, so this model is **3× larger** than one block (48÷16=3). After importing the OBJ in Blockbench, select the whole model and **scale it down** (e.g. Scale → 0.333 or 1/3 on X and Z, and about 16/43 ≈ 0.37 on Y) so it fits in a 16×16×16 block space.

- **Origin / position**  
  The mesh might be offset. After scaling, use **Move** in Blockbench to centre it in 0–16 on X and Z, and sit on Y=0.

- **No textures**  
  OBJ often has no UVs. You can re-unwrap in Blockbench or use the existing JSON models (below) for textured versions.

---

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

### create_healingmachine.obj (no shafts)

**create_healingmachine.obj** is your main OBJ – no shaft stubs, so you don’t have to force shafts into the design.

- **create_healingmachine.mtl** – material file for this OBJ. One material, `body`, for the whole mesh. Keep it next to the OBJ when opening in Blockbench or Blender.
- The OBJ already has `mtllib create_healingmachine.mtl` and `usemtl body` at the top so it uses this MTL.

### healing_machine.obj (with shaft stubs)

**healing_machine.obj** is an alternate OBJ with shaft geometry (1×1 stubs, 1-block indent on left/right/bottom). Uses **healing_machine.mtl** (body, shaft, dot_red, dot_blue).

**Your style (grid blocks):** 1 unit = 1 grid block. Stub = 1×1, indent = 1 block, ring = 3×3.

- **Coordinates**: Minecraft model space, 0–16 per axis (Y = up).
- **No UVs** in OBJ; re-unwrap in your editor or use the JSON models for textured export.
