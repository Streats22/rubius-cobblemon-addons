# Block Textures

Textures for the Create-Powered Healing Machine. Model paths use these names (case must match):

- **healing_machine.png** – base (RPM &lt; 12, like default Cobblemon)
- **healing_machine_create_md.png** – medium (12–32 RPM)
- **healing_machine_create_fl.png** – full (32+ RPM)
- **healing_machine_active.png** – active/healing base
- **healing_machine_active_create_md_charge.png** – active medium
- **healing_machine_active_create_fl_charge.png** – active full

## Specs

- **Format**: PNG
- **Size**: 16×16 (or match the base model’s UV layout) so the block doesn’t show purple/black (missing texture).
- **Location**: this folder (`textures/block/`)

If the block turns purple/black when powered, check that these files exist here and that their names match the paths above exactly (including MD/FL casing).
