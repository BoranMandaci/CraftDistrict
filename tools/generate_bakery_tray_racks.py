"""Stage 1 trolley family, north (-Z) is front; model units are 1/16 block.

One named frame drives BOTH variants. Vanilla multipart blockstates add baked
contents separately. Only rack assets and BakeryTrayRackShapes.java are written.
Existing industrial/bakery materials are referenced without regenerating them.
"""

import copy
import struct
import sys
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, TEXTURE_MAP, write_json
from generate_industrial_dough_mixer import box, part, shifted
from generate_flour_sacks import bounds, cloth_uvs as product_uvs, omit_hidden_faces, place, rounded_prism, validate_model_elements
from generate_bakery_pastry_trays import croissant

IDS = ("bakery_tray_rack_empty", "bakery_tray_rack_baked")
LEVELS = (5, 9.5, 14, 18.5, 23, 27.5)
OCCUPIED = (0, 1, 3, 5)
MATERIALS = {name: f"craftdistrict:block/bakery/industrial_bakery_oven_{tile}"
             for name, tile in (("metal", "metal"), ("steel", "steel"), ("black", "interior"))}
MATERIALS.update({name: TEXTURE_MAP[name] for name in ("bread", "bread_top", "pastry", "tart_top")})
MATERIALS.update({name: f"craftdistrict:block/bakery/bakery_trays_{name}"
                  for name in ("roll", "roll_top", "croissant", "croissant_top")})
MATERIALS["caster"] = "craftdistrict:block/bakery/bakery_tray_rack_caster"
MATERIALS["particle"] = MATERIALS["steel"]


def caster_texture():
    def pixel(x, y):
        # A metal axle hub painted into the tire ends replaces tiny hub cubes.
        if 12 <= x <= 19 and 12 <= y <= 19:
            value = 161 if x in (12, 19) or y in (12, 19) else 110
            return (value, value+5, value+8, 255)
        value = 26 + (x//6+y//5)%3*4
        return (value, value+3, value+5, 255)
    def chunk(kind, data):
        return struct.pack(">I", len(data))+kind+data+struct.pack(">I", zlib.crc32(kind+data))
    rows = b"".join(b"\0"+bytes(c for x in range(32) for c in pixel(x, y)) for y in range(32))
    data = b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR", struct.pack(">IIBBBBB", 32, 32, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(rows, 9))+chunk(b"IEND", b"")
    path = ASSETS / "textures/block/bakery/bakery_tray_rack_caster.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def caster(name, x, z):
    e = []
    # Broad stepped tire silhouette, three cuboids instead of dozens of pixels.
    for segment, y0, y1, half_depth in (("middle", .375, 2.125, 1.25),
                                       ("bottom", 0, .375, .75), ("top", 2.125, 2.5, .75)):
        p = box(f"{name}_tire_{segment}", [x-.5, y0, z-half_depth], [x+.5, y1, z+half_depth], "black")
        z0, z1 = p["from"][2], p["to"][2]
        for face, uv in (("west", [(z0-z+1.25)*6.4, (2.5-y1)*6.4, (z1-z+1.25)*6.4, (2.5-y0)*6.4]),
                         ("east", [(z+1.25-z1)*6.4, (2.5-y1)*6.4, (z+1.25-z0)*6.4, (2.5-y0)*6.4])):
            p["faces"][face] = {"texture": "#caster", "uv": [round(v, 6) for v in uv]}
        e.append(p)
    e.extend([
        box(name+"_fork_left", [x-.75, 1.25, z-.5], [x-.5, 3.75, z+.5], "steel"),
        box(name+"_fork_right", [x+.5, 1.25, z-.5], [x+.75, 3.75, z+.5], "steel"),
        box(name+"_fork_crown", [x-.75, 3.5, z-.5], [x+.75, 4, z+.5], "metal"),
    ])
    return e, ([x-.75, 0, z-1.25], [x+.75, 4, z+1.25])


def frame():
    e, collision = [], []
    for side, x in (("left", 2), ("right", 14)):
        for end, z in (("front", 3), ("rear", 13)):
            pieces, envelope = caster(f"{side}_{end}_caster", x, z)
            e.extend(pieces)
            collision.append(envelope)
    structure = []
    for side, x0 in (("left", 1.5), ("right", 13.5)):
        for end, z0 in (("front", 2.5), ("rear", 12.5)):
            structure.append(box(f"{side}_{end}_upright", [x0, 3.75, z0], [x0+1, 31, z0+1], "steel"))
    for name, y0, y1 in (("lower_chassis", 3.75, 4.5), ("top_frame", 30.5, 31.25)):
        structure.extend([
            box(name+"_front", [2.5, y0, 2.5], [13.5, y1, 3.5], "steel"),
            box(name+"_rear", [2.5, y0, 12.5], [13.5, y1, 13.5], "steel"),
            box(name+"_left", [1.5, y0, 3.5], [2.5, y1, 12.5], "steel"),
            box(name+"_right", [13.5, y0, 3.5], [14.5, y1, 12.5], "steel"),
        ])
    for index, y in enumerate(LEVELS):
        for side, x0 in (("left", 2), ("right", 13)):
            structure.append(box(f"level_{index}_{side}_tray_runner", [x0, y-.375, 2.75], [x0+1, y, 13.25], "metal"))
    structure.extend([
        box("rear_push_handle", [3, 28.5, 14.5], [13, 29.25, 15.25], "metal"),
        box("left_handle_mount", [2.5, 27.5, 13], [3.25, 29.25, 15], "steel"),
        box("right_handle_mount", [12.75, 27.5, 13], [13.5, 29.25, 15], "steel"),
        box("upper_rear_diagonal_brace", [2, 26.625, 13], [14, 27.125, 13.5], "steel"),
    ])
    structure[-1]["rotation"] = {"axis": "z", "angle": 22.5, "origin": [8, 26.875, 13.25], "rescale": False}
    collision.extend(bounds(p) for p in structure)
    e.extend(structure)
    return e, collision


def pan(index, y):
    return [box(f"tray_{index}_floor", [2.375, y, 2.875], [13.625, y+.25, 13.125], "steel"),
            box(f"tray_{index}_front_rim", [2.375, y+.25, 2.875], [13.625, y+.625, 3.25], "steel"),
            box(f"tray_{index}_rear_rim", [2.375, y+.25, 12.75], [13.625, y+.625, 13.125], "steel"),
            box(f"tray_{index}_left_rim", [2.375, y+.25, 3.25], [2.75, y+.625, 12.75], "steel"),
            box(f"tray_{index}_right_rim", [13.25, y+.25, 3.25], [13.625, y+.625, 12.75], "steel")]


def round_bread(name, width, height, loaf=False):
    pieces = rounded_prism(name, [0, 0, 0], [width, height, width], "y", "roll")
    for p in pieces:
        p["faces"]["up"]["texture"] = "#roll_top"
    if loaf:
        pieces.append(box(name+"_domed_crown", [width*.2, height, width*.2],
                          [width*.8, height+.5, width*.8], "roll"))
        pieces[-1]["faces"]["up"]["texture"] = "#roll_top"
    return pieces, (width, height+(.5 if loaf else 0), width)


def baguette(name):
    p = [box(name+"_body", [0, 0, .5], [2.5, 1.2, 6.5], "bread"),
         box(name+"_scored_crown", [.25, 1.2, 1], [2.25, 1.8, 6], "bread"),
         box(name+"_front_tip", [.5, 0, 0], [2, 1.2, .5], "bread"),
         box(name+"_rear_tip", [.5, 0, 6.5], [2, 1.2, 7], "bread")]
    p[1]["faces"]["up"]["texture"] = "#bread_top"
    return p, (2.5, 1.8, 7)


def tart(name):
    p = [box(name+"_middle", [.5, 0, 0], [3.5, .75, 4], "pastry"),
         box(name+"_left", [0, 0, .5], [.5, .75, 3.5], "pastry"),
         box(name+"_right", [3.5, 0, .5], [4, .75, 3.5], "pastry")]
    for e in p:
        e["faces"]["up"]["texture"] = "#tart_top"
    return p, (4, .75, 4)


def add_product(elements, collision, source, position, yaw=0):
    pieces, dimensions = source
    product_uvs(pieces, dimensions)
    placed = [place(p, position, dimensions, yaw) for p in pieces]
    elements.extend(placed)
    limits = [bounds(p) for p in placed]
    collision.append(([min(lo[i] for lo, hi in limits) for i in range(3)],
                      [max(hi[i] for lo, hi in limits) for i in range(3)]))


def baked_contents():
    e, collision = [], []
    for index in OCCUPIED:
        y = LEVELS[index]
        e.extend(pan(index, y))
        # One conservative tray envelope avoids tiny collision boxes for its rims.
        collision.append(([2.375, y, 2.875], [13.625, y+.625, 13.125]))
    y = LEVELS[0]+.25
    for index, (x, z, diameter) in enumerate(((3.25, 4.25, 3), (8.5, 4.75, 3.25), (6, 9, 3))):
        add_product(e, collision, round_bread(f"roll_{index}", diameter, 1.6), (x, y, z))
    y = LEVELS[1]+.25
    add_product(e, collision, baguette("baguette_left"), (4, y, 4.25))
    add_product(e, collision, baguette("baguette_right"), (9, y, 4.75))
    y = LEVELS[3]+.25
    add_product(e, collision, round_bread("round_loaf_left", 4, 1.75, True), (3.25, y, 5), 22.5)
    add_product(e, collision, round_bread("round_loaf_right", 3.75, 1.5, True), (8.5, y, 7))
    y = LEVELS[5]+.25
    pieces, _, dimensions = croissant("croissant")
    add_product(e, collision, (pieces, dimensions), (3.25, y, 5))
    add_product(e, collision, tart("berry_tart"), (8.5, y, 7.5))
    return e, collision


def validate(elements):
    validate_model_elements(elements)
    for e in elements:
        lo, hi = bounds(e)
        assert all(-1e-6 <= lo[i] < hi[i] <= (32 if i == 1 else 16)+1e-6 for i in range(3)), (e["name"], lo, hi)
        assert all(-16 <= v <= 32 for key in ("from", "to") for v in e[key])
        if "rotation" in e:
            assert e["rotation"]["angle"] in (-45, -22.5, 0, 22.5, 45)
        for face in e["faces"].values():
            assert face["texture"][1:] in MATERIALS
            assert all(0 <= v <= 16 for v in face["uv"])


def materials(elements):
    used = {f["texture"][1:] for e in elements for f in e["faces"].values()} | {"particle"}
    return {k: v for k, v in MATERIALS.items() if k in used}


def shape_source(variants):
    rows = []
    for model_id, boxes in zip(IDS, variants):
        parts = []
        for row in range(2):
            cells = []
            for lo, hi in boxes:
                y0, y1 = max(lo[1], row*16), min(hi[1], (row+1)*16)
                if y0 < y1:
                    values = (lo[0], y0-row*16, lo[2], hi[0], y1-row*16, hi[2])
                    cells.append("                    Block.box("+", ".join(f"{v:.6f}".rstrip("0").rstrip(".") for v in values)+")")
            parts.append("            rotations(Shapes.or(\n"+",\n".join(cells)+").optimize())")
        rows.append(f"        // {model_id}\n        {{\n"+",\n".join(parts)+"\n        }")
    source = '''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_bakery_tray_racks.py; edit layouts there. */
final class BakeryTrayRackShapes {
    static final VoxelShape[][][] SHAPES = {
''' + ",\n".join(rows) + '''
    };

    private BakeryTrayRackShapes() {
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
'''
    (ROOT / "src/main/java/com/craftdistrict/block/BakeryTrayRackShapes.java").write_text(source, encoding="utf-8")


def main():
    caster_texture()
    frame_elements, frame_collision = frame()
    contents, content_collision = baked_contents()
    omit_hidden_faces(frame_elements)
    omit_hidden_faces(contents)
    validate(frame_elements)
    validate(contents)
    for prefix, elements in (("bakery_tray_rack_frame", frame_elements), ("bakery_tray_rack_contents", contents)):
        for row, name in enumerate(("lower", "upper")):
            pieces = part(elements, row)
            write_json(MODELS / f"{prefix}_{name}.json", {
                "parent": "minecraft:block/block", "textures": materials(pieces), "elements": pieces})
    for index, model_id in enumerate(IDS):
        entries = []
        for row, name in enumerate(("lower", "upper")):
            for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                for prefix in (("frame", "contents") if index else ("frame",)):
                    entries.append({"when": {"facing": facing, "primary": str(row == 0).lower()},
                                    "apply": {"model": f"craftdistrict:block/bakery/bakery_tray_rack_{prefix}_{name}", "y": angle}})
        write_json(ASSETS / f"blockstates/{model_id}.json", {"multipart": entries})
        elements = copy.deepcopy(frame_elements)+(copy.deepcopy(contents) if index else [])
        write_json(ASSETS / f"models/item/{model_id}.json", {
            "parent": "minecraft:block/block", "textures": materials(elements), "elements": [shifted(e, -8) for e in elements],
            "display": {
                "gui": {"rotation": [30, 135, 0], "scale": [.42, .42, .42]},
                "ground": {"translation": [0, 3, 0], "scale": [.25, .25, .25]},
                "fixed": {"rotation": [0, 180, 0], "scale": [.4, .4, .4]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.3, .3, .3]},
                "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.3, .3, .3]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [.3, .3, .3]},
                "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [.3, .3, .3]}}})
        write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{model_id}.json", {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"craftdistrict:{model_id}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    shape_source((frame_collision, frame_collision+content_collision))
    print(f"Shared frame: {len(frame_elements)} cuboids; baked additions: {len(contents)} cuboids.")
    print("Exactly two variants; six rail levels, four loaded trays, four static casters. One new texture; existing materials referenced read-only.")


if __name__ == "__main__":
    main()
