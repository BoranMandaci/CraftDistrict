"""Editable static commercial mixer, north (-Z) is the front; units are 1/16 block.

Author the complete 16 x 32 x 16 machine here. Export splits at y=16, crops
UVs, moves rotation pivots, and removes internal cut faces. Existing oven
materials are referenced read-only. No other decoration assets are generated.
"""

import copy
import math
import struct
import sys
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, write_json
from generate_industrial_bakery_oven import clip
from generate_flour_sacks import bake_y_quarter_turn, omit_hidden_faces, rounded_prism, validate_model_elements

ID = "industrial_dough_mixer"
FACES = ("north", "south", "west", "east", "up", "down")
TILES = ("bowl", "control", "dough", "green", "stop", "red")
MATERIALS = {name: f"craftdistrict:block/bakery/industrial_bakery_oven_{tile}"
             for name, tile in (("metal", "metal"), ("steel", "steel"), ("black", "interior"),
                                ("vent", "vent"), ("service", "service"))}
MATERIALS.update({name: f"craftdistrict:block/bakery/{ID}_{name}" for name in TILES})
MATERIALS["particle"] = MATERIALS["metal"]


def png(name, pixel):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    rows = b"".join(b"\0" + bytes(c for x in range(32) for c in pixel(x, y)) for y in range(32))
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 32, 32, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b"")
    path = ASSETS / f"textures/block/bakery/{ID}_{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def control_pixel(x, y):
    # Asymmetric, upright '02' readout and clockwise speed arrow. Face uses
    # explicit [0,0,16,16] UVs on NORTH, with no reversal or face rotation.
    if x in (0, 31) or y in (0, 31):
        return (146, 155, 158, 255)
    if 4 <= x <= 27 and 3 <= y <= 15:
        for sx, glyph in ((7, ("111", "101", "101", "101", "111")),
                          (17, ("111", "001", "111", "100", "111"))):
            dx, dy = x - sx, y - 5
            if 0 <= dx < 6 and 0 <= dy < 10 and glyph[dy // 2][dx // 2] == "1":
                return (183, 204, 137, 255)
        return (20, 32, 29, 255)
    if (x, y) in {(20, 20), (21, 20), (22, 20), (23, 20), (24, 21), (25, 22),
                  (25, 23), (25, 24), (23, 23), (24, 24), (25, 25), (26, 24), (27, 23)}:
        return (181, 187, 188, 255)
    return (49, 57, 62, 255)


def textures():
    png("bowl", lambda x, y: (v := (170, 178, 186, 193)[(y // 2 + x // 12) % 4], v + 4, v + 6, 255))
    png("control", control_pixel)
    png("dough", lambda x, y: (226 - (x * 3 + y * 5) % 7, 207 - (x + y) % 5, 165 - (x + y) % 4, 255))
    png("green", lambda x, y: (204, 216, 198, 255) if 14 <= x <= 17 and 7 <= y <= 24
        else (64 + y % 3, 121 + y % 3, 73 + y % 3, 255))
    png("stop", lambda x, y: (187, 193, 193, 255) if 9 <= x <= 22 and 9 <= y <= 22
        else (27, 32, 34, 255))
    png("red", lambda x, y: (179 + y % 3, 49 + y % 3, 42 + y % 3, 255))


def box(name, a, b, material, full=False, only=FACES):
    x0, y0, z0 = a
    x1, y1, z1 = b
    # Minecraft outward face UVs: NORTH U decreases along world X.
    uv = {"north": [(16-x1)/2, (32-y1)/2, (16-x0)/2, (32-y0)/2],
          "south": [x0/2, (32-y1)/2, x1/2, (32-y0)/2],
          "west": [z0/2, (32-y1)/2, z1/2, (32-y0)/2],
          "east": [(16-z1)/2, (32-y1)/2, (16-z0)/2, (32-y0)/2],
          "up": [x0/2, z0/2, x1/2, z1/2],
          "down": [x0/2, (16-z1)/2, x1/2, (16-z0)/2]}
    return {"name": name, "from": list(a), "to": list(b), "faces": {
        face: {"texture": "#" + material, "uv": [0, 0, 16, 16] if full else uv[face]} for face in only}}


def octagonal_ring(name, y0, y1, radius, thickness, material):
    """Eight joined thin metal walls, with a genuine empty octagonal center.

    Radius is the outer apothem. Rotations above 45 degrees are baked as
    quarter turns, leaving only legal 0/45 degree element rotations.
    Slightly overlapping joins prevent pinholes without coplanar surfaces.
    """
    half = radius * math.tan(math.pi / 8)
    result = []
    for index in range(8):
        e = box(f"{name}_{index}", [8-half-.025, y0, 6.5-radius],
                [8+half+.025, y1, 6.5-radius+thickness], material)
        quarter, diagonal = divmod(index, 2)
        e = bake_y_quarter_turn(e, (8, 0, 6.5), quarter * 90)
        if diagonal:
            e["rotation"] = {"axis": "y", "angle": 45, "origin": [8, 0, 6.5], "rescale": False}
        result.append(e)
    return result


def whole_machine():
    e = []
    for side, x in (("left", 1.5), ("right", 12)):
        for end, z in (("front", 1.5), ("rear", 12)):
            e.append(box(f"{side}_{end}_rubber_foot", [x, 0, z], [x+2.5, .75, z+2.5], "black"))
    e.extend([
        box("cast_base", [1, .75, 1], [15, 3.5, 15], "metal"),
        box("base_steel_edge", [1.25, 3.5, 1.25], [14.75, 4, 14.75], "steel"),
        box("rear_motor_column", [5, 4, 11.75], [11, 26, 15], "metal"),
        box("left_column_edge", [4.625, 4, 11.5], [5, 24.5, 15.125], "steel"),
        box("right_column_edge", [11, 4, 11.5], [11.375, 24.5, 15.125], "steel"),
        box("bowl_lift_pedestal", [6, 4, 6], [10, 10.25, 12], "metal"),
        box("bowl_cradle", [4.5, 9.75, 3.5], [11.5, 11, 10.5], "steel"),
        box("motor_lower_edge", [3.5, 23.5, 3.5], [12.5, 24.25, 15.25], "steel"),
        box("motor_head", [3, 24.25, 3], [13, 29.75, 15], "metal"),
        box("motor_top_cap", [3.5, 29.75, 3.5], [12.5, 31, 14.5], "steel"),
        box("front_panel_frame", [3.75, 25, 2.625], [12.25, 29.25, 3], "steel"),
        box("front_control_panel", [7.5, 25.25, 2.375], [12, 29, 2.625], "control", True, ("north",)),
        box("start_button", [4.25, 27.25, 2.25], [5.25, 28.25, 2.625], "green", True),
        box("stop_button", [5.75, 27.25, 2.25], [6.75, 28.25, 2.625], "stop", True),
        box("emergency_stop_collar", [4.125, 25.375, 2.25], [5.625, 26.75, 2.625], "steel"),
        box("emergency_stop", [4.375, 25.625, 1.875], [5.375, 26.5, 2.25], "red"),
        box("speed_dial", [8.25, 25.75, 1.875], [9.5, 26.75, 2.375], "black"),
        box("dial_pointer", [8.8125, 26.25, 1.8125], [9, 26.6875, 1.875], "steel"),
        box("left_motor_vent", [2.875, 25.5, 6], [3, 28.75, 12.5], "vent", True, ("west",)),
        box("right_motor_vent", [13, 25.5, 6], [13.125, 28.75, 12.5], "vent", True, ("east",)),
        box("rear_motor_vent", [5, 26, 15], [11, 28.75, 15.125], "vent", True, ("south",)),
        box("rear_maintenance_panel", [5.5, 6, 15], [10.5, 20, 15.125], "service", True, ("south",)),
        box("rear_lower_service_seam", [5, 5, 15], [11, 5.375, 15.1875], "steel"),
        box("lift_handle_mount", [11.375, 8, 12.5], [12.25, 9, 13.5], "black"),
        box("bowl_lift_handle", [12.25, 8.5, 9], [12.75, 9, 13.5], "steel"),
        box("hook_drive_collar", [7, 22.625, 5.5], [9, 23.5, 7.5], "black"),
    ])
    e.extend(rounded_prism("bowl_bottom", [4.5, 11, 3], [11.5, 11.6, 10], "y", "bowl"))
    # Stepped expanding bowl: real inside walls, open top, a narrow rolled rim.
    for name, y0, y1, radius, thickness in (
        ("bowl_lower_wall", 11.5, 13, 3.5, .65),
        ("bowl_middle_wall", 13, 16, 4.5, .65),
        ("bowl_upper_wall", 16, 18.5, 5.5, .65),
        ("bowl_rolled_rim", 18.5, 19, 5.75, .55),
    ):
        e.extend(octagonal_ring(name, y0, y1, radius, thickness, "bowl"))
    # Interior stepped ledges close the transitions between wall bands. Only
    # shallow rings are used, never a filled cuboid across the bowl opening.
    e.extend(octagonal_ring("bowl_lower_transition", 12.875, 13.125, 4.5, 1.7, "bowl"))
    e.extend(octagonal_ring("bowl_upper_transition", 15.875, 16.125, 5.5, 1.7, "bowl"))
    e.extend([
        box("dough_body", [6, 11.6, 4.5], [10, 12.6, 8.5], "dough"),
        box("dough_crown", [6.625, 12.6, 5.125], [9.375, 13.25, 7.875], "dough"),
        # An open, descending hook curl; broad enough to read through the cage.
        box("hook_shaft", [7.625, 21, 6.125], [8.375, 22.625, 6.875], "steel"),
        box("hook_upper_bend", [8.25, 20, 6.125], [9, 22, 6.875], "steel"),
        box("hook_right_drop", [9, 18, 6.125], [9.75, 20.5, 6.875], "steel"),
        box("hook_front_connector", [9, 17.5, 4.875], [9.75, 18.25, 6.875], "steel"),
        box("hook_front_arc", [7, 17.5, 4.875], [9.75, 18.25, 5.625], "steel"),
        box("hook_left_return", [6.625, 16.25, 4.875], [7.375, 18.25, 5.625], "steel"),
        box("hook_tip", [6.625, 15.75, 5.375], [7.375, 16.5, 7], "steel"),
    ])
    next(p for p in e if p["name"] == "hook_upper_bend")["rotation"] = {
        "axis": "z", "angle": 45, "origin": [8.625, 21, 6.5], "rescale": False}
    # Sparse partial cage above the bowl: eight rim segments, two side uprights,
    # two crossbars. The front remains open enough to read the hook and dough.
    e.extend(octagonal_ring("guard_rail", 21.25, 21.625, 5.5, .3, "steel"))
    for side, x in (("left", 2.625), ("right", 13)):
        e.append(box(f"guard_{side}_post", [x, 19, 6.25], [x+.375, 21.625, 6.75], "steel"))
    for index, z in enumerate((4.25, 8.75)):
        e.append(box(f"guard_crossbar_{index}", [3.5, 21.375, z], [12.5, 21.75, z+.3], "steel"))
    return e


def shifted(element, dy):
    e = copy.deepcopy(element)
    for key in ("from", "to"):
        e[key][1] = round(e[key][1] + dy, 6)
    if "rotation" in e:
        e["rotation"]["origin"][1] += dy
    return e


def part(elements, row):
    pieces = []
    for e in elements:
        # All seam-crossing rotations are around Y, so clipping is exact. The oven's
        # clip routine only changes V here; all outward-facing U values survive.
        p = clip(e, 0, row)
        if p is not None and p["faces"]:
            if "rotation" in p:
                p["rotation"]["origin"][1] -= row * 16
            pieces.append(p)
    validate_model_elements(pieces)
    return pieces


# Conservative, deliberately filled bowl envelopes keep entities out of the
# machine. The rendered bowl is hollow; thin cage/hook details need no tiny
# collision boxes. All shapes are clipped to their own cell and cached in Java.
COLLISION = (
    (1, 0, 1, 15, 4, 15),
    (4.625, 4, 11.5, 11.375, 24.25, 15.1875),
    (6, 4, 6, 10, 11, 12),
    (4.5, 9.75, 3.5, 11.5, 13, 10.5),
    (4.5, 13, 2, 11.5, 16, 11),
    (3.5, 13, 3, 12.5, 16, 10),
    (4, 16, .75, 12, 21.75, 12.25),
    (2.25, 16, 2.5, 13.75, 21.75, 10.5),
    (3, 23.5, 3, 13, 29.75, 15.25),
    (3.5, 29.75, 3.5, 12.5, 31, 14.5),
    (3.75, 25, 1.8125, 12.25, 29.25, 3),
    (7, 21.75, 5.5, 9, 23.5, 7.5),
    (11.375, 8, 9, 12.75, 9, 13.5),
)


def shape_source():
    parts = []
    for row in range(2):
        boxes = []
        for x0, y0, z0, x1, y1, z1 in COLLISION:
            a, b = max(y0, row*16), min(y1, (row+1)*16)
            if a < b:
                values = (x0, a-row*16, z0, x1, b-row*16, z1)
                boxes.append("                    Block.box(" + ", ".join(f"{v:g}" for v in values) + ")")
        parts.append("            rotations(Shapes.or(\n" + ",\n".join(boxes) + ").optimize())")
    path = ROOT / "src/main/java/com/craftdistrict/block/IndustrialDoughMixerShapes.java"
    path.write_text('''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_industrial_dough_mixer.py; edit COLLISION there. */
final class IndustrialDoughMixerShapes {
    static final VoxelShape[][] SHAPES = {
''' + ",\n".join(parts) + '''
    };

    private IndustrialDoughMixerShapes() {
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] shapes = new VoxelShape[4];
        shapes[0] = north;
        for (int turn = 1; turn < 4; turn++) {
            VoxelShape rotated = Shapes.empty();
            for (AABB box : shapes[turn - 1].toAabbs()) {
                rotated = Shapes.or(rotated, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            shapes[turn] = rotated.optimize();
        }
        return shapes;
    }
}
''', encoding="utf-8")


def main():
    textures()
    elements = whole_machine()
    omit_hidden_faces(elements)
    validate_model_elements(elements)
    variants, counts = {}, []
    for row, name in enumerate(("lower", "upper")):
        pieces = part(elements, row)
        counts.append(len(pieces))
        write_json(MODELS / f"{ID}_{name}.json", {
            "parent": "minecraft:block/block", "textures": MATERIALS, "elements": pieces})
        for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            variants[f"facing={facing},primary={str(row == 0).lower()}"] = {
                "model": f"craftdistrict:block/bakery/{ID}_{name}", "y": angle}
    write_json(ASSETS / f"blockstates/{ID}.json", {"variants": variants})
    write_json(ASSETS / f"models/item/{ID}.json", {
        "parent": "minecraft:block/block", "textures": MATERIALS,
        "elements": [shifted(e, -8) for e in elements], "display": {
            "gui": {"rotation": [30, 135, 0], "scale": [.42, .42, .42]},
            "ground": {"translation": [0, 3, 0], "scale": [.25, .25, .25]},
            "fixed": {"rotation": [0, 180, 0], "scale": [.4, .4, .4]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.3, .3, .3]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.3, .3, .3]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [.3, .3, .3]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [.3, .3, .3]}}})
    write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{ID}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"craftdistrict:{ID}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    shape_source()
    print(f"Generated {ID}: {len(elements)} cuboids, {counts} per part, "
          f"{sum(len(e['faces']) for e in elements)} item faces, 8 states, {len(TILES)} new textures.")


if __name__ == "__main__":
    main()
