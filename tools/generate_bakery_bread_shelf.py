"""Generate only the Bakery Bread Shelf assets with Python 3 (standard library).

Reuses the counter's box/JSON helpers and material references without regenerating
or changing its assets. Model units are 1/16 block. Front is north (-Z); primary
is the lower block. Edit shelf levels and named food elements below, then rerun.
Rerunning replaces exported shelf JSON; the models also open in Blockbench.
"""

import copy
import struct
import sys
import zlib

# Import reusable asset helpers without creating a tools/__pycache__ directory.
sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, TEXTURE_MAP, box, crust, write_json


ID = "bakery_bread_shelf"
MATERIALS = {key: value for key, value in TEXTURE_MAP.items()
             if key not in ("glass", "tray", "chocolate")}
MATERIALS["seeded_top"] = f"craftdistrict:block/bakery/{ID}_seeded_top"
MATERIALS["braid_top"] = f"craftdistrict:block/bakery/{ID}_braid_top"
MATERIALS["simit"] = f"craftdistrict:block/bakery/{ID}_simit"
VISIBLE_CROWN_FACES = ("up", "north", "south", "west", "east")


def bread_textures():
    """Small opaque tiles in the counter's existing golden bread palette."""
    seeds = {(3, 3), (9, 2), (13, 5), (6, 7), (2, 10), (11, 10), (7, 13)}

    def pixel(x, y):
        if (x, y) in seeds or (x, y - 1) in seeds:
            return (242, 214, 160, 255)
        return crust(x, y)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    def braid_pixel(x, y):
        # Three broad alternating folds follow the lower, wider braided loaf.
        section = min(2, x * 3 // 16)
        local_x = x - section * 16 / 3
        fold_y = round(4 + local_x * 1.4 if section % 2 == 0 else 12 - local_x * 1.4)
        if y == fold_y:
            return (157, 87, 30, 255)
        if y == fold_y - 1:
            return (245, 199, 128, 255)
        return crust(x, y)

    def simit_pixel(x, y):
        # A warmer toasted crust with sparse cream sesame marks on the perimeter.
        sesame = {(3, 2), (8, 1), (13, 3), (1, 7), (3, 11), (8, 14),
                  (12, 13), (14, 8), (13, 5), (2, 5), (5, 2), (11, 14)}
        if (x, y) in sesame or (x, y - 1) in sesame:
            return (242, 214, 160, 255)
        colors = ((178, 101, 35, 255), (192, 114, 42, 255), (207, 132, 55, 255))
        return colors[(x // 3 + y // 2) % 3]

    for suffix, sample in (("seeded_top", pixel), ("braid_top", braid_pixel), ("simit", simit_pixel)):
        rows = b"".join(b"\0" + bytes(c for x in range(16) for c in sample(x, y)) for y in range(16))
        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b"")
        path = ASSETS / f"textures/block/bakery/{ID}_{suffix}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(png)


# Deck heights and depths are the source of the new stepped rack silhouette.
# All coordinates below are in the complete two-block stand's [0,32] space.
# Rotate a complete tray and its contents together so every item rests on wood.
TIERS = ((3.0, 2.0, 14.0), (11.25, 3.5, 14.0), (20.75, 5.0, 14.0))


def rack_box(name, start, end, material, **kwargs):
    element = box(name, start, end, material, **kwargs)
    # Wood uses proportional UVs without negative V coordinates above y=16.
    # Food tops use the counter's complete material tile, as before.
    if material in ("wood", "trim"):
        for face_name, face in element["faces"].items():
            if face_name in ("north", "south"):
                face["uv"] = [start[0], 0, end[0], min(end[1] - start[1], 16)]
            elif face_name in ("west", "east"):
                face["uv"] = [start[2], 0, end[2], min(end[1] - start[1], 16)]
    elif material == "simit":
        for face in element["faces"].values():
            face["uv"] = [0, 0, 16, 16]
    return element


def tilted(element, origin, angle=-22.5):
    element["rotation"] = {"origin": list(origin), "axis": "x", "angle": angle, "rescale": False}
    return element


def portion(element, anchor, scale, shift=(0, 0, 0)):
    """Resize a whole product around its floor anchor before applying tray tilt.

    Every component in a product uses the same transform, preserving its joins,
    ring opening and UV composition. Crates and rotation pivots remain unchanged.
    """
    for corner in ("from", "to"):
        element[corner] = [round(anchor[i] + (element[corner][i] - anchor[i]) * scale[i] + shift[i], 6)
                           for i in range(3)]
    return element


def tray(index):
    y, front, rear = TIERS[index]
    floor = y + 0.5
    origin = (8, floor, (front + rear) / 2)
    prefix = f"tier_{index + 1}"
    elements = [
        rack_box(f"{prefix}_crate_floor", [1.75, y, front], [14.25, floor, rear], "wood"),
        rack_box(f"{prefix}_front_retaining_rail", [1.75, floor, front - 0.5], [14.25, floor + 0.875, front], "trim"),
        rack_box(f"{prefix}_left_crate_rim", [1.75, floor, front], [2.25, floor + 0.875, rear], "wood"),
        rack_box(f"{prefix}_right_crate_rim", [13.75, floor, front], [14.25, floor + 0.875, rear], "wood"),
        rack_box(f"{prefix}_rear_crate_rim", [2.25, floor, rear - 0.5], [13.75, floor + 1.125, rear], "wood"),
    ]
    if index == 0:
        # A wider octagonal footprint and low dome read as a round loaf.
        elements.extend([
            rack_box("boule_middle", [3.25, floor, 5], [7.75, floor + 1.5, 10.5], "bread"),
            rack_box("boule_left", [2.75, floor, 6], [3.25, floor + 1.5, 9.5], "bread", seam="east"),
            rack_box("boule_right", [7.75, floor, 6], [8.25, floor + 1.5, 9.5], "bread", seam="west"),
            rack_box("boule_crown", [3.5, floor + 1.5, 5.5], [7.5, floor + 2.5, 10], "bread", top="seeded_top", only=VISIBLE_CROWN_FACES),
            rack_box("boule_top", [4, floor + 2.5, 6.25], [7, floor + 3.25, 9.25], "bread", top="seeded_top", only=VISIBLE_CROWN_FACES),
            # A low crescent opens toward the customer, unlike the domed roll.
            rack_box("crescent_middle", [10, floor, 5.5], [12.25, floor + 1.5, 7.75], "pastry", top="bread_top"),
            rack_box("crescent_left_tip", [9, floor, 4.5], [10, floor + 0.75, 6.5], "pastry", top="bread_top"),
            rack_box("crescent_right_tip", [12.25, floor, 4.5], [13.25, floor + 0.75, 6.5], "pastry", top="bread_top"),
            rack_box("rear_roll_base", [10.25, floor, 9.25], [13.25, floor + 1.25, 12.25], "bread"),
            rack_box("rear_roll_crown", [10.625, floor + 1.25, 9.625], [12.875, floor + 2, 11.875], "bread", top="seeded_top", only=VISIBLE_CROWN_FACES),
            # Bring the cinnamon pastry down to fill the front-left pocket.
            rack_box("bottom_cinnamon_pastry", [3, floor, 2.75], [6, floor + 1.25, 4.75], "pastry"),
            rack_box("bottom_cinnamon_crown", [3.5, floor + 1.25, 3.125], [5.5, floor + 2, 4.375], "pastry", top="pastry_top", only=VISIBLE_CROWN_FACES),
        ])
    elif index == 1:
        # A long baguette complements an asymmetric front/back pair of rings.
        elements.extend([
            rack_box("left_baguette_body", [2.75, floor, 4.75], [5.75, floor + 1, 12.25], "bread"),
            rack_box("left_baguette_front_tip", [3.375, floor, 4], [5.125, floor + 0.75, 4.75], "bread", seam="south", top="bread_top"),
            rack_box("left_baguette_rear_tip", [3.375, floor, 12.25], [5.125, floor + 0.75, 13], "bread", seam="north", top="bread_top"),
            rack_box("left_baguette_scored_crown", [3.125, floor + 1, 5.25], [5.375, floor + 1.875, 11.75], "bread", top="bread_top", only=VISIBLE_CROWN_FACES),
        ])
        # Eight connected perimeter pieces leave a real hole, with no backing face.
        # The broad central opening is at x=[9.25,11.75], z=[6.75,10.25].
        perimeter = (
            ("front", 9.25, 11.75, 5, 6.25),
            ("front_left", 8, 9.25, 5.5, 6.75),
            ("front_right", 11.75, 13, 5.5, 6.75),
            ("left", 7.5, 8.75, 6.75, 10.25),
            ("right", 12.25, 13.5, 6.75, 10.25),
            ("back_left", 8, 9.25, 10.25, 11.5),
            ("back_right", 11.75, 13, 10.25, 11.5),
            ("back", 9.25, 11.75, 10.75, 12),
        )
        for position in ("front", "rear"):
            for name, x0, x1, z0, z1 in perimeter:
                element = rack_box(f"simit_{position}_{name}", [x0, floor, z0], [x1, floor + 0.875, z1], "simit")
                # One continuous sesame tile per ring; both centers stay open.
                element["faces"]["up"]["uv"] = [(x0 - 7.5) * 16 / 6, (z0 - 5) * 16 / 7,
                                                (x1 - 7.5) * 16 / 6, (z1 - 5) * 16 / 7]
                elements.append(element)
    else:
        # Three broad low folds form one elongated braid, rather than four towers.
        for lobe, x0 in enumerate((2.75, 5.75, 8.75)):
            z0 = 8.25 if lobe % 2 == 0 else 7.25
            height = 2.5 if lobe == 1 else 2.25
            crown_left = 0.375 if lobe == 0 else 0.125
            crown_right = 2.625 if lobe == 2 else 2.875
            elements.extend([
                rack_box(f"braid_lobe_{lobe}_body", [x0, floor, z0], [x0 + 3, floor + 1.25, z0 + 3.5], "bread"),
                rack_box(f"braid_lobe_{lobe}_crown", [x0 + crown_left, floor + 1.25, z0 + 0.375],
                         [x0 + crown_right, floor + height, z0 + 3.125], "bread", top="braid_top", only=VISIBLE_CROWN_FACES),
            ])
            elements[-1]["faces"]["up"]["uv"] = [lobe * 16 / 3, 0, (lobe + 1) * 16 / 3, 16]
        elements.extend([
            rack_box("top_flat_berry_slice", [9, floor, 5.25], [13, floor + 0.75, 7.25], "pastry", top="tart_top"),
        ])
    # Portions are balanced as groups, with bottoms anchored on the crate floor.
    # The two rings stagger toward the rear/right without overlapping or stacking.
    # A smaller rear ring leaves a gap between products and room inside the rims.
    for element in elements:
        name = element["name"]
        if name.startswith("simit_front_"):
            portion(element, (10.5, floor, 8.5), (0.75, 6 / 7, 0.75), (-1, 0, -2))
        elif name.startswith("simit_rear_"):
            portion(element, (10.5, floor, 8.5), (2 / 3, 6 / 7, 17 / 28), (0.5, 0, 2.875))
        elif name.startswith("boule_"):
            portion(element, (5.5, floor, 7.75), (0.9, 0.95, 0.9))
        elif name.startswith("braid_lobe_"):
            portion(element, (7.25, floor, 9.5), (0.9, 0.95, 0.95))
        elif name.startswith("left_baguette_"):
            portion(element, (4.25, floor, 8.5), (1, 1, 0.95))
    return [tilted(element, origin) for element in elements]


def move_y(element, amount):
    """Move geometry AND its rotation pivot between whole-stand/block/item space."""
    element = copy.deepcopy(element)
    for key in ("from", "to"):
        element[key][1] = round(element[key][1] + amount, 6)
    if "rotation" in element:
        element["rotation"]["origin"][1] += amount
    return element


def part(lower):
    elements = []
    y0, y1 = (0.75, 16) if lower else (16, 29.5)
    seam = "up" if lower else "down"
    for side, x0, x1 in (("left", 0.75, 1.75), ("right", 14.25, 15.25)):
        elements.append(rack_box(f"{side}_rear_upright", [x0, y0, 13], [x1, y1, 14.5], "trim", seam=seam))
        # Sloped side supports rise toward the rear instead of outlining a cabinet.
        # Split along the beam's local plane near the block seam. Adjacent ends
        # share a pivot and omit their hidden end faces, so the beam stays continuous.
        leg_y0, leg_y1 = (0.25, 17.25) if lower else (17.25, 29)
        element = rack_box(f"{side}_sloping_support", [x0, leg_y0, 1.75], [x1, leg_y1, 2.75], "wood", seam=seam)
        elements.append(tilted(element, (8, 0.25, 2.25), angle=22.5))
        if lower:
            elements.append(rack_box(f"{side}_sled_foot", [x0 - 0.125, 0, 1], [x1 + 0.125, 0.75, 15], "trim"))
    if lower:
        elements.append(rack_box("low_rear_crossbar", [1.75, 1, 13.5], [14.25, 2, 14.25], "wood"))
        elements.extend(tray(0))
        elements.extend(tray(1))
    else:
        # A small raised wooden header replaces the full-depth flat cabinet roof.
        elements.extend([
            rack_box("header_lower_moulding", [1, 27.5, 12.25], [15, 28, 14], "trim"),
            rack_box("header_board", [1.5, 28, 12.5], [14.5, 29.5, 13.75], "wood"),
            rack_box("header_raised_center", [4, 29.5, 12.5], [12, 30.5, 13.75], "wood", seam="down"),
        ])
        elements.extend(tray(2))
    if not lower:
        elements = [move_y(element, -16) for element in elements]
    return {"parent": "minecraft:block/block", "textures": MATERIALS, "elements": elements}


def main():
    bread_textures()
    lower, upper = part(True), part(False)
    write_json(MODELS / f"{ID}_lower.json", lower)
    write_json(MODELS / f"{ID}_upper.json", upper)
    variants = {}
    for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for primary, name in ((True, "lower"), (False, "upper")):
            variants[f"facing={facing},primary={str(primary).lower()}"] = {
                "model": f"craftdistrict:block/bakery/{ID}_{name}", "y": angle}
    write_json(ASSETS / f"blockstates/{ID}.json", {"variants": variants})

    # Center the complete two-block model at y=8; all coordinates stay in [-16,32].
    item = copy.deepcopy(lower)
    item["elements"] = []
    for model, offset in ((lower, -8), (upper, 8)):
        for source in model["elements"]:
            element = move_y(source, offset)
            item["elements"].append(element)
    item["display"] = {
        "gui": {"rotation": [30, 135, 0], "scale": [0.42, 0.42, 0.42]},
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
    print(f"Generated {ID}: {len(lower['elements'])}/{len(upper['elements'])} cuboids, "
          "three tilted crate tiers, eight state variants; reused bakery materials.")


if __name__ == "__main__":
    main()
