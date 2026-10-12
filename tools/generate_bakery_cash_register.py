"""Editable static register: Python 3, standard library only, north (-Z) is front.

Generates only register resources. Edit named cuboids and pixel samplers below.
Slope is the vanilla -22.5 degree X rotation; all keypad parts share its pivot.
Model units are 1/16 block. Existing bakery assets are only referenced.
"""

import copy
import struct
import sys
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, TEXTURE_MAP, write_json

ID = "bakery_cash_register"
FACES = ("north", "south", "west", "east", "up", "down")
TILES = ("body", "trim", "black", "display", "keys", "receipt", "rear")
MATERIALS = {name: f"craftdistrict:block/bakery/{ID}_{name}" for name in TILES}
MATERIALS["wood"] = TEXTURE_MAP["trim"]
MATERIALS["particle"] = MATERIALS["body"]
GLYPHS = {
    "0": ("111", "101", "101", "101", "111"),
    "1": ("010", "110", "010", "010", "111"),
    "2": ("111", "001", "111", "100", "111"),
    "3": ("111", "001", "111", "001", "111"),
    "4": ("101", "101", "111", "001", "001"),
    "5": ("111", "100", "111", "001", "111"),
    "6": ("111", "100", "111", "101", "111"),
    "7": ("111", "001", "010", "010", "010"),
    "8": ("111", "101", "111", "101", "111"),
    "9": ("111", "101", "111", "001", "111"),
    "C": ("111", "100", "100", "100", "111"),
    ">": ("100", "010", "001", "010", "100"),
    ".": ("000", "000", "000", "000", "010"),
    "-": ("000", "000", "111", "000", "000"),
}
KEY_LABELS = (("1", "2", "3", "C"), ("4", "5", "6", ">"),
              ("7", "8", "9", "-"), (".", "0", "-", ">"))


def png(name, size, pixel):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    width, height = size
    rows = b"".join(b"\0" + bytes(c for x in range(width) for c in pixel(x, y)) for y in range(height))
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b"")
    path = ASSETS / f"textures/block/bakery/{ID}_{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def textures():
    def display(x, y):
        if x in (0, 31) or y in (0, 15):
            return (71, 88, 75, 255)
        for text, sx in (("1", 4), ("2", 9), ("5", 18), ("0", 23)):
            dx, dy = x-sx, y-5
            if 0 <= dx < 3 and 0 <= dy < 5 and GLYPHS[text][dy][dx] == "1":
                return (179, 215, 135, 255)
        if x == 15 and y == 9:
            return (179, 215, 135, 255)
        return (18, 31, 26, 255)
    def keys(x, y):
        column, row = x//8, y//8
        px, py = x%8, y%8
        color = (126, 133, 132) if column < 3 else (131, 78, 69) if row == 0 else (81, 115, 86)
        if px in (0,7) or py in (0,7):
            return (43, 49, 51, 255)
        dx, dy = px-2, py-1
        if 0 <= dx < 3 and 0 <= dy < 5 and GLYPHS[KEY_LABELS[row][column]][dy][dx] == "1":
            return (29, 34, 35, 255)
        return (*color, 255)
    def rear(x, y):
        if x in (1,14) or y in (1,14):
            return (84, 91, 94, 255)
        if x in (3,12) and y in (3,12):
            return (127, 132, 132, 255)
        if 4 <= x <= 11 and y in (6,8,10):
            return (20, 25, 28, 255)
        return (48, 54, 58, 255)
    png("body", (16,16), lambda x,y: ((47,52,57,255),(53,58,63,255),(58,63,68,255))[(x//4+y//5)%3])
    png("trim", (16,16), lambda x,y: ((93,101,105,255),(108,115,117,255))[(x//7+y//2)%2])
    png("black", (16,16), lambda x,y: (23,27,30,255) if (x+y)%7 else (29,33,36,255))
    png("display", (32,16), display)
    png("keys", (32,32), keys)
    png("receipt", (16,16), lambda x,y: (115,119,112,255) if 3<=x<=12 and y in (4,7,10) else (223,224,209,255))
    png("rear", (16,16), rear)


def box(name, start, end, material, full=False, only=FACES):
    x0,y0,z0 = start
    x1,y1,z1 = end
    uv = {"north":[x0,16-y1,x1,16-y0], "south":[16-x1,16-y1,16-x0,16-y0],
          "west":[16-z1,16-y1,16-z0,16-y0], "east":[z0,16-y1,z1,16-y0],
          "up":[x0,z0,x1,z1], "down":[x0,16-z1,x1,16-z0]}
    return {"name":name, "from":start, "to":end, "faces":{
        f:{"texture":"#"+material, "uv":[0,0,16,16] if full else uv[f]} for f in only}}


def keypad_slope(element):
    element["rotation"] = {"origin":[7,3.5,7], "axis":"x", "angle":-22.5, "rescale":False}
    return element


def screen(name, start, end, face):
    # Lettering belongs only on the outward screen. Wrapping the readout onto
    # thin edges repeats/compresses it and reverses its top-face presentation.
    element = box(name, start, end, "black", True)
    element["faces"][face] = {"texture":"#display", "uv":[0,0,16,16]}
    return element


def key_label(row, column):
    # Vanilla UP UVs run +X/+Z. The operator stands at -Z, so rotate the
    # cell 180 degrees (both axes, not a mirror) to read it from the front.
    return {"texture":"#keys", "uv":[column*4,row*4,(column+1)*4,(row+1)*4], "rotation":180}


def register_model():
    elements = [
        box("cash_drawer_base", [2.25,0,2.5], [13.75,2.25,13.875], "body"),
        box("drawer_front", [2.5,.5,2.375], [13.5,1.875,2.5], "trim"),
        box("drawer_seam", [2.75,1.75,2.3125], [13.25,1.875,2.375], "black"),
        box("drawer_handle", [6,.875,2], [10,1.25,2.375], "trim"),
        box("left_wood_inlay", [2,.5,4], [2.25,1.75,12.75], "wood"),
        box("right_wood_inlay", [13.75,.5,4], [14,1.75,12.75], "wood"),
        box("lower_housing", [2.75,2.25,6], [13.25,3.25,13.25], "body"),
        box("rear_housing_step", [2.75,3.25,10], [13.25,4.5,13.25], "body"),
        keypad_slope(box("sloped_keypad_deck", [3,3.5,3.75], [11,4.125,10.25], "black")),
        box("printer_housing", [11,2.25,6], [13.25,5.75,13.25], "body"),
        box("receipt_slot", [11.25,5.625,7.375], [13,5.75,7.875], "black"),
        box("short_receipt", [11.375,5.75,7.25], [12.875,6.5,7.375], "receipt", True),
        box("display_stem", [5.75,4.5,10.75], [7.25,7.25,12.25], "black"),
        box("raised_display_bezel", [3.25,7,10.75], [11.25,10,12.5], "body"),
        screen("price_display", [3.75,7.5,10.625], [10.75,9.5,10.75], "north"),
        screen("customer_display", [4.75,8,12.5], [9.75,9.5,12.625], "south"),
        box("display_rear_wood_trim", [3.75,7.25,12.5], [10.75,7.5,12.625], "wood"),
        box("rear_service_panel", [3,.5,13.875], [13,2,14], "rear", True),
        box("rear_cable_socket", [10.5,2.5,13.25], [12,3.25,13.375], "black"),
    ]
    for row in range(4):
        for column in range(3):
            x, z = 3.5+column*1.875, 4+row*1.375
            # From the north-facing operator's view, +X is left and +Z is
            # the far/top row. Reverse cell selection as well as glyph rotation.
            label_row, label_column = 3-row, 2-column
            key = box(f"key_{KEY_LABELS[label_row][label_column]}_{row}_{column}", [x,4.125,z], [x+1.25,4.375,z+1], "trim")
            key["faces"]["up"] = key_label(label_row, label_column)
            elements.append(keypad_slope(key))
    for name, z0, z1, row in (("clear",4,5,0), ("confirm",6.75,9.125,1)):
        key = box(f"{name}_key", [9.25,4.125,z0], [10.5,4.375,z1], "trim")
        key["faces"]["up"] = key_label(row, 3)
        elements.append(keypad_slope(key))
    return {"parent":"minecraft:block/block", "textures":MATERIALS, "elements":elements}


def main():
    textures()
    model = register_model()
    write_json(MODELS / f"{ID}.json", model)
    write_json(ASSETS / f"blockstates/{ID}.json", {"variants":{
        f"facing={facing}":{"model":f"craftdistrict:block/bakery/{ID}", "y":angle}
        for facing,angle in (("north",0),("east",90),("south",180),("west",270))}})
    item = copy.deepcopy(model)
    item["display"] = {
        "gui":{"rotation":[30,135,0], "translation":[0,2,0], "scale":[.8,.8,.8]},
        "ground":{"translation":[0,3,0], "scale":[.5,.5,.5]},
        "fixed":{"rotation":[0,180,0], "translation":[0,2,0], "scale":[.65,.65,.65]},
        "thirdperson_righthand":{"rotation":[75,45,0], "translation":[0,2.5,0], "scale":[.45,.45,.45]},
        "thirdperson_lefthand":{"rotation":[75,45,0], "translation":[0,2.5,0], "scale":[.45,.45,.45]},
        "firstperson_righthand":{"rotation":[0,45,0], "scale":[.6,.6,.6]},
        "firstperson_lefthand":{"rotation":[0,225,0], "scale":[.6,.6,.6]},
    }
    write_json(ASSETS / f"models/item/{ID}.json", item)
    write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{ID}.json", {
        "type":"minecraft:block", "pools":[{"rolls":1, "entries":[{"type":"minecraft:item", "name":f"craftdistrict:{ID}"}],
        "conditions":[{"condition":"minecraft:survives_explosion"}]}]})
    print(f"Generated {ID}: {len(model['elements'])} cuboids, {len(TILES)} own textures, four facing variants.")


if __name__ == "__main__":
    main()
