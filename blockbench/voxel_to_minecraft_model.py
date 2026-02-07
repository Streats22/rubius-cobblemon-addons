#!/usr/bin/env python3
"""
Convert Blockbench voxel model or PNG heightmap to Minecraft Java block model JSON.
- JSON: healing_machine_create_48x43x48(1).json (dimension + voxels)
- PNG: healing_machine_create_48x43x48.png (48x43 grayscale = X x Z, luminance = height 0..48)
Merges adjacent voxels into boxes and scales to fit one block (0-16).
Requires: pip install Pillow (for --png).
"""
import json
import sys
from pathlib import Path

def load_voxels(path):
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    dim = data["dimension"][0]
    w, h, d = int(dim["width"]), int(dim["height"]), int(dim["depth"])
    filled = set()
    for v in data["voxels"]:
        x, y, z = int(v["x"]), int(v["y"]), int(v["z"])
        filled.add((x, y, z))
    return filled, w, h, d

def load_voxels_from_png(path, max_height=48):
    """Load a 48x43 grayscale PNG as heightmap: pixel (x,z) -> luminance -> y in [0, max_height)."""
    try:
        from PIL import Image
    except ImportError:
        print("Error: Pillow required for PNG. Run: pip install Pillow", file=sys.stderr)
        sys.exit(1)
    img = Image.open(path)
    img = img.convert("L")
    width, height = img.size
    # Image: width = X (48), height = Z (43). Each pixel (x,z) -> height from luminance.
    w, d = width, height
    h = max_height
    filled = set()
    for x in range(w):
        for z in range(d):
            lum = img.getpixel((x, z))
            y_top = min(max_height, max(0, int(lum * max_height / 256)))
            for y in range(y_top):
                filled.add((x, y, z))
    return filled, w, h, d

def merge_boxes(filled):
    """Greedy merge: for each voxel, grow largest box in X then Y then Z, then remove it."""
    remaining = set(filled)
    boxes = []
    while remaining:
        (x, y, z) = next(iter(remaining))
        # Max extent in X (same y, z)
        dx = 1
        while (x + dx, y, z) in remaining:
            dx += 1
        # Max extent in Y (same z, full x strip)
        dy = 1
        while all((x + ix, y + dy, z) in remaining for ix in range(dx)):
            dy += 1
        # Max extent in Z (full x-y rectangle)
        dz = 1
        while all((x + ix, y + iy, z + dz) in remaining for ix in range(dx) for iy in range(dy)):
            dz += 1
        boxes.append((x, y, z, x + dx, y + dy, z + dz))
        for ix in range(dx):
            for iy in range(dy):
                for iz in range(dz):
                    remaining.discard((x + ix, y + iy, z + iz))
    return boxes

def box_to_element(x0, y0, z0, x1, y1, z1, scale_x, scale_y, scale_z):
    """Convert box to Minecraft element with UVs that wrap: each face uses UVs matching its size/position in 0-16."""
    from_f = [round(x0 * scale_x, 4), round(y0 * scale_y, 4), round(z0 * scale_z, 4)]
    to_f = [round(x1 * scale_x, 4), round(y1 * scale_y, 4), round(z1 * scale_z, 4)]
    x0f, y0f, z0f = from_f[0], from_f[1], from_f[2]
    x1f, y1f, z1f = to_f[0], to_f[1], to_f[2]

    def uv(u1, v1, u2, v2):
        # Clamp to 0-16 so texture doesn't wrap oddly at edges
        u1 = max(0, min(16, round(u1, 4)))
        v1 = max(0, min(16, round(v1, 4)))
        u2 = max(0, min(16, round(u2, 4)))
        v2 = max(0, min(16, round(v2, 4)))
        return {"uv": [u1, v1, u2, v2], "texture": "#all"}

    # Minecraft face UV: (u1,v1) to (u2,v2) in 0-16 texture space. Map model coords so texture wraps.
    # North (face at z=z0), South (face at z=z1): extent in X and Y
    # East (face at x=x1), West (face at x=x0): extent in Z and Y
    # Up (face at y=y1), Down (face at y=y0): extent in X and Z
    faces = {
        "north": uv(x0f, 16 - y1f, x1f, 16 - y0f),
        "south": uv(x0f, 16 - y1f, x1f, 16 - y0f),
        "east": uv(z0f, 16 - y1f, z1f, 16 - y0f),
        "west": uv(z0f, 16 - y1f, z1f, 16 - y0f),
        "up": uv(x0f, z0f, x1f, z1f),
        "down": uv(x0f, z1f, x1f, z0f),
    }
    return {"from": from_f, "to": to_f, "faces": faces}

def main():
    script_dir = Path(__file__).resolve().parent
    use_png = "--png" in sys.argv
    if use_png:
        png_path = script_dir / "healing_machine_create_48x43x48.png"
        if not png_path.exists():
            print(f"Error: {png_path} not found", file=sys.stderr)
            sys.exit(1)
        filled, w, h, d = load_voxels_from_png(png_path, max_height=48)
        print(f"Loaded {len(filled)} voxels from PNG heightmap, dimension {w}x{h}x{d}", file=sys.stderr)
    else:
        voxel_path = script_dir / "healing_machine_create_48x43x48(1).json"
        if not voxel_path.exists():
            print(f"Error: {voxel_path} not found", file=sys.stderr)
            sys.exit(1)
        filled, w, h, d = load_voxels(voxel_path)
        print(f"Loaded {len(filled)} voxels, dimension {w}x{h}x{d}", file=sys.stderr)

    boxes = merge_boxes(filled)
    print(f"Merged into {len(boxes)} boxes", file=sys.stderr)

    # Scale to fit 0-16 (one block)
    scale_x = 16.0 / w
    scale_y = 16.0 / h
    scale_z = 16.0 / d

    elements = []
    for (x0, y0, z0, x1, y1, z1) in boxes:
        elements.append(box_to_element(x0, y0, z0, x1, y1, z1, scale_x, scale_y, scale_z))

    out = {
        "textures": {
            "all": "rubius_cobblemon_addons:block/healing_machine",
            "tray": "cobblemon:block/functional/healing_machine_tray",
            "particle": "rubius_cobblemon_addons:block/healing_machine"
        },
        "elements": elements,
        "gui_light": "side",
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 135, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]},
            "firstperson_lefthand": {"rotation": [0, 135, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]}
        }
    }

    models_dir = script_dir.parent / "src" / "main" / "resources" / "assets" / "rubius_cobblemon_addons" / "models" / "block"
    models_dir.mkdir(parents=True, exist_ok=True)
    # When using PNG, overwrite the main base model (_0) so the game uses it
    out_name = "create_powered_healing_machine_0.json" if use_png else "create_powered_healing_machine_new.json"
    out_path = models_dir / out_name
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=4)
    print(f"Wrote {len(elements)} elements to {out_path}", file=sys.stderr)

if __name__ == "__main__":
    main()
