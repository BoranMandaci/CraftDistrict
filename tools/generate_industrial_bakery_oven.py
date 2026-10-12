"""Editable static oven asset source. Run with Python 3, standard library only.

Author the whole 32 x 32 x 16 oven below, facing north (-Z). Export clips it into
four ordinary local block models, cropping UVs and omitting new internal faces.
Does not regenerate either wooden decoration. Model units are 1/16 block.
"""

import copy
import sys
import struct
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, TEXTURE_MAP, write_json

ID = "industrial_bakery_oven"
FACES = ("north", "south", "west", "east", "up", "down")
TILES = ("metal", "steel", "stone", "interior", "service", "vent", "glass", "control_upper", "control_lower")
MATERIALS = {name: f"craftdistrict:block/bakery/{ID}_{name}" for name in TILES}
MATERIALS.update({name: TEXTURE_MAP[name] for name in ("bread", "bread_top")})
MATERIALS["particle"] = MATERIALS["metal"]


def tile(name, sample):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    rows = b"".join(b"\0" + bytes(c for x in range(32) for c in sample(x, y)) for y in range(32))
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 32, 32, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b"")
    path = ASSETS / f"textures/block/bakery/{ID}_{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def panel_pixel(x, y, temperature):
    # Three pixel-font digits and indicator lights; detail is in the tile, not cubes.
    digits = {"0": ("111", "101", "101", "101", "111"),
              "1": ("010", "110", "010", "010", "111"),
              "2": ("111", "001", "111", "100", "111"),
              "9": ("111", "101", "111", "001", "111")}
    if x in (0, 31) or y in (0, 31):
        return (119, 127, 129, 255)
    if 3 <= x <= 28 and 3 <= y <= 17:
        for index, digit in enumerate(temperature):
            dx, dy = x - (5 + index * 8), y - 6
            if 0 <= dx < 6 and 0 <= dy < 10 and digits[digit][dy // 2][dx // 2] == "1":
                return (176, 200, 122, 255)
        return (21, 31, 31, 255)
    if 5 <= x <= 8 and 22 <= y <= 25:
        return (172, 181, 106, 255)
    if 13 <= x <= 16 and 22 <= y <= 25:
        return (179, 115, 65, 255)
    if y in (28, 29) and 4 <= x <= 26:
        return (94, 103, 107, 255)
    return (55, 62, 67, 255)


def textures():
    def metal(x, y):
        shade = (57, 62, 66)[(x // 4 + y // 3) % 3]
        return (shade, shade + 5, shade + 9, 255)
    def steel(x, y):
        shade = (149, 157, 165, 172)[(y // 2 + x // 11) % 4]
        return (shade, shade + 5, shade + 7, 255)
    def service(x, y):
        # Framed gasket, stamped ribs and tiny slotted fasteners stay in pixels.
        if x in (1, 30) or y in (1, 30):
            return (111, 119, 122, 255)
        if x in (2, 29) or y in (2, 29):
            return (43, 49, 54, 255)
        for sx, sy in ((4, 4), (27, 4), (4, 27), (27, 27)):
            if sx <= x <= sx+1 and sy <= y <= sy+1:
                return (73, 82, 87, 255) if x-sx == y-sy else (156, 164, 166, 255)
        if y == 15:
            return (43, 49, 54, 255)
        if y == 16 or (x in (8, 23) and 6 <= y <= 25):
            return (78, 86, 89, 255)
        return metal(x, y)
    def vent(x, y):
        if 2 <= x <= 29 and y % 6 in (2, 3):
            return (23, 28, 31, 255)
        return steel(x, y)
    def glass(x, y):
        # Binary alpha keeps the windows on the vanilla cutout layer on every loader.
        # Dark grid and short blue-gray highlights; holes reveal real static contents.
        if x in (0, 1, 30, 31) or y in (0, 1, 30, 31) or (x + 2 * y) % 7 == 0:
            return (39, 54, 61, 255)
        if x - y in (4, 5) and 5 <= y <= 10:
            return (115, 143, 150, 255)
        return (39, 54, 61, 0)
    for name, sample in (
        ("metal", metal), ("steel", steel), ("service", service), ("vent", vent), ("glass", glass),
        ("stone", lambda x, y: ((91, 93, 89, 255), (104, 106, 102, 255), (115, 117, 112, 255))[(x // 3 + y // 2) % 3]),
        ("interior", lambda x, y: (27 + y % 3, 30 + y % 3, 32 + y % 3, 255)),
        ("control_upper", lambda x, y: panel_pixel(x, y, "220")),
        ("control_lower", lambda x, y: panel_pixel(x, y, "190")),
    ):
        tile(name, sample)


def box(name, start, end, material, full_tile=False, only=FACES, top=None):
    x0, y0, z0 = start
    x1, y1, z1 = end
    # 32px sheet over 32 model units retains one pixel per 1/16 block.
    uv = {"north": [x0 / 2, (32-y1) / 2, x1 / 2, (32-y0) / 2],
          "south": [(32-x1) / 2, (32-y1) / 2, (32-x0) / 2, (32-y0) / 2],
          "west": [(16-z1) / 2, (32-y1) / 2, (16-z0) / 2, (32-y0) / 2],
          "east": [z0 / 2, (32-y1) / 2, z1 / 2, (32-y0) / 2],
          "up": [x0 / 2, z0 / 2, x1 / 2, z1 / 2],
          "down": [x0 / 2, (16-z1) / 2, x1 / 2, (16-z0) / 2]}
    return {"name": name, "from": start, "to": end, "faces": {
        f: {"texture": "#" + (top if f == "up" and top else material),
            "uv": [0, 0, 16, 16] if full_tile else uv[f]} for f in only}}


def whole_oven():
    e = [
        box("stone_plinth", [.5, 0, 1], [31.5, 2, 15.5], "stone"),
        box("steel_toe_rail", [.75, 2, .75], [31.25, 3, 15.25], "steel"),
        box("left_body", [.75, 3, 2.5], [2.5, 31, 15], "metal"),
        box("right_body", [29.5, 3, 2.5], [31.25, 31, 15], "metal"),
        box("rear_body", [2.5, 3, 14], [29.5, 31, 15.5], "metal"),
        box("dark_chamber_back", [2.5, 3, 13.5], [24.75, 28, 14], "interior"),
        box("control_column", [24.75, 3, 2], [29.5, 28, 14], "steel"),
        box("chamber_separator", [2.5, 14.5, 2.25], [24.75, 16.5, 14], "metal"),
        box("lower_apron", [2.5, 3, 2], [24.75, 4.25, 14], "metal"),
        box("upper_vent_housing", [2.5, 27, 2.25], [29.5, 31, 15], "metal"),
        box("upper_vent_grille", [3.5, 28.25, 2], [28.5, 30.5, 2.25], "vent", True),
        box("top_lip", [.5, 31, 1.75], [31.5, 32, 15.5], "steel"),
        box("left_service_panel", [.5, 5, 5], [.75, 27, 13.5], "service", True),
        box("right_service_panel", [31.25, 5, 5], [31.5, 27, 13.5], "service", True),
        box("left_side_vent", [.375, 22, 7], [.5, 25.5, 12.5], "vent", True),
        box("right_side_vent", [31.5, 22, 7], [31.625, 25.5, 12.5], "vent", True),
        box("rear_service_panel", [3.5, 5, 15.5], [28.5, 28, 15.75], "service", True),
        box("rear_lower_vent", [7, 6, 15.75], [25, 9, 16], "vent", True),
        box("rear_upper_vent", [9, 24, 15.75], [23, 27, 16], "vent", True),
        # Shallow frame rails and side straps add relief within the existing depth.
        box("rear_panel_left_rail", [3, 5, 15.5], [3.5, 28, 15.875], "steel"),
        box("rear_panel_right_rail", [28.5, 5, 15.5], [29, 28, 15.875], "steel"),
        box("rear_panel_bottom_rail", [3.5, 4.5, 15.5], [28.5, 5, 15.875], "steel"),
        box("rear_panel_top_rail", [3.5, 28, 15.5], [28.5, 28.5, 15.875], "steel"),
        box("left_side_reinforcing_strap", [.375, 14.75, 5], [.5, 15.25, 13.5], "steel"),
        box("right_side_reinforcing_strap", [31.5, 14.75, 5], [31.625, 15.25, 13.5], "steel"),
    ]
    for label, y0, y1 in (("lower", 4.25, 14.5), ("upper", 16.5, 27)):
        e.extend([
            box(f"{label}_left_frame", [2.75, y0, 1.75], [4.25, y1, 3.25], "steel"),
            box(f"{label}_right_frame", [23, y0, 1.75], [24.5, y1, 3.25], "steel"),
            box(f"{label}_bottom_frame", [4.25, y0, 1.75], [23, y0+1.25, 3.25], "steel"),
            box(f"{label}_top_frame", [4.25, y1-1.5, 1.75], [23, y1, 3.25], "steel"),
            box(f"{label}_dark_window", [4.25, y0+1.25, 2.375], [23, y1-1.5, 2.375], "glass", True, ("north", "south")),
            box(f"{label}_handle_left_mount", [4.875, y1-2.125, .75], [5.75, y1-1.25, 1.75], "metal"),
            box(f"{label}_handle_right_mount", [19.25, y1-2.125, .75], [20.125, y1-1.25, 1.75], "metal"),
            # A slight overlap onto dark glass separates the grip from its steel frame.
            box(f"{label}_steel_handle", [5, y1-2, .125], [20, y1-1.25, .75], "steel"),
            box(f"{label}_hinge", [2.5, y0+3, 1.5], [2.75, y0+5, 2.75], "metal"),
            box(f"{label}_temperature_panel", [25.25, y0+1.5, 1.75], [29, y1-1.25, 2], f"control_{label}", True),
            box(f"{label}_control_knob", [27.75, y0+2.25, 1.25], [28.75, y0+3.25, 1.75], "metal"),
        ])
    # Keep seven products: vary their profiles rather than filling the chambers.
    # x, z, width, depth, body height, silhouette.
    products = (
        ((6, 5, 5, 5, 1.5, "round"), (15, 6, 5, 5, 1.375, "tin")),
        ((6, 5, 3.5, 6, 1.25, "long"), (15, 6, 5.5, 4.5, 1.125, "flat")),
        ((5, 5, 3, 7, 1.25, "long"), (11, 5.25, 3.25, 6.5, 1.5, "tin"),
         (17, 6, 4.25, 4.75, 1.75, "round")),
    )
    for index, y in enumerate((5.5, 9.5, 18.25)):
        e.append(box(f"baking_tray_{index}", [4.25, y, 4], [23, y+.375, 12.5], "steel"))
        for product, (x, z, width, depth, height, shape) in enumerate(products[index]):
            floor = y+.375
            name = f"tray_{index}_bread_{product}"
            inset = .5 if shape == "round" else 0
            crown_x = .875 if shape == "round" else .25 if shape == "long" else .5
            crown_z = .75 if shape == "round" else 1 if shape == "long" else .5
            crown_height = .75 if shape == "round" else .45 if shape == "flat" else .5
            e.extend([
                box(name, [x+inset, floor, z], [x+width-inset, floor+height, z+depth], "bread", True),
                box(f"{name}_crown", [x+crown_x, floor+height, z+crown_z],
                    [x+width-crown_x, floor+height+crown_height, z+depth-crown_z], "bread", True,
                    ("up", "north", "south", "west", "east"), top="bread_top"),
            ])
            if shape == "round":
                # Two shoulders make a clipped-corner footprint without tiny cubes.
                e.extend([
                    box(f"{name}_left_shoulder", [x, floor, z+.75], [x+.5, floor+height, z+depth-.75],
                        "bread", True, tuple(f for f in FACES if f != "east")),
                    box(f"{name}_right_shoulder", [x+width-.5, floor, z+.75], [x+width, floor+height, z+depth-.75],
                        "bread", True, tuple(f for f in FACES if f != "west")),
                ])
    return e


def clip(element, column, row):
    """Crop geometry and face UVs at block boundaries, without duplicate seam faces."""
    bounds0, bounds1 = [column*16, row*16, 0], [(column+1)*16, (row+1)*16, 16]
    a, b = element["from"], element["to"]
    c, d = [max(a[i], bounds0[i]) for i in range(3)], [min(b[i], bounds1[i]) for i in range(3)]
    if c[0] >= d[0] or c[1] >= d[1] or c[2] > d[2]:
        return None
    result = copy.deepcopy(element)
    result["from"] = [c[i]-bounds0[i] for i in range(3)]
    result["to"] = [d[i]-bounds0[i] for i in range(3)]
    # U axis/direction, V axis/direction, normal axis/direction for each face.
    axes = {"north": (0,1,1,-1,2,-1), "south": (0,-1,1,-1,2,1),
            "west": (2,-1,1,-1,0,-1), "east": (2,1,1,-1,0,1),
            "up": (0,1,2,1,1,1), "down": (0,1,2,-1,1,-1)}
    for face, (uaxis, usign, vaxis, vsign, normal, nsign) in axes.items():
        if face not in result["faces"]:
            continue
        if (nsign < 0 and c[normal] != a[normal]) or (nsign > 0 and d[normal] != b[normal]):
            del result["faces"][face]
            continue
        original = element["faces"][face]["uv"]
        uv = []
        for axis, sign, start_uv, end_uv in ((uaxis,usign,original[0],original[2]), (vaxis,vsign,original[1],original[3])):
            length = b[axis]-a[axis]
            low = (c[axis]-a[axis])/length if sign > 0 else (b[axis]-d[axis])/length
            high = (d[axis]-a[axis])/length if sign > 0 else (b[axis]-c[axis])/length
            uv.append((start_uv+(end_uv-start_uv)*low, start_uv+(end_uv-start_uv)*high))
        # Matching cut edges serialize identically, even for fractional panel UVs.
        result["faces"][face]["uv"] = [round(v, 6) for v in (uv[0][0], uv[1][0], uv[0][1], uv[1][1])]
    return result


def main():
    textures()
    elements = whole_oven()
    variants = {}
    counts = []
    for part in range(4):
        column, row = part % 2, part // 2
        pieces = [piece for e in elements if (piece := clip(e, column, row)) is not None]
        counts.append(len(pieces))
        write_json(MODELS / f"{ID}_{part}.json", {"parent":"minecraft:block/block", "render_type":"minecraft:cutout",
                                               "textures":MATERIALS, "elements":pieces})
        for facing, angle in (("north",0),("east",90),("south",180),("west",270)):
            variants[f"facing={facing},part={part}"] = {"model":f"craftdistrict:block/bakery/{ID}_{part}", "y":angle}
    write_json(ASSETS / f"blockstates/{ID}.json", {"variants":variants})
    # Item uses unsplit source geometry centered on the usual (8,8,8) model pivot.
    for e in elements:
        for corner in ("from", "to"):
            e[corner] = [e[corner][0]-8, e[corner][1]-8, e[corner][2]]
    write_json(ASSETS / f"models/item/{ID}.json", {"parent":"minecraft:block/block", "render_type":"minecraft:cutout",
        "textures":MATERIALS, "elements":elements, "display":{
            "gui":{"rotation":[30,135,0], "scale":[.4,.4,.4]},
            "ground":{"translation":[0,3,0], "scale":[.25,.25,.25]},
            "fixed":{"rotation":[0,180,0], "scale":[.4,.4,.4]},
            "thirdperson_righthand":{"rotation":[75,45,0], "translation":[0,2.5,0], "scale":[.3,.3,.3]},
            "thirdperson_lefthand":{"rotation":[75,45,0], "translation":[0,2.5,0], "scale":[.3,.3,.3]},
            "firstperson_righthand":{"rotation":[0,45,0], "scale":[.3,.3,.3]},
            "firstperson_lefthand":{"rotation":[0,225,0], "scale":[.3,.3,.3]}}})
    write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{ID}.json", {
        "type":"minecraft:block", "pools":[{"rolls":1, "entries":[{"type":"minecraft:item", "name":f"craftdistrict:{ID}"}],
        "conditions":[{"condition":"minecraft:survives_explosion"}]}]})
    print(f"Generated {ID}: {len(elements)} source cuboids, {counts} per block part, 16 states, {len(TILES)} new textures.")


if __name__ == "__main__":
    main()
