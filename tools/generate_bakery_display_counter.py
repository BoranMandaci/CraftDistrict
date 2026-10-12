"""Editable bakery counter asset source; run with Python 3, no third-party packages.

Generates only Bakery Display Counter resources. Edit dimensions/palettes here and
rerun, or edit the exported vanilla JSON models in Blockbench. Rerunning replaces
those exports. Model units are sixteenths of a block; the front faces north (-Z).
"""

import copy
import json
from pathlib import Path
import struct
import zlib


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/craftdistrict"
TEXTURES = ASSETS / "textures/block/bakery"
MODELS = ASSETS / "models/block/bakery"
ID = "bakery_display_counter"
FACES = ("down", "up", "north", "south", "west", "east")


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def png(name, pixel):
    """Write exact 16x16 RGBA pixel art without resampling or external tools."""
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    rows = b"".join(b"\0" + bytes(c for x in range(16) for c in pixel(x, y)) for y in range(16))
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b"")
    TEXTURES.mkdir(parents=True, exist_ok=True)
    (TEXTURES / f"{ID}_{name}.png").write_bytes(data)


def grain(x, y, dark=False):
    # Horizontal boards, staggered joins, and restrained two-pixel grain marks.
    colors = ((133, 87, 46), (153, 103, 57), (171, 121, 69), (184, 134, 80))
    if dark:
        colors = ((84, 50, 30), (104, 65, 35), (121, 79, 43), (135, 91, 50))
    index = 1 + ((x // 3 + y * 5 + x * y) % 3)
    if y in (7, 15) or (x == (3 if y < 8 else 11) and y % 8 < 3):
        index = 0
    elif (x + y * 3) % 13 < 2:
        index = 1
    return (*colors[index], 255)


def crust(x, y, top=False):
    colors = ((157, 87, 30), (183, 108, 44), (205, 137, 60), (224, 162, 87))
    index = (x * 7 + y * 3 + x * y) % 4
    if top and any(abs(x - (3 + slit * 5 + (y % 6) // 2)) <= 1 for slit in range(3)):
        if 3 <= y <= 12:
            return (245, 199, 128, 255)
    return (*colors[index], 255)


def pastry(x, y, top=False):
    if top:
        radius = max(abs(x - 7), abs(y - 7))
        if radius in (1, 4, 6):
            return (115, 64, 30, 255)
        if (x + y) % 11 == 0:
            return (248, 216, 163, 255)
    return ((210, 143, 70, 255), (225, 161, 87, 255), (193, 119, 49, 255))[(x + y * 3) % 3]


def glass(x, y):
    # Fully clear pixels and opaque highlights work with vanilla cutout rendering.
    if (x - y in (4, 5) and 2 <= y <= 6) or (x - y == -7 and 10 <= y <= 12):
        return (204, 230, 231, 255)
    return (204, 230, 231, 0)


def tart_top(x, y):
    # Crust rim, light custard and a small berry center; detail stays in pixels.
    radius = max(abs(x - 7.5), abs(y - 7.5))
    if radius > 5.5:
        return (209, 146, 72, 255)
    if 4 <= x <= 11 and 4 <= y <= 11:
        return ((150, 48, 52, 255), (181, 63, 65, 255))[(x // 2 + y // 2) % 2]
    return (242, 212, 147, 255)


def chocolate(x, y):
    # Glazed eclair top with a restrained lighter stripe.
    if x in (6, 7) and 2 <= y <= 13:
        return (163, 112, 68, 255)
    return ((86, 49, 32, 255), (105, 60, 39, 255))[(x // 3 + y // 4) % 2]


TEXTURE_MAP = {name: f"craftdistrict:block/bakery/{ID}_{name}"
               for name in ("wood", "trim", "bread", "bread_top", "pastry", "pastry_top",
                            "tart_top", "chocolate", "glass", "tray")}
TEXTURE_MAP["particle"] = TEXTURE_MAP["wood"]


def box(name, start, end, texture, seam=None, top=None, only=None):
    faces = {}
    for face in only or FACES:
        if face == seam:
            continue
        # Proportional UVs retain a 16-pixel-per-block scale on the wooden frame.
        if face in ("north", "south"):
            uv = [start[0], 16 - end[1], end[0], 16 - start[1]]
        elif face in ("west", "east"):
            uv = [start[2], 16 - end[1], end[2], 16 - start[1]]
        else:
            uv = [start[0], start[2], end[0], end[2]]
        if texture in ("bread", "pastry", "tray", "glass"):
            uv = [0, 0, 16, 16]
        faces[face] = {"uv": uv, "texture": "#" + (top if face == "up" and top else texture)}
    return {"name": name, "from": start, "to": end, "faces": faces}


def half(primary):
    seam = "east" if primary else "west"
    outer = [0, 1] if primary else [15, 16]
    center = [15.5, 16] if primary else [0, 0.5]
    glass_x = 0.5 if primary else 15.5
    elements = [
        box("wooden_base", [0, 0, 0], [16, 5, 16], "wood", seam),
        # Recess the service panel so the handle stays inside the one-block depth.
        box("wooden_back", [0, 5, 14], [16, 15, 15.5], "wood", seam),
        box("rear_handle_left_mount", [6, 11.25, 15.5], [6.5, 12, 16], "trim", only=("west", "east", "up", "down", "south")),
        box("rear_handle_right_mount", [9.5, 11.25, 15.5], [10, 12, 16], "trim", only=("west", "east", "up", "down", "south")),
        box("rear_handle_grip", [6.5, 11.5, 15.75], [9.5, 12, 16], "trim", only=("up", "down", "north", "south")),
        box("countertop", [0, 15, 0], [16, 16, 16], "trim", seam),
        box("lower_shelf", [0, 5, 1], [16, 6, 14], "wood", seam),
        box("upper_shelf", [0, 10, 1], [16, 10.5, 14], "trim", seam),
        box("window_bottom_rail", [0, 6, 0], [16, 6.75, 1], "trim", seam),
        box("window_top_rail", [0, 14.25, 0], [16, 15, 1], "trim", seam),
        box("outer_front_post", [outer[0], 6.75, 0], [outer[1], 14.25, 1], "trim"),
        box("center_front_post", [center[0], 6.75, 0], [center[1], 14.25, 1], "trim", seam),
        box("front_glass", [1 if primary else 0.5, 6.75, 0.5],
            [15.5 if primary else 15, 14.25, 0.5], "glass", only=("north", "south")),
        box("end_glass", [glass_x, 6.75, 1], [glass_x, 14.25, 14], "glass", only=("west", "east")),
        box("bread_tray", [2, 6, 3], [14, 6.25, 12], "tray"),
        box("pastry_tray", [2, 10.5, 3], [14, 10.75, 12], "tray"),
    ]
    # Different contents per half: eight items, with two or three cuboids at most.
    if primary:
        elements.extend([
            box("baguette_base", [3, 6.25, 3.5], [6, 7.25, 11.5], "bread"),
            box("baguette_crown", [3.5, 7.25, 4], [5.5, 7.75, 11], "bread", top="bread_top", only=("up", "north", "south", "west", "east")),
            box("round_loaf_base", [8, 6.25, 5], [13, 7.5, 10], "bread"),
            box("round_loaf_crown", [8.75, 7.5, 5.75], [12.25, 8.75, 9.25], "bread", top="bread_top", only=("up", "north", "south", "west", "east")),
            box("cinnamon_roll_base", [3, 10.75, 5], [6.5, 11.75, 8.5], "pastry"),
            box("cinnamon_roll_crown", [3.5, 11.75, 5.5], [6, 12.5, 8], "pastry", top="pastry_top", only=("up", "north", "south", "west", "east")),
            box("berry_tart", [9, 10.75, 5], [13, 11.5, 9], "pastry", top="tart_top"),
        ])
    else:
        elements.extend([
            box("sandwich_loaf_base", [3, 6.25, 4], [7.5, 7.75, 11], "bread"),
            box("sandwich_loaf_crown", [3.5, 7.75, 4.5], [7, 8.75, 10.5], "bread", top="bread_top", only=("up", "north", "south", "west", "east")),
            box("small_roll_base", [10, 6.25, 6], [13, 7, 9], "pastry"),
            box("small_roll_crown", [10.5, 7, 6.5], [12.5, 7.75, 8.5], "pastry", top="bread_top", only=("up", "north", "south", "west", "east")),
            box("croissant_center", [4.5, 10.75, 6.5], [7.5, 12.25, 8.5], "pastry", top="bread_top"),
            box("croissant_left_tip", [3, 10.75, 4.5], [4.5, 11.75, 7.5], "pastry", top="bread_top"),
            box("croissant_right_tip", [7.5, 10.75, 4.5], [9, 11.75, 7.5], "pastry", top="bread_top"),
            box("chocolate_eclair", [11, 10.75, 3.5], [13, 12, 10.5], "pastry", top="chocolate"),
        ])
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": TEXTURE_MAP, "elements": elements}


def main():
    png("wood", grain)
    png("trim", lambda x, y: grain(x, y, True))
    png("bread", crust)
    png("bread_top", lambda x, y: crust(x, y, True))
    png("pastry", pastry)
    png("pastry_top", lambda x, y: pastry(x, y, True))
    png("tart_top", tart_top)
    png("chocolate", chocolate)
    png("glass", glass)
    png("tray", lambda x, y: (178, 168, 147, 255) if x in (0, 15) or y in (0, 15)
        else (223, 214, 190, 255))

    primary, extension = half(True), half(False)
    write_json(MODELS / f"{ID}_primary.json", primary)
    write_json(MODELS / f"{ID}_extension.json", extension)
    variants = {}
    for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for is_primary, part in ((True, "primary"), (False, "extension")):
            variants[f"facing={facing},primary={str(is_primary).lower()}"] = {
                "model": f"craftdistrict:block/bakery/{ID}_{part}", "y": angle}
    write_json(ASSETS / f"blockstates/{ID}.json", {"variants": variants})

    # A complete two-part inventory model centered at (8,8,8), inside vanilla bounds.
    item = copy.deepcopy(primary)
    item["elements"] = []
    for model, offset in ((primary, -8), (extension, 8)):
        for source_element in model["elements"]:
            element = copy.deepcopy(source_element)
            element["from"][0] += offset
            element["to"][0] += offset
            item["elements"].append(element)
    item["display"] = {
        "gui": {"rotation": [30, 135, 0], "translation": [0, 0, 0], "scale": [0.42, 0.42, 0.42]},
        "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 180, 0], "scale": [0.4, 0.4, 0.4]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.3, 0.3, 0.3]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.3, 0.3, 0.3]},
    }
    write_json(ASSETS / f"models/item/{ID}.json", item)
    write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{ID}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"craftdistrict:{ID}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    print(f"Generated {ID}: {len(primary['elements'])}/{len(extension['elements'])} cuboids per part, "
          f"{len(TEXTURE_MAP) - 1} textures, 8 blockstate variants.")


if __name__ == "__main__":
    main()
