"""Stage 2: one 32x32x16 sealed case, two independent dessert compositions.

North (-Z) is front. Standard-library generation; existing assets are read-only.
Vanilla multipart models share the external case across both four-part blocks.
Glass is binary-alpha cutout, with one double-sided plane per physical panel.
"""

import copy
import struct
import sys
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, write_json
from generate_flour_sacks import bounds, cloth_uvs as product_uvs, place, rounded_prism, omit_hidden_faces, validate_model_elements

IDS = ("refrigerated_cake_display", "refrigerated_pastry_display")
SHELVES = (8, 15.5, 23.5)
SHELF_THICKNESS = .375
TILES = ("glass", "shelf", "vent", "light", "chocolate_layers", "vanilla_layers", "pink_layers",
         "cheese", "frosting", "chocolate", "pastry", "custard", "strawberry", "berry")
MATERIALS = {name: f"craftdistrict:block/bakery/refrigerated_display_{name}" for name in TILES}
MATERIALS.update({name: f"craftdistrict:block/bakery/industrial_bakery_oven_{tile}"
                  for name, tile in (("metal", "metal"), ("steel", "steel"), ("black", "interior"), ("service", "service"))})
MATERIALS["particle"] = MATERIALS["metal"]
FACES = ("north", "south", "west", "east", "up", "down")


def pixel(name, x, y):
    if name == "glass":
        # More than 96% clear: short highlights, no opaque grid or tint field.
        if (x-y == -3 and 6 <= y <= 9) or (x-y == 7 and 15 <= y <= 17):
            return (202, 221, 222, 255)
        return (202, 221, 222, 0)
    if name == "shelf":
        shade = 190+(x//12+y//8)%3*4
        return (shade, shade+6, shade+7, 255)
    if name == "vent":
        return (25, 31, 35, 255) if 2 <= x <= 29 and y%6 in (2, 3) else (66, 73, 78, 255)
    if name == "light":
        return (240, 233, 202, 255) if y not in (0, 31) else (157, 163, 154, 255)
    if name.endswith("_layers"):
        cream = (239, 222, 184, 255)
        base = {"chocolate_layers": (89, 49, 35), "vanilla_layers": (224, 202, 153),
                "pink_layers": (207, 141, 154)}[name]
        if y in (9, 10, 21, 22):
            return cream
        shade = (x//6+y//4)%3*3
        return (*(c-shade for c in base), 255)
    base = {"cheese": (235, 215, 153), "frosting": (242, 230, 207), "chocolate": (72, 40, 29),
            "pastry": (207, 149, 71), "custard": (244, 222, 160),
            "strawberry": (182, 49, 57), "berry": (91, 60, 101)}[name]
    shade = (x//5+y//6)%3*4
    if name == "strawberry" and (x*3+y*5)%37 == 0:
        return (220, 148, 108, 255)
    return (*(c-shade for c in base), 255)


def textures():
    def chunk(kind, data):
        return struct.pack(">I", len(data))+kind+data+struct.pack(">I", zlib.crc32(kind+data))
    for name in TILES:
        rows = b"".join(b"\0"+bytes(c for x in range(32) for c in pixel(name, x, y)) for y in range(32))
        data = b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR", struct.pack(">IIBBBBB", 32, 32, 8, 6, 0, 0, 0))
        data += chunk(b"IDAT", zlib.compress(rows, 9))+chunk(b"IEND", b"")
        path = ASSETS / f"textures/block/bakery/refrigerated_display_{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


def box(name, a, b, material, only=FACES, full=False):
    x0, y0, z0 = a
    x1, y1, z1 = b
    # Correct outward face conventions, with 32-pixel material sheets spanning
    # the whole 32-unit case. Product UVs are projected independently below.
    uv = {"north": [(32-x1)/2, (32-y1)/2, (32-x0)/2, (32-y0)/2],
          "south": [x0/2, (32-y1)/2, x1/2, (32-y0)/2],
          "west": [z0/2, (32-y1)/2, z1/2, (32-y0)/2],
          "east": [(16-z1)/2, (32-y1)/2, (16-z0)/2, (32-y0)/2],
          "up": [x0/2, z0/2, x1/2, z1/2],
          "down": [x0/2, (16-z1)/2, x1/2, (16-z0)/2]}
    return {"name": name, "from": list(a), "to": list(b), "faces": {
        f: {"texture": "#"+material, "uv": [0, 0, 16, 16] if full else uv[f]} for f in only}}


def shared_case():
    e = [box("refrigeration_plinth", [.75, .5, 1], [31.25, 7.25, 15.25], "metal"),
         box("lower_toe_strip", [1, 1, .875], [31, 1.625, 15.375], "steel"),
         box("front_vent_grille", [3, 2.75, .875], [29, 5.75, 1], "vent", ("north",), True),
         box("plinth_upper_lip", [.75, 7.25, .75], [31.25, 7.75, 15.5], "steel"),
         box("top_cap", [.75, 30.75, .75], [31.25, 31.5, 15.5], "metal"),
         box("top_front_steel_edge", [.875, 30.5, .75], [31.125, 30.75, 1.75], "steel"),
         box("rear_center_door_seam", [15.875, 7.75, 14.5], [16.125, 30.75, 15.5], "metal"),
         box("front_glass", [1.5, 7.75, 1.375], [30.5, 30.5, 1.375], "glass", ("north", "south")),
         box("left_glass", [1.125, 7.75, 1.75], [1.125, 30.5, 14.5], "glass", ("west", "east")),
         box("right_glass", [30.875, 7.75, 1.75], [30.875, 30.5, 14.5], "glass", ("west", "east"))]
    for side, x0 in (("left", .75), ("right", 30.5)):
        for end, z0 in (("front", .75), ("rear", 14.5)):
            e.append(box(f"{side}_{end}_post", [x0, 7.75, z0], [x0+.75, 30.75, z0+1], "steel"))
        e.append(box(f"{side}_plinth_vent", [x0-.125 if side == "left" else x0+.75, 3, 4],
                     [x0 if side == "left" else x0+.875, 5.5, 12], "vent", ("west" if side == "left" else "east",), True))
    for name, x0, x1 in (("left", 1.5, 15.875), ("right", 16.125, 30.5)):
        e.append(box(name+"_rear_service_door", [x0, 7.75, 14.875], [x1, 30.5, 15.125], "service", ("south", "north", "west", "east", "up", "down"), True))
        x = 12.5 if name == "left" else 18.75
        e.extend([box(name+"_handle_lower_mount", [x, 16.75, 15.125], [x+.625, 17.375, 15.875], "metal"),
                  box(name+"_handle_upper_mount", [x, 21.25, 15.125], [x+.625, 21.875, 15.875], "metal"),
                  box(name+"_rear_handle", [x, 17, 15.625], [x+.625, 21.625, 16], "steel")])
    for side, x in (("left", 1.5), ("right", 28)):
        for end, z in (("front", 2), ("rear", 12)):
            e.append(box(f"{side}_{end}_foot", [x, 0, z], [x+2.5, .5, z+2.5], "black"))
    for index, y in enumerate(SHELVES):
        e.append(box(f"shelf_{index}", [1.75, y, 2], [30.25, y+SHELF_THICKNESS, 14.25], "shelf"))
        e.append(box(f"shelf_{index}_front_bead", [1.75, y, 1.875], [30.25, y+.375, 2], "steel"))
    for end, z in (("front", 2.25), ("rear", 13.5)):
        p = box(end+"_static_light_strip", [2, 30.375, z], [30, 30.5, z+.375], "light")
        p["shade"] = False  # Texture detail only: no emitted/dynamic light.
        e.append(p)
    return e


def berry(name, x, y, z, material="strawberry"):
    return [box(name+"_base", [x, y, z], [x+.75, y+.625, z+.75], material),
            box(name+"_tip", [x+.125, y+.625, z+.125], [x+.625, y+.875, z+.625], material)]


def round_cake(name, diameter, height, layer, glaze="frosting", fruit=2):
    e = rounded_prism(name+"_layered_body", [0, 0, 0], [diameter, height, diameter], "y", layer)
    e += rounded_prism(name+"_icing", [0, height, 0], [diameter, height+.375, diameter], "y", glaze)
    for i in range(fruit):
        x, z = diameter*.3+i*.8, diameter*.35+(i%2)*.65
        e.extend(berry(f"{name}_fruit_{i}", x, height+.375, z))
    return e, (diameter, height+.375+(.875 if fruit else 0), diameter)


def wedge(name, width, height, depth, material, glaze):
    e = []
    for i, fraction in enumerate((.35, .65, 1)):
        x0 = width*(1-fraction)/2
        z0, z1 = i*depth/3, (i+1)*depth/3
        e.append(box(f"{name}_slice_{i}", [x0, 0, z0], [width-x0, height, z1], material))
        e.append(box(f"{name}_icing_{i}", [x0, height, z0], [width-x0, height+.375, z1], glaze))
    return e, (width, height+.375, depth)


def eclair(name, length, depth=2.5, cream=False):
    e = rounded_prism(name+"_choux", [0, 0, 0], [length, 1.25, depth], "x", "pastry")
    e.append(box(name+"_cream_seam", [.25, .625, -.0625], [length-.25, .9375, depth+.0625], "frosting"))
    e += rounded_prism(name+"_topping", [.125, 1.25, .25], [length-.125, 1.625, depth-.25], "y", "frosting" if cream else "chocolate")
    if cream:
        e.extend(berry(name+"_fruit", length*.5, 1.625, depth*.4))
    return e, (length, 2.5 if cream else 1.625, depth+.125)


def fruit_tart(name, diameter, fruit=2):
    e = rounded_prism(name+"_crust", [0, 0, 0], [diameter, .875, diameter], "y", "pastry")
    e += rounded_prism(name+"_custard", [.375, .875, .375], [diameter-.375, 1.125, diameter-.375], "y", "custard")
    for i in range(fruit):
        e.extend(berry(f"{name}_fruit_{i}", diameter*.27+i*.8, 1.125, diameter*.35+(i%2)*.75,
                      "strawberry" if i%2 == 0 else "berry"))
    return e, (diameter, 2, diameter)


def danish(name):
    p = box(name+"_folded_diamond", [.6, 0, .6], [3.4, 1.125, 3.4], "pastry")
    p["rotation"] = {"axis": "y", "angle": 45, "origin": [2, 0, 2], "rescale": False}
    e = [p, box(name+"_custard_center", [1, 1.125, 1], [3, 1.375, 3], "custard")]
    e.extend(berry(name+"_fruit", 1.5, 1.375, 1.5, "berry"))
    return e, (4, 2.25, 4)


def cream_puff(name):
    e = rounded_prism(name+"_bottom", [0, 0, 0], [3.5, .75, 3.5], "y", "pastry")
    e += rounded_prism(name+"_cream", [.125, .75, .125], [3.375, 1.25, 3.375], "y", "frosting")
    e += rounded_prism(name+"_crown", [.25, 1.25, .25], [3.25, 1.875, 3.25], "y", "pastry")
    return e, (3.5, 1.875, 3.5)


def add_product(e, groups, source, x, shelf, z):
    pieces, size = source
    product_uvs(pieces, size)
    position = (x, SHELVES[shelf]+SHELF_THICKNESS, z)
    placed = [place(p, position, size, 0) for p in pieces]
    e.extend(placed)
    limits = [bounds(p) for p in placed]
    groups.append({"shelf": shelf, "name": pieces[0]["name"],
                   "lo": [min(a[i] for a, b in limits) for i in range(3)],
                   "hi": [max(b[i] for a, b in limits) for i in range(3)]})


def composition(variant):
    e, groups = [], []
    if variant == "cake":
        layout = (
            (wedge("chocolate_slice", 4, 2.5, 5, "chocolate_layers", "chocolate"), 3, 0, 4),
            (wedge("cheesecake_slice", 3.5, 2.25, 4.5, "cheese", "frosting"), 10, 0, 8),
            (round_cake("strawberry_cake", 4, 2.25, "vanilla_layers"), 18, 0, 4),
            (round_cake("fruit_mini_cake", 3.5, 2, "pink_layers", fruit=1), 25, 0, 8),
            (round_cake("chocolate_layer_cake", 4.5, 2.75, "chocolate_layers", "chocolate", 0), 3.5, 1, 5),
            (round_cake("pink_strawberry_cake", 4, 2.5, "pink_layers"), 10, 1, 4),
            (round_cake("cream_centerpiece", 5.5, 3.25, "vanilla_layers", fruit=3), 22, 1, 5),
            (round_cake("top_fruit_cake", 4, 2, "vanilla_layers", fruit=1), 5, 2, 5),
            (wedge("top_cheesecake", 4.5, 2.5, 5, "cheese", "frosting"), 23, 2, 6),
        )
    elif variant == "pastry":
        layout = (
            (eclair("chocolate_eclair", 5.5), 3, 0, 4),
            (eclair("short_eclair", 4.5), 9.5, 0, 9),
            (fruit_tart("bottom_fruit_tart", 4.5), 18, 0, 4),
            (danish("bottom_custard_danish"), 25, 0, 8),
            (fruit_tart("middle_fruit_tart", 5, 3), 3.5, 1, 5),
            (cream_puff("cream_profiterole"), 10, 1, 4.5),
            (fruit_tart("focal_fruit_tart", 5.5, 3), 22, 1, 5),
            (danish("top_custard_danish"), 5, 2, 5),
            (eclair("cream_filled_pastry", 6.5, 3, True), 22, 2, 6),
        )
    else:
        raise ValueError(variant)
    for source, x, shelf, z in layout:
        add_product(e, groups, source, x, shelf, z)
    return e, groups


def clip(element, column, row):
    """Correct outward UV clipping, including zero-thickness glass panels.

    Rotated products are kept inside their own column; Y rotations may cross
    the horizontal seam. No X/Z rotated geometry is clipped across a boundary.
    """
    origin, maximum = [column*16, row*16, 0], [(column+1)*16, (row+1)*16, 16]
    a, b = element["from"], element["to"]
    c, d = [max(a[i], origin[i]) for i in range(3)], [min(b[i], maximum[i]) for i in range(3)]
    if any(c[i] > d[i] or (c[i] == d[i] and a[i] != b[i]) for i in range(3)):
        return None
    r = copy.deepcopy(element)
    r["from"], r["to"] = [c[i]-origin[i] for i in range(3)], [d[i]-origin[i] for i in range(3)]
    if "rotation" in r:
        r["rotation"]["origin"] = [r["rotation"]["origin"][i]-origin[i] for i in range(3)]
        if r["rotation"]["axis"] == "y":
            assert c[0] == a[0] and d[0] == b[0], "Keep rotated products inside one column"
        else:
            assert c == a and d == b, "Keep X/Z rotated products inside one complete cell"
    axes = {"north": (0,-1,1,-1,2,-1), "south": (0,1,1,-1,2,1),
            "west": (2,1,1,-1,0,-1), "east": (2,-1,1,-1,0,1),
            "up": (0,1,2,1,1,1), "down": (0,1,2,-1,1,-1)}
    for face in list(r["faces"]):
        ua, us, va, vs, normal, ns = axes[face]
        if (ns < 0 and c[normal] != a[normal]) or (ns > 0 and d[normal] != b[normal]):
            del r["faces"][face]
            continue
        old = element["faces"][face]["uv"]
        values = []
        for axis, sign, uv0, uv1 in ((ua,us,old[0],old[2]), (va,vs,old[1],old[3])):
            length = b[axis]-a[axis]
            low = (c[axis]-a[axis])/length if sign > 0 else (b[axis]-d[axis])/length
            high = (d[axis]-a[axis])/length if sign > 0 else (b[axis]-c[axis])/length
            values.append((uv0+(uv1-uv0)*low, uv0+(uv1-uv0)*high))
        r["faces"][face]["uv"] = [round(v, 6) for v in (values[0][0], values[1][0], values[0][1], values[1][1])]
    return r if r["faces"] else None


def model(elements):
    used = {f["texture"][1:] for e in elements for f in e["faces"].values()} | {"particle"}
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {k: v for k, v in MATERIALS.items() if k in used}, "elements": elements}


def validate(elements):
    validate_model_elements(elements)
    for e in elements:
        assert all(-16 <= v <= 32 for key in ("from", "to") for v in e[key]), e["name"]
        lo, hi = bounds(e)
        assert all(-1e-6 <= lo[i] <= hi[i] <= (32 if i < 2 else 16)+1e-6 for i in range(3)), (e["name"], lo, hi)
        for face in e["faces"].values():
            assert face["texture"][1:] in MATERIALS
            assert all(0 <= v <= 16 for v in face["uv"])


COLLISION = ((.75, 0, .875, 31.25, 7.75, 15.5),
             (.75, 7.75, 1.125, 31.25, 31.5, 15.5),
             (12.5, 16.75, 15.5, 13.125, 21.875, 16),
             (18.75, 16.75, 15.5, 19.375, 21.875, 16))


def shape_source():
    rows = []
    for part in range(4):
        column, row = part%2, part//2
        cells = []
        for x0,y0,z0,x1,y1,z1 in COLLISION:
            a, b = max(x0,column*16), min(x1,(column+1)*16)
            c, d = max(y0,row*16), min(y1,(row+1)*16)
            if a < b and c < d:
                values = (a-column*16,c-row*16,z0,b-column*16,d-row*16,z1)
                cells.append("                    Block.box("+", ".join(f"{v:g}" for v in values)+")")
        rows.append("            rotations(Shapes.or(\n"+",\n".join(cells)+").optimize())")
    source = '''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_refrigerated_displays.py. Sealed cases share shapes. */
final class RefrigeratedDisplayShapes {
    static final VoxelShape[][] SHAPES = {
''' + ",\n".join(rows) + '''
    };

    private RefrigeratedDisplayShapes() {
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
    (ROOT / "src/main/java/com/craftdistrict/block/RefrigeratedDisplayShapes.java").write_text(source, encoding="utf-8")


def main():
    textures()
    case = shared_case()
    omit_hidden_faces(case)
    validate(case)
    for index, model_id in enumerate(IDS):
        variant = "cake" if index == 0 else "pastry"
        contents, groups = composition(variant)
        omit_hidden_faces(contents)
        validate(contents)
        entries = []
        for part in range(4):
            column, row = part%2, part//2
            for prefix, source in (("refrigerated_display_case", case), (f"refrigerated_display_{variant}_contents", contents)):
                pieces = [p for e in source if (p := clip(e, column, row)) is not None]
                validate_model_elements(pieces)
                if index == 0 or prefix != "refrigerated_display_case":
                    write_json(MODELS / f"{prefix}_{part}.json", model(pieces))
                for facing, angle in (("north",0),("east",90),("south",180),("west",270)):
                    entries.append({"when": {"facing": facing, "part": str(part)},
                                    "apply": {"model": f"craftdistrict:block/bakery/{prefix}_{part}", "y": angle}})
        write_json(ASSETS / f"blockstates/{model_id}.json", {"multipart": entries})
        elements = copy.deepcopy(case)+copy.deepcopy(contents)
        for e in elements:
            for key in ("from", "to"):
                e[key][0] -= 8
                e[key][1] -= 8
            if "rotation" in e:
                e["rotation"]["origin"][0] -= 8
                e["rotation"]["origin"][1] -= 8
        item = model(elements)
        item["display"] = {
            "gui": {"rotation": [30,135,0], "scale": [.4,.4,.4]},
            "ground": {"translation": [0,3,0], "scale": [.25,.25,.25]},
            "fixed": {"rotation": [0,180,0], "scale": [.4,.4,.4]},
            "thirdperson_righthand": {"rotation": [75,45,0], "translation": [0,2.5,0], "scale": [.3,.3,.3]},
            "thirdperson_lefthand": {"rotation": [75,45,0], "translation": [0,2.5,0], "scale": [.3,.3,.3]},
            "firstperson_righthand": {"rotation": [0,45,0], "scale": [.3,.3,.3]},
            "firstperson_lefthand": {"rotation": [0,225,0], "scale": [.3,.3,.3]}}
        write_json(ASSETS / f"models/item/{model_id}.json", item)
        write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{model_id}.json", {
            "type": "minecraft:block", "pools": [{"rolls":1, "entries":[{"type":"minecraft:item", "name":f"craftdistrict:{model_id}"}],
            "conditions":[{"condition":"minecraft:survives_explosion"}]}]})
        print(f"{model_id}: {len(case)} shared case cuboids/planes + {len(contents)} dessert cuboids; 9 desserts (4/3/2 per shelf).")
    shape_source()
    print("Shared four-section case, 32 multipart cases per variant, 14 new textures, binary-alpha glass.")


if __name__ == "__main__":
    main()
