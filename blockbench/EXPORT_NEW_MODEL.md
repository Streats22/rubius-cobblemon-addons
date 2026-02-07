# Using the New 48×43×48 Healing Machine Model

The Create-style healing machine model is **`healing_machine_create_48x43x48(1).json`** (Blockbench voxel format, 47×42×47). It has been converted to Minecraft Java block format and is used in-game as `create_powered_healing_machine_new.json`.

## Automatic conversion (already done)

A Python script **`voxel_to_minecraft_model.py`** in this folder converts the voxel JSON to Minecraft block model JSON (merges voxels into boxes, scales to 0–16). To regenerate after editing the voxel model:

```bash
python blockbench/voxel_to_minecraft_model.py
```

That overwrites `src/main/resources/assets/rubius_cobblemon_addons/models/block/create_powered_healing_machine_new.json`. Textures can be changed later in that JSON (see “Texture variable (can be done later)” below).

## Manual export from Blockbench (alternative)

If you prefer to export from Blockbench instead of using the script:

## 1. Export from Blockbench

1. Open **Blockbench** and open your project (the 48×43×48 healing machine model).
2. If the project was created as **Generic Model** or **Bedrock**, use **File → Convert Project** → **Minecraft Java Block/Item** so the export format is correct.
3. Assign the **normal texture** for the mod:
   - In the **Texture** tab, add or select a texture.
   - Use the mod’s texture path: `rubius_cobblemon_addons:block/healing_machine`
   - Or use a single texture and name the variable **`#all`** so it matches the mod.
4. **File → Export → Export Minecraft Java Block Model**.
5. Save the exported JSON as:
   ```
   src/main/resources/assets/rubius_cobblemon_addons/models/block/create_powered_healing_machine_new.json
   ```
   (Overwrite the existing file there; that file is the placeholder the block uses.)

## 2. Texture variable (can be done later)

- Textures are **not** baked into the model; they’re set in the JSON. You can change them anytime by editing the `"textures"` block in `create_powered_healing_machine_new.json` (no need to re-export from Blockbench).
- The mod uses **`#all`** for the main texture. Right now it points to `healing_machine_create_MD` so the block doesn’t show missing (purple) texture. When you’re ready, change `"all"` to your texture path, e.g. `"rubius_cobblemon_addons:block/your_texture"`.
- In Blockbench, if your texture variable is named something else (e.g. `0` or `particle`), rename it to **`all`** in the exported JSON so the same texture reference works.

## 3. In-game and switching to the new model

- The block currently uses the **original** model (`create_powered_healing_machine_0`) so the existing texture looks correct. To use your new model:
  1. Replace `create_powered_healing_machine_new.json` with your Blockbench export (see [MODEL_AND_TEXTURE.md](MODEL_AND_TEXTURE.md) for UV and texture layout).
  2. In `models/block/create_powered_healing_machine.json`, set the parent to `create_powered_healing_machine_new` (instead of `create_powered_healing_machine_0`).
  3. Run the game and press **F3+T** (reload resources) to see the new model.

## Notes

- **Java block model limits**: Max 3×3×3 blocks; rotations in 22.5° steps. Blockbench will warn if the model exceeds these.
- **Resource path**: So Blockbench can resolve `rubius_cobblemon_addons:block/...`, set **File → Project Settings → Minecraft Assets** (or resource pack path) to your mod’s `src/main/resources/assets/` folder.
