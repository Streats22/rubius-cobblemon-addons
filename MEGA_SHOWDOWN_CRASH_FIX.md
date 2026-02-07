# Mega Showdown crash – cause and fix

## What’s going wrong

The game **crashes when loading or creating a world** because **Mega Showdown** (updated to **1.6.4+1.7.2**) expects a **new JSON format** for mega stones, but your **Z-A Mega content** still uses the **old format**.

- **Error:** `No key aspect_conditions in MapLike[...]`
- **When:** Loading world / creating world (registry load).
- **Packs involved:** `mod/zamega` (from **zamega** mod JAR), **ZAfor1.7.zip**, and **ATM x MSD [3.1.2].zip**.

You added “Z-A Mega’s Early” in a non‑traditional way (e.g. as a separate mod or custom pack). Those files still use the old `aspect` structure; 1.6.4+ requires `aspect_conditions`.

---

## Old vs new format

**Old (what your packs use):**
```json
{
  "showdown_id": "absolitez",
  "pokemons": ["Absol"],
  "aspect": {
    "apply_aspects": ["mega_evolution=mega_z"],
    "revert_aspects": ["mega_evolution=none"],
    "blacklist_aspects_apply": []
  }
}
```

**New (what Mega Showdown 1.6.4+ expects):**
```json
{
  "showdown_id": "absolitez",
  "pokemons": ["Absol"],
  "aspect_conditions": {
    "apply": { "aspects": ["mega_evolution=mega_z"] },
    "revert": { "aspects": ["mega_evolution=none"] }
  }
}
```

So the crash is from **format mismatch**, not from adding the pack “in a non‑traditional way” by itself.

---

## Where the old Z‑A data still is (after removing the resource pack)

The Z‑A mega data can still be loaded from **two** places:

1. **zamega mod (Navas' ZAmega)**  
   - **Path:** `E:\Prism launcher instances\Rubius Cobblemon(1)\minecraft\mods\zamega-neoforge-1.4.7.jar`  
   - This JAR is still in `mods/`. It provides the `mod/zamega` pack with old‑format mega JSONs. Removing “the resource pack” does **not** remove this mod.

2. **ZAfor1.7.zip copied into KubeJS data**  
   - **Path:** `E:\Prism launcher instances\Rubius Cobblemon(1)\minecraft\kubejs\data\ZAfor1.7.zip`  
   - This is the “extracted directly somewhere” copy: the ZA pack zip was placed in **kubejs/data/** so it gets loaded as data. The game still loads it and fails on the old format.

3. **Leftover references (optional clean‑up)**  
   - `options.txt` and `config/defaultoptions/options.txt` still list `file/ZAfor1.7.zip` in resource packs.  
   - `config/resourcepackoverrides.json` still has `"file/ZAfor1.7.zip"` in `default_packs`.  
   - Removing these references avoids the game looking for a missing file; the main fix is (1) and (2) below.

---

## Fix 1: Quick fix (stop the crash) — **APPLIED: files disabled with `.disabled`**

The following files have been **disabled** by renaming to add `.disabled` so the game ignores them. To re‑enable later, rename back (remove `.disabled`).

1. **zamega mod**  
   - Path: `…\minecraft\mods\`  
   - Renamed: `zamega-neoforge-1.4.7.jar` → **`zamega-neoforge-1.4.7.jar.disabled`**

2. **ZAfor1.7 in KubeJS data**  
   - Path: `…\minecraft\kubejs\data\`  
   - Renamed: `ZAfor1.7.zip` → **`ZAfor1.7.zip.disabled`**

3. **ATM x MSD** (all four locations — the pack is loaded as both data and resource pack)  
   - `…\minecraft\datapacks\`  
     - Renamed: `ATM x MSD [3.1.2].zip` → **`ATM x MSD [3.1.2].zip.disabled`**  
   - `…\minecraft\global_packs\required_data\`  
     - Renamed: `ATM x MSD [3.1.2].zip` → **`ATM x MSD [3.1.2].zip.disabled`**  
   - `…\minecraft\global_packs\optional_data\`  
     - Renamed: `ATM x MSD [3.1.2].zip` → **`ATM x MSD [3.1.2].zip.disabled`**  
   - **`…\minecraft\resourcepacks\`** ← this was still active and caused the crash  
     - Renamed: `ATM x MSD [3.1.2].zip` → **`ATM x MSD [3.1.2].zip.disabled`**

4. **World-specific datapacks (why “turned off in game” didn’t fix it)**  
   Turning off a pack in **Options → Resource Packs** only affects which resource packs are used for textures/sounds. It does **not** stop:
   - **Global Packs mod** – In `config/global_packs.toml` it is set to load **`resourcepacks/`** and **`global_packs/required_data/`** as **required** data packs. So every zip in those folders is loaded as data when you load any world, regardless of the resource pack menu.
   - **World datapacks** – Each world can have its own `saves/<WorldName>/datapacks/` folder. Packs there are loaded when you load that world and are **not** controlled by the resource pack menu.  
   So the crash can still happen if:
   - A zip is still in `resourcepacks/` or `global_packs/required_data/` (we disabled those), or  
   - A zip is inside a **world’s** `datapacks/` folder.  
   **Applied:** Renamed **`saves/RUBIUS_STIKES/datapacks/ZAfor1.7.zip`** → **`ZAfor1.7.zip.disabled`**. If you use another world that had these packs enabled, check `saves/<WorldName>/datapacks/` and add `.disabled` to any ZA or ATM MSD zip there.

5. **Clean up ZAfor1.7 references (optional)**  
   In **Options → Resource Packs**, remove **ZAfor1.7.zip** if it still appears. You can also edit:
   - `options.txt` – remove `"file/ZAfor1.7.zip"` from the `resourcePacks` list  
   - `config/defaultoptions/options.txt` – same  
   - `config/resourcepackoverrides.json` – remove the line `"file/ZAfor1.7.zip"` from the `default_packs` array

After this, **create/load a world again**. The crash should stop; you’ll lose the Z-A megas (and possibly ATM MSD megas) until you use an updated pack or Fix 2.

---

## Fix 2: Keep Z-A megas (update the JSONs)

To keep the Z-A (and ATM MSD) content and fix the crash:

- **Preferred:** Get an **updated** “Z-A Mega’s Early” (and ATM x MSD) pack that explicitly supports **Mega Showdown 1.6.4+** / Cobblemon 1.7.2 (check Modrinth/CurseForge/Discord for “Mega Showdown 1.6” or “aspect_conditions”).
- **Otherwise:** Someone has to **convert all mega JSONs** from the old `aspect` format to the new `aspect_conditions` format (and remove or re‑map `blacklist_aspects_apply` / `required_aspects_apply` if the new format doesn’t support them the same way). The conversion is:
  - `aspect.apply_aspects` → `aspect_conditions.apply.aspects`
  - `aspect.revert_aspects` → `aspect_conditions.revert.aspects`
  - Remove the old `aspect` block and use only `aspect_conditions` as above.

If you still have **Z-A Mega’s Early.zip** (e.g. in Downloads), you could:
- Extract it, convert every `mega_showdown/mega_showdown/mega/*.json` file to the new format, then repack as a datapack or put inside a mod’s `data` folder, **or**
- Wait for an official/compatible “Z-A for Mega Showdown 1.6.4” pack.

---

## Summary

| What | Action |
|------|--------|
| **Crash** | Caused by old mega JSON format in Z-A / zamega / ATM MSD packs; Mega Showdown 1.6.4+ requires `aspect_conditions`. |
| **Quick fix** | Remove `zamega-neoforge-1.4.7.jar` from `mods/`. If needed, also remove/disable **ATM x MSD [3.1.2].zip** from datapacks and global_packs. |
| **Proper fix** | Use packs updated for Mega Showdown 1.6.4+, or convert all mega JSONs to the new `aspect_conditions` format. |

Once only packs with the new format (or no custom mega JSONs) are loaded, the “mega_showdown” registry loads correctly and the crash goes away.
