# Block Textures

Textures for the Create-Powered Healing Machine (Create + Cobblemon style). Model paths use these names (case must match):

- **healing_machine.png** – base (RPM &lt; 12, like default Cobblemon)
- **healing_machine_create_md.png** – medium (12–32 RPM)
- **healing_machine_create_fl.png** – full (32+ RPM)
- **healing_machine_active.png** – active/healing base
- **healing_machine_active_create_md_charge.png** – active medium
- **healing_machine_active_create_fl_charge.png** – active full

## Specs

- **Format**: PNG
- **Size**: 16×16. The model uses an **atlas layout**: different UV regions (0–16) map to different parts of the texture (body, tray edges, etc.). Keep UVs in 0–16 so textures display correctly.
- **Location**: this folder (`textures/block/`)
- **Tray**: The tray top uses Cobblemon’s texture (`cobblemon:block/functional/healing_machine_tray`); no local copy needed.
- **Shaft**: The block model no longer includes shaft/drive elements (removed to avoid Create shaft rendering glitches). The machine is purely Cobblemon-style body + tray.
- **Animated textures**: For any PNG that should animate (e.g. active/charge), add a `.mcmeta` file with the **exact same name** as the PNG: `healing_machine_active_create_fl_charge.png.mcmeta` next to `healing_machine_active_create_fl_charge.png`. The model references the texture by path (e.g. `healing_machine_active_create_fl_charge`); Minecraft only applies animation when it finds `&lt;that_name&gt;.png.mcmeta`. Example mcmeta: `{"animation":{"frametime":2}}` (2 game ticks per frame).

If the block turns purple/black, check that the PNG files above exist and that model JSON texture keys match these names exactly (e.g. `healing_machine_create_md`).

**After changing textures:** Run **`./gradlew build`** (or **`runClient`**) so assets are packaged; then reload resources in-game (F3+T) if needed.
