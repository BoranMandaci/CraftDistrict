"""Stage 3: three compact single-cell wall racks; north (-Z) is outward.

Named model parts and conservative collision groups are edited here. Only this
family's resources and BakeryWallToolShapes.java are generated. Existing wood
and metal textures are referenced read-only; no signage models are regenerated.
"""

import copy
import struct
import sys
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, TEXTURE_MAP, write_json
from generate_flour_sacks import bounds, cloth_uvs as product_uvs, omit_hidden_faces, place, rounded_prism, validate_model_elements

IDS = ("bakery_bread_peel_rack", "bakery_utensil_wall_rack", "bakery_rolling_pin_rack")
TILES = ("wood_light", "wood_mid", "wood_dark", "working_steel")
MATERIALS = {name: f"craftdistrict:block/bakery/bakery_wall_tools_{name}" for name in TILES}
MATERIALS.update({"rail": TEXTURE_MAP["trim"], "metal": "craftdistrict:block/bakery/industrial_bakery_oven_metal",
                  "steel": "craftdistrict:block/bakery/industrial_bakery_oven_steel"})
MATERIALS["particle"] = MATERIALS["rail"]
FACES = ("north", "south", "west", "east", "up", "down")


def textures():
    def pixel(name, x, y):
        if name == "working_steel":
            shade = (176, 184, 191)[(y//3+x//9)%3]
            return (shade, shade+4, shade+6, 255)
        base = {"wood_light": (207, 159, 100), "wood_mid": (180, 125, 68), "wood_dark": (149, 96, 53)}[name]
        shade = -12 if x in (3, 11) and 2 <= y <= 13 else -5 if (x*5+y*2)%17 == 0 else (x//5+y//7)%2*3
        return (*(c+shade for c in base), 255)
    def chunk(kind, data):
        return struct.pack(">I", len(data))+kind+data+struct.pack(">I", zlib.crc32(kind+data))
    for name in TILES:
        rows = b"".join(b"\0"+bytes(c for x in range(16) for c in pixel(name, x, y)) for y in range(16))
        data = b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
        data += chunk(b"IDAT", zlib.compress(rows, 9))+chunk(b"IEND", b"")
        path = ASSETS / f"textures/block/bakery/bakery_wall_tools_{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)


def box(name, a, b, material):
    x0,y0,z0 = a
    x1,y1,z1 = b
    uv = {"north": [16-x1,16-y1,16-x0,16-y0], "south": [x0,16-y1,x1,16-y0],
          "west": [z0,16-y1,z1,16-y0], "east": [16-z1,16-y1,16-z0,16-y0],
          "up": [x0,z0,x1,z1], "down": [x0,16-z1,x1,16-z0]}
    return {"name": name, "from": list(a), "to": list(b),
            "faces": {f: {"texture":"#"+material,"uv":uv[f]} for f in FACES}}


def add(elements, collision, pieces):
    """One coarse envelope per curved tool section, rather than six tiny boxes."""
    elements.extend(pieces)
    limits = [bounds(p) for p in pieces]
    collision.append(([min(lo[i] for lo,hi in limits) for i in range(3)],
                      [max(hi[i] for lo,hi in limits) for i in range(3)]))


def curved(name, position, size, axis, material):
    e = rounded_prism(name, [0,0,0], list(size), axis, material)
    product_uvs(e, size)
    return [place(p, position, size, 0) for p in e]


def bread_peels():
    e, collision = [], []
    mounts = [box("upper_mounting_rail", [1.5,10.5,14.5], [14.5,11.5,16], "rail"),
              box("lower_paddle_rest_rail", [1.5,4,14.5], [14.5,4.75,16], "rail")]
    for name, x, y, width, height, handle_top, material in (
        ("long_peel",2,1.25,5,5,15.5,"wood_light"),
        ("short_peel",9,2,4.5,4.5,14.25,"wood_mid"),
    ):
        cx = x+width/2
        add(e, collision, curved(name+"_paddle", (x,y,12.875), (width,height,.75), "z", material))
        neck = box(name+"_neck", [cx-.625,y+height-.5,13.125], [cx+.625,y+height+1,13.625], material)
        handle = box(name+"_long_handle", [cx-.375,y+height+.5,13.125], [cx+.375,handle_top,13.625], material)
        add(e, collision, [neck])
        add(e, collision, [handle])
        mounts.extend([
            box(name+"_handle_clamp", [cx-.5,10,13.5], [cx+.5,11.5,14.75], "metal"),
            box(name+"_lower_rest_pad", [cx-.75,3.75,13.5], [cx+.75,4.25,14.75], "rail"),
        ])
    for p in mounts:
        add(e, collision, [p])
    return e, collision


def utensils():
    e, collision = [], []
    mounts = [box("metal_hanging_bar", [1.5,13.75,14.5], [14.5,14.75,15.5], "steel"),
              box("left_wall_bracket", [1.5,12.75,15.25], [2.25,15.5,16], "metal"),
              box("right_wall_bracket", [13.75,12.75,15.25], [14.5,15.5,16], "metal")]
    for index, cx in enumerate((3.5,6.75,10,13)):
        mounts.extend([
            box(f"hook_{index}_arm", [cx-.1875,13.875,13.25], [cx+.1875,14.125,15], "metal"),
            box(f"hook_{index}_tip", [cx-.1875,12.75,13.25], [cx+.1875,14.125,13.625], "metal"),
        ])
    for p in mounts:
        add(e, collision, [p])
    # Broad bench scraper: clipped lower corners, a rounded wooden cross-grip
    # and a narrow hanging tab. It is visually distinct from the long spatula.
    add(e, collision, [box("scraper_blade_upper", [2,6.5,12.75], [5,10,13.125], "working_steel"),
                       box("scraper_blade_lower", [2.25,6,12.75], [4.75,6.5,13.125], "working_steel")])
    add(e, collision, curved("scraper_wood_grip", (1.75,10,12.375), (3.5,.875,.875), "x", "wood_mid"))
    add(e, collision, [box("scraper_hanging_tab", [3.25,10.75,12.75], [3.75,13.125,13.25], "metal")])
    # Long, slightly flared pastry spatula, with a steel neck and wood handle.
    add(e, collision, [box("spatula_blade", [6,4,12.5], [7.5,8.25,13], "working_steel"),
                       box("spatula_rounded_tip", [6.25,3.5,12.5], [7.25,4,13], "working_steel")])
    add(e, collision, [box("spatula_neck", [6.5,8,12.5], [7,9.5,13.25], "steel")])
    add(e, collision, [box("spatula_handle", [6.375,9.25,12.75], [7.125,13.125,13.5], "wood_dark")])
    # Round dough/pastry cutter; the blade is a flat octagonal wheel, with a fork
    # connecting it to its handle and one useful central axle detail.
    add(e, collision, curved("dough_cutter_wheel", (8.75,4.25,12.375), (2.5,2.5,.625), "z", "working_steel"))
    for p in [box("cutter_left_fork", [9.375,5.5,12.75], [9.75,9.25,13.125], "steel"),
              box("cutter_right_fork", [10.25,5.5,12.75], [10.625,9.25,13.125], "steel"),
              box("cutter_handle", [9.625,9,12.75], [10.375,13.125,13.25], "wood_light"),
              box("cutter_axle", [9.75,5.25,12.25], [10.25,5.75,12.375], "metal")]:
        add(e, collision, [p])
    # A small palette knife finishes the set; its blade is much narrower than
    # the spatula and scraper, with a short wooden handle and tapered point.
    add(e, collision, [box("palette_knife_blade", [12.625,6.25,12.75], [13.375,10.5,13.125], "working_steel"),
                       box("palette_knife_tip", [12.8125,5.75,12.75], [13.1875,6.25,13.125], "working_steel")])
    add(e, collision, [box("palette_knife_handle", [12.625,10.25,12.75], [13.375,13.125,13.25], "wood_mid")])
    return e, collision


def rolling_pins():
    e, collision = [], []
    frame = [box("left_wooden_upright", [1.5,1.5,14.5], [2.25,14.75,16], "rail"),
             box("right_wooden_upright", [13.75,1.5,14.5], [14.5,14.75,16], "rail"),
             box("upper_mounting_crossbar", [2.25,14,14.5], [13.75,14.75,16], "rail"),
             box("lower_mounting_crossbar", [2.25,1.5,14.5], [13.75,2.25,16], "rail")]
    for p in frame:
        add(e, collision, [p])
    for index, (cx0,cx1,cy,diameter,material) in enumerate((
        (4,12,3.5,1.75,"wood_light"), (3.5,12.5,8,2,"wood_mid"), (3,13,12.5,2.125,"wood_dark"))):
        radius = diameter/2
        add(e, collision, curved(f"pin_{index}_barrel", (cx0,cy-radius,12.75-radius),
                                 (cx1-cx0,diameter,diameter), "x", material))
        for name,a,b in (("left_handle",[cx0-1.5,cy-.375,12.375],[cx0,cy+.375,13.125]),
                         ("right_handle",[cx1,cy-.375,12.375],[cx1+1.5,cy+.375,13.125])):
            add(e, collision, [box(f"pin_{index}_{name}",a,b,material)])
        for side,x0 in (("left",3.5),("right",11.5)):
            add(e, collision, [box(f"pin_{index}_{side}_cradle", [x0,cy-radius-.25,11.5],
                                   [x0+1,cy-radius+.125,14.75], "rail")])
            add(e, collision, [box(f"pin_{index}_{side}_cradle_lip", [x0,cy-radius-.25,11.375],
                                   [x0+1,cy-radius+.5,11.75], "rail")])
    return e, collision


def arrangement(model_id):
    e, collision = {IDS[0]:bread_peels, IDS[1]:utensils, IDS[2]:rolling_pins}[model_id]()
    omit_hidden_faces(e)
    validate_model_elements(e)
    for p in e:
        lo,hi = bounds(p)
        assert all(-1e-6 <= lo[i] <= hi[i] <= 16+1e-6 for i in range(3)), (p["name"],lo,hi)
        assert all(-16 <= v <= 32 for key in ("from","to") for v in p[key])
        for face in p["faces"].values():
            assert face["texture"][1:] in MATERIALS
            assert all(0 <= uv <= 16 for uv in face["uv"])
    used = {f["texture"][1:] for p in e for f in p["faces"].values()} | {"particle"}
    return {"parent":"minecraft:block/block", "textures":{k:v for k,v in MATERIALS.items() if k in used}, "elements":e}, collision


def write_shapes(shapes):
    rows = []
    for model_id, boxes in shapes.items():
        rows.extend([f"            // {model_id}", "            rotations(Shapes.or("])
        for i,(lo,hi) in enumerate(boxes):
            coordinates = ", ".join(f"{v:.6f}".rstrip("0").rstrip(".") for v in lo+hi)
            rows.append("                    Block.box("+coordinates+")"+( "," if i < len(boxes)-1 else ").optimize()),"))
    source = '''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_bakery_wall_tools.py; edit tool layouts there. */
final class BakeryWallToolShapes {
    static final VoxelShape[][] SHAPES = {
''' + "\n".join(rows) + '''
    };

    private BakeryWallToolShapes() {
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
    (ROOT / "src/main/java/com/craftdistrict/block/BakeryWallToolShapes.java").write_text(source,encoding="utf-8")


def main():
    textures()
    shapes = {}
    for model_id in IDS:
        model, collision = arrangement(model_id)
        write_json(MODELS / f"{model_id}.json",model)
        write_json(ASSETS / f"blockstates/{model_id}.json", {"variants":{
            f"facing={face}":{"model":f"craftdistrict:block/bakery/{model_id}","y":angle}
            for face,angle in (("north",0),("east",90),("south",180),("west",270))}})
        item = copy.deepcopy(model)
        for p in item["elements"]:
            for key in ("from","to"):
                p[key][2] -= 6
            if "rotation" in p:
                p["rotation"]["origin"][2] -= 6
        item["display"] = {
            "gui":{"rotation":[20,135,0],"scale":[.75,.75,.75]},
            "ground":{"translation":[0,3,0],"scale":[.4,.4,.4]},
            "fixed":{"rotation":[0,180,0],"scale":[.75,.75,.75]},
            "thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.4,.4,.4]},
            "thirdperson_lefthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.4,.4,.4]},
            "firstperson_righthand":{"rotation":[0,45,0],"scale":[.6,.6,.6]},
            "firstperson_lefthand":{"rotation":[0,225,0],"scale":[.6,.6,.6]}}
        write_json(ASSETS / f"models/item/{model_id}.json",item)
        write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{model_id}.json", {
            "type":"minecraft:block", "pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"craftdistrict:{model_id}"}],
            "conditions":[{"condition":"minecraft:survives_explosion"}]}]})
        shapes[model_id] = collision
        print(f"{model_id}: {len(model['elements'])} visible cuboids, {sum(len(p['faces']) for p in model['elements'])} faces, {len(collision)} shape groups.")
    write_shapes(shapes)
    print("Exactly three single-cell wall racks; 12 facing states; four new opaque 16x16 textures.")


if __name__ == "__main__":
    main()
