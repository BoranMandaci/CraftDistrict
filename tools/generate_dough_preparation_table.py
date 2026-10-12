"""Editable 32x16x16 workbench; north (-Z) is front, primary is x=0..16.

Python standard library only. Generates only dough_preparation_table resources.
Named furniture and prop groups use vanilla cuboids and one 22.5-degree Y turn.
Texture UV directions follow Minecraft FaceInfo, including cropped seam UVs.
"""

import copy
import struct
import sys
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, write_json

ID = "dough_preparation_table"
FACES = ("north", "south", "west", "east", "up", "down")
SIDES_TOP = tuple(f for f in FACES if f != "down")
TILES = ("wood", "edge", "worktop", "dough", "cloth", "bag_front", "flour", "bowl")
MATERIALS = {name: f"craftdistrict:block/bakery/{ID}_{name}" for name in TILES}
MATERIALS["particle"] = MATERIALS["wood"]


def png(name, size, sample):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    width, height = size
    rows = b"".join(b"\0" + bytes(c for x in range(width) for c in sample(x, y)) for y in range(height))
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b"")
    path = ASSETS / f"textures/block/bakery/{ID}_{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def grain(x, y, edge=False):
    # Warmer, lighter than the display furniture, with restrained plank seams.
    palette = ((126,87,48), (171,126,76), (186,142,89), (198,155,101)) if edge else (
        (150,108,63), (191,148,96), (202,160,107), (214,174,121))
    index = 0 if y in (7,15) else 1 + ((x//5 + y//2 + x*y//17) % 3)
    if (x + y*3) % 19 == 0:
        index = 1
    return (*palette[index], 255)


def cloth(x, y):
    if x in (1,14) or y == 14:
        return (160,141,107,255)
    return ((210,194,160,255), (215,200,166,255), (219,204,172,255))[(x//4+y//3)%3]


def textures():
    def worktop(x, y):
        # Flour is part of the surface texture, not floating overlay geometry.
        patches = ((6,5,4,2), (14,10,3,2), (23,12,3,1))
        for cx,cy,rx,ry in patches:
            if ((x-cx)/rx)**2 + ((y-cy)/ry)**2 < 1 and (x*3+y*5)%7 < 4:
                return ((235,224,196,255),(219,207,177,255))[(x+y)%2]
        return grain(x,y)
    def bag_front(x, y):
        # Upright wheat emblem, mapped only to the bag's outward NORTH face.
        if x == 8 and 4 <= y <= 11 or (x,y) in ((6,4),(7,5),(10,5),(9,6),(6,7),(7,8),(10,8),(9,9)):
            return (134,94,49,255)
        return cloth(x,y)
    png("wood", (16,16), grain)
    png("edge", (16,16), lambda x,y: grain(x,y,True))
    png("worktop", (32,16), worktop)
    png("cloth", (16,16), cloth)
    png("bag_front", (16,16), bag_front)
    png("dough", (16,16), lambda x,y: (228,207,162,255) if (x*7+y*11+x*y)%29 < 3 else (239,219,177,255))
    png("flour", (16,16), lambda x,y: (226,219,194,255) if (x*3+y*7+x*y)%23 < 3 else (241,234,213,255))
    png("bowl", (16,16), lambda x,y: (160,169,154,255) if y in (0,15) else (
        (195,202,185,255) if (x+y//3)%7 == 0 else (180,190,174,255)))


def box(name, start, end, material, only=FACES, top=None):
    return {"name":name, "from":start, "to":end, "faces":{
        f:{"texture":"#"+(top if f == "up" and top else material), "uv":[0,0,16,16]}
        for f in only}}


def furniture():
    e = [
        box("preparation_top", [0,11,.5], [32,12.5,15.5], "edge", top="worktop"),
        box("front_apron", [1.25,9.5,1.25], [30.75,11,2.25], "edge", only=tuple(f for f in FACES if f != "up")),
        box("rear_apron", [1.25,9.5,13.75], [30.75,11,14.75], "edge", only=tuple(f for f in FACES if f != "up")),
        box("storage_shelf", [1.5,2.5,2], [30.5,3.25,14], "wood"),
        box("front_shelf_edge", [3.25,2,2], [28.75,2.5,2.75], "edge", only=tuple(f for f in FACES if f != "up")),
        box("rear_shelf_edge", [3.25,2,13.25], [28.75,2.5,14], "edge", only=tuple(f for f in FACES if f != "up")),
    ]
    for label,x in (("left",1.25),("right",28.75)):
        for side,z in (("front",1.25),("rear",12.75)):
            leg = box(f"{label}_{side}_leg", [x,0,z], [x+2,11,z+2], "wood",
                      only=tuple(f for f in FACES if f != "up"))
            # Grain follows the posts rather than reading as stacked tiny boards.
            for face in ("north","south","west","east"):
                leg["faces"][face]["rotation"] = 90
            e.append(leg)
        e.extend([
            box(f"{label}_upper_cross_support", [x,9.5,3.25], [x+2,11,12.75], "edge", only=tuple(f for f in FACES if f != "up")),
            box(f"{label}_lower_cross_support", [x,4,3.25], [x+2,4.75,12.75], "edge"),
        ])
    return e


def dough_and_pin():
    e = [
        box("dough_sheet_center", [4,12.5,2.75], [12,12.875,7.5], "dough", SIDES_TOP),
        box("dough_sheet_left_lobe", [3.5,12.5,3.5], [4,12.75,6.75], "dough", tuple(f for f in SIDES_TOP if f != "east")),
        box("dough_sheet_right_lobe", [12,12.5,3.5], [12.5,12.75,6.5], "dough", tuple(f for f in SIDES_TOP if f != "west")),
        box("dough_ball_contact", [18.75,12.5,4.25], [21.25,12.875,6.75], "dough", SIDES_TOP),
        box("dough_ball_body", [18,12.875,4], [22,14.25,7], "dough", SIDES_TOP),
        box("dough_ball_front_shoulder", [18.5,12.875,3.5], [21.5,14.25,4], "dough", tuple(f for f in SIDES_TOP if f != "south")),
        box("dough_ball_rear_shoulder", [18.5,12.875,7], [21.5,14.25,7.5], "dough", tuple(f for f in SIDES_TOP if f != "north")),
        box("dough_ball_crown", [18.5,14.25,4.5], [21.5,15,6.5], "dough", SIDES_TOP),
        box("dough_ball_front_crown", [19,14.25,4], [21,15,4.5], "dough", tuple(f for f in SIDES_TOP if f != "south")),
        box("dough_ball_rear_crown", [19,14.25,6.5], [21,15,7], "dough", tuple(f for f in SIDES_TOP if f != "north")),
        box("dough_ball_top", [19.25,15,4.75], [20.75,15.25,6.25], "dough", SIDES_TOP),
    ]
    # Seven broad pieces give a stepped round barrel, narrower necks and handles.
    pin = [
        box("rolling_pin_barrel", [5.5,12.5,9], [12.5,14.5,10], "wood"),
        box("rolling_pin_front_bevel", [5.5,13,8.5], [12.5,14,9], "wood", tuple(f for f in FACES if f != "south")),
        box("rolling_pin_rear_bevel", [5.5,13,10], [12.5,14,10.5], "wood", tuple(f for f in FACES if f != "north")),
        box("rolling_pin_left_neck", [5,13,9], [5.5,14,10], "edge", tuple(f for f in FACES if f != "east")),
        box("rolling_pin_right_neck", [12.5,13,9], [13,14,10], "edge", tuple(f for f in FACES if f != "west")),
        box("rolling_pin_left_handle", [3.5,13.125,9.125], [5,13.875,9.875], "edge", tuple(f for f in FACES if f != "east")),
        box("rolling_pin_right_handle", [13,13.125,9.125], [14.5,13.875,9.875], "edge", tuple(f for f in FACES if f != "west")),
    ]
    for element in pin:
        element["rotation"] = {"axis":"y", "angle":22.5, "origin":[9,13.5,9.5], "rescale":False}
    return e + pin


def bowl_and_flour_bag():
    e = [
        box("bowl_foot", [25.25,12.5,4], [28.75,13,7], "bowl", SIDES_TOP),
        box("bowl_front_wall", [25,13,3.25], [29,14.5,4], "bowl"),
        box("bowl_rear_wall", [25,13,7], [29,14.5,7.75], "bowl"),
        box("bowl_left_wall", [24.5,13,4], [25.25,14.5,7], "bowl"),
        box("bowl_right_wall", [28.75,13,4], [29.5,14.5,7], "bowl"),
        box("bowl_front_rim", [25,14.5,3.125], [29,15,4.125], "bowl", SIDES_TOP),
        box("bowl_rear_rim", [25,14.5,6.875], [29,15,7.875], "bowl", SIDES_TOP),
        box("bowl_left_rim", [24.375,14.5,4.125], [25.375,15,6.875], "bowl", SIDES_TOP),
        box("bowl_right_rim", [28.625,14.5,4.125], [29.625,15,6.875], "bowl", SIDES_TOP),
        box("flour_in_mixing_bowl", [25.25,13,4], [28.75,13.125,7], "flour", ("up",)),
        box("flour_bag_rounded_base", [23,12.5,9.5], [26,13.2,13], "cloth", SIDES_TOP),
        box("flour_bag_body", [22.5,13.2,9], [26.5,15.125,13.5], "cloth", SIDES_TOP),
        box("flour_bag_mouth_front", [22.75,15.125,9.75], [26.25,16,10.25], "cloth", SIDES_TOP),
        box("flour_bag_mouth_rear", [22.75,15.125,12.75], [26.25,15.875,13.25], "cloth", SIDES_TOP),
        box("flour_bag_mouth_left", [22.75,15.125,10.25], [23.25,15.875,12.75], "cloth", SIDES_TOP),
        box("flour_bag_mouth_right", [25.75,15.125,10.25], [26.25,16,12.75], "cloth", SIDES_TOP),
        box("visible_flour_in_open_bag", [23.25,15.125,10.25], [25.75,15.375,12.75], "flour", ("up",)),
    ]
    next(part for part in e if part["name"] == "flour_bag_body")["faces"]["north"]["texture"] = "#bag_front"
    return e


def storage():
    e = [
        box("storage_crate_floor", [4,3.25,6], [12,3.75,12], "edge", SIDES_TOP),
        box("storage_crate_front", [4,3.75,6], [12,6.25,6.625], "wood", SIDES_TOP),
        box("storage_crate_back", [4,3.75,11.375], [12,6.75,12], "wood", SIDES_TOP),
        box("storage_crate_left", [4,3.75,6.625], [4.625,6.75,11.375], "edge", SIDES_TOP),
        box("storage_crate_right", [11.375,3.75,6.625], [12,6.75,11.375], "edge", SIDES_TOP),
        box("crate_inner_wooden_board", [5.25,3.75,8], [10.5,4.375,10.5], "wood", SIDES_TOP),
    ]
    for name,x,z,w,d,h in (("large_storage_sack",18,6,4.5,5,4.25), ("small_storage_sack",24.5,7,3.5,4.5,3)):
        e.extend([
            box(f"{name}_body", [x,3.25,z], [x+w,3.25+h,z+d], "cloth", SIDES_TOP),
            box(f"{name}_shoulder", [x+.5,3.25+h,z+.5], [x+w-.5,3.75+h,z+d-.5], "cloth", SIDES_TOP),
            box(f"{name}_tied_neck", [x+w/2-.5,3.75+h,z+d/2-.5], [x+w/2+.5,4+h,z+d/2+.5], "edge", SIDES_TOP),
        ])
    return e


def split_part(element, primary):
    """Clip along X only, preserving vanilla face UV directions at the joint."""
    offset = 0 if primary else 16
    a, b = element["from"], element["to"]
    low, high = max(a[0],offset), min(b[0],offset+16)
    if low >= high:
        return None
    result = copy.deepcopy(element)
    if "rotation" in element:
        # All angled pin pieces stay inside the primary; never clip a rotated mesh.
        assert low == a[0] and high == b[0]
        result["rotation"]["origin"][0] -= offset
    result["from"][0], result["to"][0] = low-offset, high-offset
    if low != a[0]:
        result["faces"].pop("west",None)
    if high != b[0]:
        result["faces"].pop("east",None)
    for face in ("north","south","up","down"):
        if face not in result["faces"]:
            continue
        original = element["faces"][face]["uv"]
        # NORTH U runs toward -X; other spanning faces run toward +X.
        fractions = ((b[0]-high)/(b[0]-a[0]), (b[0]-low)/(b[0]-a[0])) if face == "north" else (
            (low-a[0])/(b[0]-a[0]), (high-a[0])/(b[0]-a[0]))
        assert not element["faces"][face].get("rotation",0) or low == a[0] and high == b[0]
        result["faces"][face]["uv"] = [round(original[0]+(original[2]-original[0])*fractions[0],6),
            original[1], round(original[0]+(original[2]-original[0])*fractions[1],6), original[3]]
    return result


def main():
    textures()
    elements = furniture() + dough_and_pin() + bowl_and_flour_bag() + storage()
    variants, counts = {}, []
    for primary, name in ((True,"primary"),(False,"extension")):
        pieces = [piece for e in elements if (piece := split_part(e,primary)) is not None]
        counts.append(len(pieces))
        write_json(MODELS / f"{ID}_{name}.json", {"parent":"minecraft:block/block", "textures":MATERIALS, "elements":pieces})
        for facing,angle in (("north",0),("east",90),("south",180),("west",270)):
            variants[f"facing={facing},primary={str(primary).lower()}"] = {
                "model":f"craftdistrict:block/bakery/{ID}_{name}", "y":angle}
    write_json(ASSETS / f"blockstates/{ID}.json", {"variants":variants})
    item = copy.deepcopy(elements)
    for element in item:
        element["from"][0] -= 8
        element["to"][0] -= 8
        if "rotation" in element:
            element["rotation"]["origin"][0] -= 8
    write_json(ASSETS / f"models/item/{ID}.json", {"parent":"minecraft:block/block", "textures":MATERIALS,
        "elements":item, "display":{
            "gui":{"rotation":[30,135,0], "scale":[.43,.43,.43]},
            "ground":{"translation":[0,3,0], "scale":[.25,.25,.25]},
            "fixed":{"rotation":[0,180,0], "scale":[.4,.4,.4]},
            "thirdperson_righthand":{"rotation":[75,45,0], "translation":[0,2.5,0], "scale":[.3,.3,.3]},
            "thirdperson_lefthand":{"rotation":[75,45,0], "translation":[0,2.5,0], "scale":[.3,.3,.3]},
            "firstperson_righthand":{"rotation":[0,45,0], "scale":[.3,.3,.3]},
            "firstperson_lefthand":{"rotation":[0,225,0], "scale":[.3,.3,.3]}}})
    write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{ID}.json", {
        "type":"minecraft:block", "pools":[{"rolls":1, "entries":[{"type":"minecraft:item", "name":f"craftdistrict:{ID}"}],
        "conditions":[{"condition":"minecraft:survives_explosion"}]}]})
    print(f"Generated {ID}: {len(elements)} source cuboids, {counts} per part, {len(TILES)} opaque textures, 8 states.")


if __name__ == "__main__":
    main()
