"""Five independent static signage variants; north (-Z) is outward from the wall.

Python standard library only. Edit named geometry, pixel art and layouts here.
Only this family's resources and BakerySignageShapes.java are generated.
Wall-mounted models reach z=16; the neighboring SOUTH cell is their support.
"""

import copy
import struct
import sys
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, TEXTURE_MAP, write_json
from generate_flour_sacks import bounds, omit_hidden_faces, validate_model_elements

# Keep this order aligned with BakerySignageBlock.Variant. Entries remain separate
# registry IDs, block/item models and loot tables so they can be reviewed individually.
IDS = ("bakery_hanging_shop_sign", "bakery_wall_shop_sign",
       "bakery_menu_board_wall", "bakery_daily_special_board", "bakery_sidewalk_menu_board")
TILES = ("hanging", "shop", "menu", "special", "sidewalk", "iron")
MATERIALS = {name: f"craftdistrict:block/bakery/bakery_signage_{name}" for name in TILES}
MATERIALS.update({name: TEXTURE_MAP[name] for name in ("wood", "trim")})
MATERIALS["particle"] = MATERIALS["wood"]
FACES = ("north", "south", "west", "east", "up", "down")
CHALK = (222, 217, 190, 255)
GOLD = (208, 172, 106, 255)


def png(name, pixel):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    rows = b"".join(b"\0" + bytes(c for x in range(32) for c in pixel(x, y)) for y in range(32))
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 32, 32, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b"")
    path = ASSETS / f"textures/block/bakery/bakery_signage_{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def loaf(x, y, cx, cy, width, height):
    dx, dy = abs(x-cx), y-cy
    if dx > width/2 or abs(dy) > height/2:
        return None
    if abs(dy) > height/2-1 and dx > width/2-2:
        return None
    # Three diagonal scores create a bread motif, not a rectangular brand logo.
    if any(abs(x-(cx-width/3 + i*width/3 + dy*.4)) < .8 for i in range(3)) and abs(dy) < height/2-1:
        return (247, 219, 161, 255)
    return (188, 126, 57, 255) if abs(dy) > height/2-1 else (218, 164, 88, 255)


def wheat(x, y, cx, cy):
    dx, dy = x-cx, y-cy
    return ((dx == 0 and -7 <= dy <= 6) or (abs(dx) == 1 and dy in (-6, -3, 0))
            or (abs(dx) == 2 and dy in (-7, -4, -1)))


def crescent(x, y, cx, cy, scale):
    dx, dy = (x-cx)/scale, (y-cy)/scale
    return dx*dx + dy*dy <= 1 and (dx*.9)**2 + (dy-.6)**2 > .7**2


def pixel(name, x, y):
    if name == "iron":
        shade = (53, 62, 69)[(x//5+y//4)%3]
        return (shade, shade+3, shade+5, 255)
    if name in ("hanging", "shop"):
        if x in (0, 31) or y in (0, 31):
            return (113, 72, 38, 255)
        cx, cy, width, height = (16, 13, 19, 10) if name == "hanging" else (20, 13, 17, 12)
        bread = loaf(x, y, cx, cy, width, height)
        if bread:
            return bread
        if name == "shop" and wheat(x, y, 6, 13):
            return (125, 85, 44, 255)
        if y in (24, 25) and any(a <= x <= b for a, b in ((6, 11), (14, 20), (23, 26))):
            return (116, 73, 38, 255)
        if y == 28 and 8 <= x <= 24:
            return (179, 133, 72, 255)
        shade = (x//9+y//6)%3*3
        return (231-shade, 211-shade, 164-shade, 255)
    background = (28+(x*3+y)%3, 38+(x+y)%2, 33+(x+y)%3, 255)
    if name == "menu":
        if bread := loaf(x, y, 16, 5, 12, 5):
            return CHALK if bread == (247, 219, 161, 255) else GOLD
        if y == 10 and 3 <= x <= 28:
            return GOLD
        for row, cy in enumerate((14, 20, 26)):
            if (row == 0 and loaf(x, y, 6, cy, 5, 3)) or (row == 1 and crescent(x, y, 6, cy, 2.5)):
                return CHALK
            if row == 2 and (x-6)**2+(y-cy)**2 <= 5:
                return CHALK
            if y == cy and 11 <= x <= (20, 22, 18)[row]:
                return CHALK
            if y == cy+2 and 11 <= x <= (16, 18, 15)[row]:
                return (135, 150, 128, 255)
            if y == cy and 25 <= x <= 28:
                return GOLD
    elif name == "special":
        if y in (3, 4) and 8 <= x <= 23:
            return GOLD
        if y == 7 and 12 <= x <= 19:
            return CHALK
        if crescent(x, y, 16, 17, 8):
            return GOLD if (x+y)%7 else CHALK
        if y in (27, 28) and any(a <= x <= b for a, b in ((6, 14), (17, 24))):
            return CHALK
    elif name == "sidewalk":
        if bread := loaf(x, y, 16, 7, 16, 7):
            return CHALK if bread == (247, 219, 161, 255) else GOLD
        if y == 14 and 5 <= x <= 26:
            return GOLD
        for cy, length in ((18, 24), (23, 21), (28, 25)):
            if y == cy and 7 <= x <= length:
                return CHALK
            if y == cy+1 and 7 <= x <= length-4:
                return (135, 150, 128, 255)
    return background


def textures():
    for name in TILES:
        png(name, lambda x, y, name=name: pixel(name, x, y))


def box(name, a, b, material, only=FACES):
    x0, y0, z0 = a
    x1, y1, z1 = b
    uv = {"north": [16-x1, 16-y1, 16-x0, 16-y0],
          "south": [x0, 16-y1, x1, 16-y0],
          "west": [z0, 16-y1, z1, 16-y0],
          "east": [16-z1, 16-y1, 16-z0, 16-y0],
          "up": [x0, z0, x1, z1], "down": [x0, 16-z1, x1, 16-z0]}
    # A-frame local leg coordinates extend just below zero before rotation.
    # Rebase their undirected wood UVs into the tile rather than exporting negatives.
    return {"name": name, "from": list(a), "to": list(b), "faces": {
        f: {"texture": "#"+material, "uv": [max(0, min(16, v)) for v in uv[f]]} for f in only}}


def artwork(e, face, material, uv=(0, 0, 16, 16)):
    e["faces"][face] = {"texture": "#"+material, "uv": list(uv)}
    return e


def framed_board(name, a, b, material, face="north"):
    x0, y0, z0 = a
    x1, y1, z1 = b
    e = [box(name+"_left_frame", [x0, y0, z0], [x0+1, y1, z1], "trim"),
         box(name+"_right_frame", [x1-1, y0, z0], [x1, y1, z1], "trim"),
         box(name+"_bottom_frame", [x0+1, y0, z0], [x1-1, y0+1, z1], "trim"),
         box(name+"_top_frame", [x0+1, y1-1, z0], [x1-1, y1, z1], "trim"),
         box(name+"_backing", [x0+1, y0+1, z0+.125], [x1-1, y1-1, z1-.125], "wood")]
    z = z0 if face == "north" else z1
    skin = box(name+"_artwork", [x0+1, y0+1, z-.0625], [x1-1, y1-1, z+.0625], "wood", (face,))
    e.append(artwork(skin, face, material))
    return e


def tilt(e, angle):
    e = copy.deepcopy(e)
    e["rotation"] = {"origin": [8, 13, 8], "axis": "x", "angle": angle, "rescale": False}
    return e


def hanging():
    e = [box("wall_mount", [7, 10.5, 15.25], [9, 16, 16], "iron"),
         box("projecting_bracket_arm", [7.625, 14.5, .75], [8.375, 15.25, 16], "iron"),
         box("bracket_brace", [7.625, 11.5, 13.5], [8.375, 15, 14.25], "iron")]
    e[-1]["rotation"] = {"origin": [8, 13.25, 13.875], "axis": "x", "angle": -45, "rescale": False}
    for index, z in enumerate((3, 10.5)):
        e.append(box(f"suspension_link_{index}", [7.75, 11.5, z], [8.25, 14.5, z+.5], "iron"))
    e.extend([
        box("plaque_top_frame", [7.25, 10.75, 2], [8.75, 11.75, 12], "trim"),
        box("plaque_bottom_frame", [7.25, 4.25, 2], [8.75, 5.25, 12], "trim"),
        box("plaque_front_frame", [7.25, 5.25, 2], [8.75, 10.75, 3], "trim"),
        box("plaque_rear_frame", [7.25, 5.25, 11], [8.75, 10.75, 12], "trim"),
        box("plaque_wood_core", [7.375, 5.25, 3], [8.625, 10.75, 11], "wood"),
        artwork(box("plaque_left_artwork", [7.3125, 5.25, 3], [7.375, 10.75, 11], "wood", ("west",)), "west", "hanging"),
        artwork(box("plaque_right_artwork", [8.625, 5.25, 3], [8.6875, 10.75, 11], "wood", ("east",)), "east", "hanging"),
    ])
    return e


def sidewalk():
    e = []
    collision = []
    for front, angle in ((True, 22.5), (False, -22.5)):
        name = "front" if front else "rear"
        z0, z1 = (7, 8) if front else (8, 9)
        face = "north" if front else "south"
        e.extend(tilt(p, angle) for p in framed_board(name, [2, 1.5, z0], [14, 13, z1], "sidewalk", face))
        # Three collision bands approximate each incline instead of filling the
        # whole triangular space with one large AABB. Tiny artwork needs no box.
        for y0, y1 in ((1.5, 5), (5, 9), (9, 13)):
            collision.append(bounds(tilt(box(name+"_collision", [2, y0, z0-.0625], [14, y1, z1+.0625], "wood"), angle)))
        for side, x in (("left", 2), ("right", 13)):
            # Frame rails are the upper legs; extend only below them so their
            # outer faces do not overlap and flicker with identical long legs.
            leg = tilt(box(f"{name}_{side}_leg", [x, -.9, z0], [x+1, 1.5, z1], "trim"), angle)
            e.append(leg)
            collision.append(bounds(leg))
            foot_z = 2 if front else 12
            foot = box(f"{name}_{side}_foot", [x-.25, 0, foot_z], [x+1.25, .625, foot_z+2], "trim")
            e.append(foot)
            collision.append(bounds(foot))
    e.extend([box("steel_top_hinge", [2, 13, 7.5], [14, 13.625, 8.5], "iron"),
              box("left_spreader", [2.25, 4, 4.75], [2.75, 4.5, 11.25], "wood"),
              box("right_spreader", [13.25, 4, 4.75], [13.75, 4.5, 11.25], "wood")])
    collision.extend(bounds(p) for p in e[-3:])
    return e, collision


def arrangement(model_id):
    if model_id == IDS[0]:
        e = hanging()
    elif model_id == IDS[1]:
        e = framed_board("shop", [1, 4, 14.25], [15, 12, 16], "shop")
        e.extend([box("raised_header", [5, 12, 14.625], [11, 13, 15.75], "wood"),
                  box("lower_wood_bead", [2, 3.5, 14.375], [14, 4, 15.875], "wood")])
    elif model_id == IDS[2]:
        e = framed_board("wall_menu", [1, 1, 14.5], [15, 15, 16], "menu")
    elif model_id == IDS[3]:
        e = framed_board("daily_special", [3, 4, 14.5], [13, 13, 16], "special")
        e.append(box("chalk_ledge", [3, 3.75, 13.625], [13, 4.375, 16], "wood"))
    elif model_id == IDS[4]:
        e, collision = sidewalk()
    else:
        raise ValueError(model_id)
    if model_id != IDS[4]:
        collision = [bounds(p) for p in e if not p["name"].endswith("artwork")]
    omit_hidden_faces(e)
    validate_model_elements(e)
    for piece in e:
        lo, hi = bounds(piece)
        assert all(-1e-6 <= lo[i] <= hi[i] <= 16+1e-6 for i in range(3)), (piece["name"], lo, hi)
        if "rotation" in piece:
            assert piece["rotation"]["angle"] in (-45, -22.5, 0, 22.5, 45)
        for face in piece["faces"].values():
            assert face["texture"][1:] in MATERIALS
            assert all(0 <= value <= 16 for value in face["uv"])
    used = {face["texture"][1:] for piece in e for face in piece["faces"].values()} | {"particle"}
    return {"parent": "minecraft:block/block", "textures": {k: v for k, v in MATERIALS.items() if k in used}, "elements": e}, collision


def write_shapes(shapes):
    rows = []
    for model_id, boxes in shapes.items():
        rows.extend([f"            // {model_id}", "            rotations(Shapes.or("])
        for i, (lo, hi) in enumerate(boxes):
            values = ", ".join(f"{v:.6f}".rstrip("0").rstrip(".") for v in lo+hi)
            rows.append("                    Block.box("+values+")" + ("," if i < len(boxes)-1 else ").optimize()),"))
    source = '''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_bakery_signage.py; edit layouts there. */
final class BakerySignageShapes {
    static final VoxelShape[][] SHAPES = {
''' + "\n".join(rows) + '''
    };

    private BakerySignageShapes() {
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
    (ROOT / "src/main/java/com/craftdistrict/block/BakerySignageShapes.java").write_text(source, encoding="utf-8")


def main():
    textures()
    shapes = {}
    for model_id in IDS:
        model, collision = arrangement(model_id)
        write_json(MODELS / f"{model_id}.json", model)
        write_json(ASSETS / f"blockstates/{model_id}.json", {"variants": {
            f"facing={facing}": {"model": f"craftdistrict:block/bakery/{model_id}", "y": angle}
            for facing, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        item = copy.deepcopy(model)
        # Center wall models for inventory display without changing their block
        # geometry or texture direction. The hanging sign uses its broad side.
        if model_id not in (IDS[0], IDS[4]):
            for p in item["elements"]:
                for key in ("from", "to"):
                    p[key][2] -= 7
        item["display"] = {
            "gui": {"rotation": [20, 225 if model_id == IDS[0] else 135, 0], "scale": [.75, .75, .75]},
            "ground": {"translation": [0, 3, 0], "scale": [.4, .4, .4]},
            "fixed": {"rotation": [0, 270 if model_id == IDS[0] else 180, 0], "scale": [.75, .75, .75]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.4, .4, .4]},
            "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.4, .4, .4]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [.6, .6, .6]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [.6, .6, .6]}}
        write_json(ASSETS / f"models/item/{model_id}.json", item)
        write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{model_id}.json", {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"craftdistrict:{model_id}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        shapes[model_id] = collision
        print(f"{model_id}: {len(model['elements'])} cuboids, {sum(len(e['faces']) for e in model['elements'])} faces, {len(collision)} shape envelopes")
    write_shapes(shapes)
    print("Generated five signage variants, 20 facing states and six new opaque 32x32 textures.")


if __name__ == "__main__":
    main()
