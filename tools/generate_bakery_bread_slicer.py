"""Stage 4: one static commercial slicer. NORTH (-Z) is the output/front.

Named structural parts, separate whole loaf and five slices, and coarse shape
envelopes are edited here. Only this decoration's assets/shapes are written.
Existing industrial textures are referenced read-only. Vanilla legal rotations
and nonempty face maps are validated before export.
"""

import copy
import struct
import sys
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, write_json
from generate_flour_sacks import bounds, cloth_uvs, omit_hidden_faces, place, rounded_prism, validate_model_elements

ID = "bakery_bread_slicer"
TILES = ("brushed_steel", "crust", "crumb", "controls")
MATERIALS = {name: f"craftdistrict:block/bakery/{ID}_{name}" for name in TILES}
MATERIALS.update({name: f"craftdistrict:block/bakery/industrial_bakery_oven_{name}"
                  for name in ("metal", "steel", "interior", "vent", "service")})
MATERIALS["particle"] = MATERIALS["metal"]
FACES = ("north", "south", "west", "east", "up", "down")


def textures():
    def pixel(name, x, y):
        if name == "brushed_steel":
            v = (169, 177, 185, 178)[y % 4] + (3 if x in (1, 14) else 0)
            return (v, v+4, v+7, 255)
        if name == "crust":
            v = -16 if (x*3+y*7)%19 == 0 else 9 if (x*7+y)%13 == 0 else 0
            if y in (3, 8, 13) and 5 <= x <= 10:
                return (238, 193, 109, 255)  # Broad baked scoring, no micro-cubes.
            return (194+v, 125+v, 56+v, 255)
        if name == "crumb":
            if x < 2 or x > 13 or y < 2 or y > 13:
                return (184, 111, 46, 255)
            v = -19 if (x*7+y*11)%23 == 0 else 4 if (x+y)%5 == 0 else 0
            return (242+v, 217+v, 165+v, 255)
        if 3 <= x <= 5 and 9 <= y <= 11:
            return (70, 160, 99, 255)
        if 9 <= x <= 11 and 9 <= y <= 11:
            return (190, 61, 51, 255)
        # Deliberately asymmetric indicator bars: broad left, short right.
        # Entire tile is mapped to the outward north face without UV reversal.
        if 2 <= x <= 13 and 2 <= y <= 6:
            if (3 <= x <= 5 and y <= 5) or (8 <= x <= 11 and y == 4):
                return (139, 222, 181, 255)
            return (21, 37, 36, 255)
        if x in (2, 13) or y in (1, 14):
            return (126, 140, 147, 255)
        return (54, 63, 69, 255)
    def chunk(kind, data):
        return struct.pack(">I", len(data))+kind+data+struct.pack(">I", zlib.crc32(kind+data))
    for name in TILES:
        rows = b"".join(b"\0"+bytes(c for x in range(16) for c in pixel(name,x,y)) for y in range(16))
        png = b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",16,16,8,6,0,0,0))
        png += chunk(b"IDAT",zlib.compress(rows,9))+chunk(b"IEND",b"")
        path = ASSETS / f"textures/block/bakery/{ID}_{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(png)


def box(name, a, b, material):
    x0,y0,z0 = a
    x1,y1,z1 = b
    uv = {"north":[16-x1,16-y1,16-x0,16-y0], "south":[x0,16-y1,x1,16-y0],
          "west":[z0,16-y1,z1,16-y0], "east":[16-z1,16-y1,16-z0,16-y0],
          "up":[x0,z0,x1,z1], "down":[x0,16-z1,x1,16-z0]}
    return {"name":name,"from":list(a),"to":list(b),
            "faces":{f:{"texture":"#"+material,"uv":uv[f]} for f in FACES}}


def curved(name, position, size, material, sliced=False):
    pieces = rounded_prism(name,[0,0,0],list(size),"z",material)
    cloth_uvs(pieces,size)
    if sliced:
        for p in pieces:
            for face in ("north", "south"):
                p["faces"][face]["texture"] = "#crumb"
    return [place(p,position,size,0) for p in pieces]


def geometry():
    e = []
    def add(name, a, b, material):
        p = box(name,a,b,material)
        e.append(p)
        return p
    # Feet and low base leave a distinct stainless bread bed above the chassis.
    for x in (1.75,12.25):
        for z in (1.75,12.25):
            add(f"rubber_foot_{x}_{z}",[x,0,z],[x+2,1,z+2],"interior")
    add("lower_chassis",[1,1,1],[15,2.5,15],"metal")
    add("front_base_trim",[1,1.8,.875],[15,2.5,1.125],"steel")
    add("bread_path_bed",[3,2.5,.75],[10.5,3,15.25],"brushed_steel")
    # Housing surrounds an open throat; no exposed blades or functional parts.
    add("left_mechanism_column",[1.75,2.5,6.5],[3.5,10,10.5],"metal")
    add("right_motor_column",[10.75,2.5,6],[14.5,9,13.75],"metal")
    add("protective_upper_bridge",[3.5,7.5,6.75],[10.75,10,10.5],"brushed_steel")
    add("dark_throat_guard",[3.5,6.75,7.5],[10.75,7.5,10],"interior")
    hood = add("sloped_protective_hood",[2.5,9.8,7],[12,11.3,11],"brushed_steel")
    hood["rotation"] = {"origin":[7.25,10.55,9],"axis":"x","angle":22.5,"rescale":False}
    add("front_housing_edge",[3.5,7.5,6.5],[10.75,8,6.75],"steel")
    add("left_side_recess",[1.625,3.75,7],[1.75,8.75,10],"service")
    e[-1]["faces"]["west"]["uv"] = [0,0,16,16]
    add("right_side_motor_vent",[14.5,4,9],[14.625,7.5,12.5],"vent")
    e[-1]["faces"]["east"]["uv"] = [0,0,16,16]
    add("rear_maintenance_frame",[11,3.25,13.75],[14.25,8,14],"steel")
    panel = add("rear_maintenance_inset",[11.25,3.5,14],[14,7.75,14.0625],"service")
    panel["faces"]["south"]["uv"] = [0,0,16,16]
    add("rear_bridge_seam",[3.5,8.25,10.5],[10.75,8.625,10.625],"metal")
    # Separate guides outline the feed/output areas without hiding the bread.
    for side,x in (("left",3.25),("right",10)):
        add(f"{side}_input_guide",[x,3,10],[x+.5,4,15],"steel")
        add(f"{side}_output_guide",[x,3,1],[x+.5,3.75,6.5],"steel")
    add("rear_feed_stop",[3.25,3,15],[10.5,3.75,15.25],"steel")
    add("front_output_lip",[3,3,.75],[10.5,3.25,1],"steel")
    # Compact facing panel alongside, rather than over, the sliced output.
    add("front_control_panel",[11,4,5.8125],[14.25,8.5,6],"metal")
    indicator = add("front_indicator",[11.3125,7,5.6875],[13.9375,8.125,5.8125],"controls")
    indicator["faces"]["north"]["uv"] = [2,2,14,7]
    start = add("start_button",[11.4375,5.5,5.5625],[12.3125,6.375,5.8125],"metal")
    stop = add("stop_button",[12.9375,5.5,5.5625],[13.8125,6.375,5.8125],"metal")
    # The indicator and both buttons use disjoint atlas regions, so colored
    # buttons are not also painted behind the protruding control geometry.
    for p,uv in ((start,[3,9,6,12]),(stop,[9,9,12,12])):
        for f in p["faces"].values():
            f.update(texture="#controls",uv=uv)
    add("adjustment_knob",[12,4.25,5.375],[13.125,5.125,5.8125],"steel")
    add("knob_index_mark",[12.4375,4.8125,5.3125],[12.6875,5.0625,5.375],"interior")
    # Whole rear loaf meets the open throat. Six broad bevel parts per bread
    # cross-section supply rounded edges without tiny stair-step cubes.
    e.extend(curved("whole_input_loaf",[4.125,3,9.375],[5.25,3.75,5.5],"crust"))
    # Five independently spaced slices, staggered gently across the output bed.
    for i,shift in enumerate((0,.125,-.125,.0625,0)):
        e.extend(curved(f"output_slice_{i}",[4.25+shift,3,1.375+i*1.025],
                         [5,3.625,.65],"crust",True))
    # Coarse conservative structure envelopes; bread slices retain real gaps.
    collision = [([1,0,.875],[15,2.5,15]),([3,2.5,.75],[10.5,3.25,15.25]),
                 ([1.625,2.5,6.5],[3.5,10,10.5]),([10.75,2.5,5.3125],[14.625,9,14.0625]),
                 ([3.5,6.75,6.5],[10.75,10,10.625])]
    collision.append(bounds(hood))
    collision += [([3.25,3,10],[3.75,4,15]),([10,3,10],[10.5,4,15]),
                  ([3.25,3,1],[3.75,3.75,6.5]),([10,3,1],[10.5,3.75,6.5]),
                  ([3.25,3,15],[10.5,3.75,15.25]),
                  ([4.125,3,9.375],[9.375,6.75,14.875])]
    collision += [([4.25+s,3,1.375+i*1.025],[9.25+s,6.625,2.025+i*1.025])
                  for i,s in enumerate((0,.125,-.125,.0625,0))]
    return e,collision


def arrangement():
    e,collision = geometry()
    omit_hidden_faces(e)
    validate_model_elements(e)
    for p in e:
        lo,hi = bounds(p)
        assert all(-1e-6 <= lo[i] < hi[i] <= 16+1e-6 for i in range(3)), (p["name"],lo,hi)
        for face in p["faces"].values():
            assert face["texture"][1:] in MATERIALS
            assert all(0 <= v <= 16 for v in face["uv"])
    used = {f["texture"][1:] for p in e for f in p["faces"].values()} | {"particle"}
    return {"parent":"minecraft:block/block","textures":{k:v for k,v in MATERIALS.items() if k in used},"elements":e},collision


def write_shapes(collision):
    rows = []
    for i,(lo,hi) in enumerate(collision):
        coords = ", ".join(f"{v:.6f}".rstrip("0").rstrip(".") for v in lo+hi)
        rows.append("            Block.box("+coords+")"+( "," if i < len(collision)-1 else ").optimize());"))
    source = '''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_bakery_bread_slicer.py; edit envelopes there. */
final class BreadSlicerShapes {
    static final VoxelShape[] SHAPES = rotations(Shapes.or(
''' + "\n".join(rows) + '''

    private BreadSlicerShapes() {
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] shapes = new VoxelShape[4];
        shapes[0] = north;
        for (int turn = 1; turn < 4; turn++) {
            VoxelShape result = Shapes.empty();
            for (AABB box : shapes[turn - 1].toAabbs()) {
                result = Shapes.or(result, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            shapes[turn] = result.optimize();
        }
        return shapes;
    }
}
'''
    (ROOT / "src/main/java/com/craftdistrict/block/BreadSlicerShapes.java").write_text(source,encoding="utf-8")


def main():
    textures()
    model,collision = arrangement()
    write_json(MODELS / f"{ID}.json",model)
    write_json(ASSETS / f"blockstates/{ID}.json",{"variants":{
        f"facing={face}":{"model":f"craftdistrict:block/bakery/{ID}","y":angle}
        for face,angle in (("north",0),("east",90),("south",180),("west",270))}})
    item = copy.deepcopy(model)
    item["display"] = {
        "gui":{"rotation":[25,135,0],"scale":[.72,.72,.72]},
        "ground":{"translation":[0,2,0],"scale":[.35,.35,.35]},
        "fixed":{"rotation":[0,180,0],"scale":[.65,.65,.65]},
        "thirdperson_righthand":{"rotation":[75,45,0],"scale":[.4,.4,.4]},
        "thirdperson_lefthand":{"rotation":[75,45,0],"scale":[.4,.4,.4]},
        "firstperson_righthand":{"rotation":[0,45,0],"scale":[.6,.6,.6]},
        "firstperson_lefthand":{"rotation":[0,225,0],"scale":[.6,.6,.6]}}
    write_json(ASSETS / f"models/item/{ID}.json",item)
    write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{ID}.json", {
        "type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"craftdistrict:{ID}"}],
        "conditions":[{"condition":"minecraft:survives_explosion"}]}]})
    write_shapes(collision)
    print(f"{ID}: {len(model['elements'])} visible cuboids, {sum(len(p['faces']) for p in model['elements'])} faces; {len(collision)} collision envelopes cached in four rotations.")


if __name__ == "__main__":
    main()
