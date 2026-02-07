# Model and Texture Compatibility

## Why the texture looked wrong

The distorted, “circuitry” look on the healing machine came from a **model–texture mismatch**:

1. **Our texture (`healing_machine.png`)** is a **16×16 atlas** laid out for the **original** Cobblemon-style model. Each face uses a **small region** of that atlas (e.g. north uses UV `[4, 13.5, 8, 16]` = one slice of the texture). The original model (`create_powered_healing_machine_0`) has many elements, each with UVs that pick the right part of the atlas.

2. **The placeholder “new” model** was a **single 16×16×16 cube** with **UV `[0, 0, 16, 16]` on every face**. That means the **entire** 16×16 texture was drawn on each of the six sides. So every face showed the full atlas (body, tray, edges, etc.) crammed onto one quad → repeated, stretched, misaligned look.

3. **Conclusion:** The current `healing_machine.png` is correct only when used with the **original** model (`create_powered_healing_machine_0`), which has the right UV layout. Using it on a single-cube or on a new mesh that has different UVs will keep looking wrong.

## Current setup (fixed)

- The **block and item** again use **`create_powered_healing_machine_0`** (original multi-part model with correct UVs).
- So in the crafting table and in-world, the healing machine should look correct with `healing_machine.png` (and the tray/shaft textures as in the original).

## Applying textures later

Textures are **referenced** in the model JSON, not baked into the mesh. You can:

- Export your model from Blockbench **without** final textures.
- In `create_powered_healing_machine_new.json`, the `"textures"` block (e.g. `"all": "rubius_cobblemon_addons:block/healing_machine_create_MD"`) is just a path. Change it anytime to point to a different PNG in `textures/block/` – no need to re-export from Blockbench unless you change geometry or UVs.

So: finish the model and UVs first; assign or swap textures later by editing the JSON (and adding any new PNGs to `textures/block/`).

## Using the new 48×43×48 Blockbench model

To use your new mesh without texture distortion:

1. **UV space:** Minecraft Java block models use **UV in 0–16** (normalized). In Blockbench:
   - Use **File → Convert Project → Minecraft Java Block/Item** so the project is in Java block format.
   - Use a **16×16 texture** in the project (or one that matches the 0–16 UV range). If you use a 48×48 or 256×256 texture, Blockbench may export UVs in 0–48 or 0–256; those must be **scaled to 0–16** (e.g. multiply by 16/48 or 16/256) in the JSON, or the texture will look wrong.

2. **Texture layout:** Either:
   - **Option A:** In Blockbench, UV-unwrap the new model so each face uses **regions of our existing** `healing_machine.png` atlas (like the original model). Then export; the same texture will work.
   - **Option B:** Use a **single texture** for the whole new model (one 16×16 or higher-res texture made for your mesh). In Blockbench assign that texture and name the variable **`#all`**, then export. Put that texture in `textures/block/` and point `#all` to it in the JSON.

3. **Model size:** Minecraft block space is **0–16 per axis**. If your Blockbench model is 48 units tall, scale it to 16 (e.g. scale 1/3) so it fits in one block, then export. Otherwise the model will extend outside the block.

4. **Switching to the new model:** When the new model JSON and texture are ready:
   - Save the export as `create_powered_healing_machine_new.json` (overwriting the placeholder).
   - In `models/block/create_powered_healing_machine.json`, change the parent from `create_powered_healing_machine_0` to `create_powered_healing_machine_new`.
   - Reload resources (F3+T) in-game.

## Reference: original model UVs

The original model (`create_powered_healing_machine_0` / `create_powered_healing_machine_base.json`) uses:

- **Main body** (0,0,0)–(16,10,16): north `[4, 13.5, 8, 16]`, east `[0, 11, 4, 13.5]`, etc. (small slices of the 16×16 texture).
- **Tray top:** `#tray` (Cobblemon tray texture).
- **Shaft/drive:** Removed from the model to avoid Create shaft rendering glitches; the block no longer has shaft elements.

So `healing_machine.png` is an **atlas**: different UV regions for different parts. Any new model that reuses it must use the same UV regions (or a new texture made for the new UV unwrap).
