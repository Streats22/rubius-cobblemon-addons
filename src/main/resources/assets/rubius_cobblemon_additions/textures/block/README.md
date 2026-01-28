# Block Textures

This folder should contain the texture files for the Create-Powered Healing Machine block.

## Required Textures

The following texture files are referenced in the block model:

1. **create_powered_healing_machine_front.png** - Front face (where the healing interface is)
2. **create_powered_healing_machine_back.png** - Back face
3. **create_powered_healing_machine_side.png** - Left and right sides
4. **create_powered_healing_machine_top.png** - Top face
5. **create_powered_healing_machine_bottom.png** - Bottom face

## Texture Specifications

- **Format**: PNG
- **Size**: 16x16 pixels (standard Minecraft block texture size)
- **Location**: Place all textures in this folder (`textures/block/`)

## Creating Textures

You can:
1. Use the Cobblemon healing machine texture as a base and add Create cogwheel elements
2. Create custom textures from scratch
3. Use texture editing tools like:
   - Paint.NET
   - GIMP
   - Aseprite
   - Blockbench (for 3D models)

## Temporary Solution

Until custom textures are created, you can temporarily use placeholder textures by:
1. Copying textures from `cobblemon:healing_machine` 
2. Using Minecraft's default block textures (like `minecraft:block/iron_block`)
3. Creating simple colored placeholder textures

To use placeholder textures, edit `models/block/create_powered_healing_machine.json` and change the texture paths to reference existing textures.
