"""Police collection: two-cell desk/sign and three-cell waiting bench.

North (-Z) is the public/front face; widths are authored across X=0..32/48.
Only police/ assets and PoliceEntranceShapes.java are written. Existing JSON
clipping/face-validation and the preview renderer are reused without regeneration
of Bakery content. Pillow is used for deterministic pixel-art textures.
"""

import copy
import sys

sys.dont_write_bytecode = True
from PIL import Image, ImageDraw, ImageFont
from generate_bakery_display_counter import ASSETS, ROOT, write_json
from generate_flour_sacks import bounds, omit_hidden_faces, validate_model_elements

IDS = ("police_reception_desk", "police_department_sign", "police_waiting_area_bench")
MODELS = ASSETS / "models/block/police"
TILES = ("navy", "metal", "light", "seat", "dark", "paper", "folder", "monitor", "keyboard", "phone", "shield", "label")
MATERIALS = {name:f"craftdistrict:block/police/entrance_{name}" for name in TILES}
MATERIALS["particle"] = MATERIALS["navy"]
FACES = ("north", "south", "west", "east", "up", "down")
GLYPHS = {
    "P":("11110","10001","10001","11110","10000","10000","10000"),
    "O":("01110","10001","10001","10001","10001","10001","01110"),
    "L":("10000","10000","10000","10000","10000","10000","11111"),
    "I":("11111","00100","00100","00100","00100","00100","11111"),
    "C":("01111","10000","10000","10000","10000","10000","01111"),
    "E":("11111","10000","10000","11110","10000","10000","11111"),
}


def textures():
    palette = {"navy":(38,54,76),"metal":(136,148,158),"light":(207,215,221),
               "seat":(77,99,124),"dark":(34,40,47),"paper":(226,230,231),"folder":(94,109,125)}
    for name in TILES:
        image = Image.new("RGBA",(64,16) if name == "label" else (16,16))
        for y in range(image.height):
            for x in range(image.width):
                if name in palette:
                    shade = (-5 if (x*3+y*11)%29 == 0 else 2 if (x+y)%7 == 0 else 0)
                    if name == "metal": shade += (y%3)-1
                    if name == "seat" and x in (1,14) and 2 <= y <= 13: shade -= 9
                    color = tuple(v+shade for v in palette[name])
                    if name == "paper" and y in (5,8,11) and 3 <= x <= 11: color=(148,162,175)
                elif name == "monitor":
                    color=(27,49,61)
                    if 1 <= y <= 3 and 1 <= x <= 14: color=(61,104,131)
                    # Generic shield/status tile left; a few clear case rows right.
                    if 5 <= y <= 8 and 2 <= x <= 5 or 9 <= y <= 11 and abs(x-3.5) <= 11-y: color=(140,179,194)
                    if y in (6,9,12) and 8 <= x <= (13 if y == 6 else 11): color=(132,166,183)
                    if x == 13 and y in (9,12): color=(85,156,135)
                elif name == "keyboard":
                    color=(42,49,57)
                    if 1 <= x <= 14 and 3 <= y <= 12 and x%3 != 0 and y%3 != 0: color=(126,138,146)
                    if 5 <= x <= 10 and y == 13: color=(146,156,164)
                elif name == "phone":
                    color=(53,61,70)
                    if 2 <= x <= 13 and 1 <= y <= 5: color=(23,41,44)
                    if 3 <= x <= 10 and y in (2,4): color=(127,175,163)
                    if x in (3,4,7,8,11,12) and y in (8,9,11,12,14,15): color=(181,190,196)
                elif name == "shield":
                    color=(38,54,76)
                    if (3 <= y <= 8 and 4 <= x <= 11) or (9 <= y <= 12 and abs(x-7.5) <= 12-y): color=(207,216,223)
                    if 6 <= x <= 9 and 5 <= y <= 9: color=(84,117,146)
                else:
                    color=(38,54,76)
                    if y in (1,14) and 2 <= x <= 61: color=(103,126,151)
                    if 3 <= y <= 8 and 4 <= x <= 11 or 9 <= y <= 12 and abs(x-7.5) <= 12-y: color=(207,216,223)
                    if 6 <= x <= 9 and 5 <= y <= 9: color=(84,117,146)
                image.putpixel((x,y),(*color,255))
        if name == "label":
            for index,letter in enumerate("POLICE"):
                for y,row in enumerate(GLYPHS[letter]):
                    for x,pixel in enumerate(row):
                        if pixel == "1": image.putpixel((18+index*7+x,4+y),(236,241,245,255))
        path = ASSETS / f"textures/block/police/entrance_{name}.png"
        path.parent.mkdir(parents=True,exist_ok=True)
        image.save(path)


def box(name,a,b,material):
    # Outward UV bases: north increases toward -X; south toward +X.
    # Each named panel owns a texture tile. Splitting crops this continuous tile.
    return {"name":name,"from":list(a),"to":list(b),
            "faces":{f:{"texture":"#"+material,"uv":[0,0,16,16]} for f in FACES}}


def reception_desk():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    add("recessed_dark_plinth",[1,0,2.5],[31,1,13.5],"dark")
    add("public_front_panel",[.5,1,2],[31.5,15,3],"navy")
    add("front_lower_metal_strip",[.5,1,1.875],[31.5,1.5,2],"metal")
    add("left_end_panel",[.5,1,3],[1.5,13.5,14],"navy")
    add("right_end_panel",[30.5,1,3],[31.5,13.5,14],"navy")
    add("left_high_counter_return",[.5,13.5,3],[1.5,15,5.5],"navy")
    add("right_high_counter_return",[30.5,13.5,3],[31.5,15,5.5],"navy")
    add("right_storage_pedestal",[17.5,1,3],[30.5,13.5,13],"navy")
    add("offset_knee_space_crossbar",[1.5,2.5,3],[17.5,3.25,4],"metal")
    add("light_staff_work_surface",[0,13.5,5.5],[32,14.5,15],"light")
    add("light_public_counter_ledge",[0,15,1],[32,16,5.5],"light")
    add("front_counter_edge",[0,14.75,.875],[32,15.5,1],"metal")
    shield=add("generic_front_shield",[13,6.25,1.9375],[19,10.25,2],"navy")
    shield["faces"]["north"]["texture"]="#shield"
    for side,x0,x1 in (("inner",18,23.75),("outer",24,30.25)):
        for drawer,y0,y1 in (("upper",8,13),("lower",1.75,7.75)):
            add(f"{side}_{drawer}_drawer",[x0,y0,13],[x1,y1,13.125],"folder")
            add(f"{side}_{drawer}_handle",[x0+1,y1-.875,13.125],[x1-1,y1-.5,13.375],"metal")
    props_start=len(e)
    add("monitor_foot",[6.5,11.5,7.75],[11.5,11.875,10.25],"dark")
    add("monitor_foot_metal_inset",[7,11.875,8.125],[11,12,9.875],"metal")
    add("monitor_stand",[8.5,12,8.4375],[9.5,14.75,9.25],"metal")
    add("monitor_housing",[3.5,14.25,8.25],[14.5,20.75,9.125],"dark")
    add("monitor_lower_bezel",[3.5,14.25,9.125],[14.5,14.75,9.25],"metal")
    screen=add("monitor_staff_facing_screen",[3.875,14.75,9.125],[14.125,20.375,9.1875],"dark")
    screen["faces"]["south"]["texture"]="#monitor"
    keyboard=add("keyboard",[11,11.5,10.5],[20,11.875,14],"dark")
    keyboard["faces"]["up"]["texture"]="#keyboard"
    add("telephone_base",[20,11.5,8.25],[25.75,12,13.75],"dark")
    add("telephone_control_body",[21.75,12,8.5],[25.5,12.25,13.5],"dark")
    add("telephone_rear_riser",[21.75,12.25,8.5],[25.5,12.625,9.125],"dark")
    phone=add("telephone_keypad_and_display",[22,12.25,9.125],[25.25,12.375,13.25],"dark")
    phone["faces"]["up"]["texture"]="#phone"
    add("telephone_front_trim",[21.75,11.8125,13.75],[25.5,12,13.875],"metal")
    # Handset beside the controls: two broad receiver ends joined by a narrower
    # grip with real clearance below it. It does not cover the keypad/display.
    add("telephone_handset_front_receiver",[20,12,8.75],[21.5,13,10],"dark")
    add("telephone_handset_rear_receiver",[20,12,12],[21.5,13,13.25],"dark")
    add("telephone_handset_grip",[20.375,12.5,9.75],[21.125,13,12.25],"dark")
    add("document_folder",[14,11.5,10],[18.5,11.75,13.5],"folder")
    paper=add("paperwork",[14.5,11.75,10.25],[18,11.875,13.25],"light")
    paper["faces"]["up"]["texture"]="#paper"
    add("pen_cup",[28,11.5,10],[29.5,13.125,11.5],"metal")
    add("pen_cup_dark_opening",[28.25,13.125,10.25],[29.25,13.1875,11.25],"dark")
    add("pen_one",[28.375,13.1875,10.4375],[28.625,14.5,10.6875],"dark")
    add("pen_two",[28.875,13.1875,10.8125],[29.125,14.125,11.0625],"navy")
    for p in e[props_start:]:
        dx=.5 if p['name'].startswith('monitor_') else -6 if p['name']=='keyboard' else 4 if p['name'] in ('document_folder','paperwork') or p['name'].startswith('telephone_') else -8 if p['name'].startswith('pen_') else 0
        dz=-3 if p['name'].startswith('pen_') else 0
        for key in ('from','to'):
            p[key][0]+=dx
            p[key][1]+=3
            p[key][2]+=dz
    return e


def department_sign():
    e = [
        box("metal_wall_backing",[0,3,15.5],[32,13.5,16],"metal"),
        box("navy_plaque_body",[1,3.75,14.875],[31,12.75,15.5],"navy"),
        box("top_frame",[0,12.75,14.625],[32,13.5,15.5],"light"),
        box("bottom_frame",[0,3,14.625],[32,3.75,15.5],"light"),
        box("left_frame",[0,3.75,14.625],[1,12.75,15.5],"metal"),
        box("right_frame",[31,3.75,14.625],[32,12.75,15.5],"metal"),
        box("outward_police_lettering",[2,5,14.8125],[30,12,14.875],"navy"),
    ]
    e[-1]["faces"]["north"]["texture"]="#label"
    return e


def waiting_bench():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    add("connected_under_seat_beam",[1,4.5,7],[47,5.5,10.5],"metal")
    add("back_support_crossbar",[1,7.5,11.5],[47,8.125,12.25],"metal")
    for side,x in (("left",7),("right",39)):
        add(f"{side}_floor_foot",[x,0,3],[x+2,1,13],"dark")
        add(f"{side}_metal_leg",[x+.25,1,7.25],[x+1.75,5.5,8.75],"metal")
        add(f"{side}_back_support",[x+.5,5.5,11.5],[x+1.5,8.125,12.25],"metal")
    add("center_floor_foot",[23,0,3],[25,1,13],"dark")
    add("center_metal_leg",[23.25,1,7.25],[24.75,5.5,8.75],"metal")
    for index,x in enumerate((1.5,17.5,33.5)):
        add(f"seat_{index}_shell",[x,5.5,4],[x+13,6.25,12],"metal")
        add(f"seat_{index}_padded_surface",[x+.25,6.25,4.25],[x+12.75,7,11.75],"seat")
        back=add(f"seat_{index}_backrest",[x,8,11.75],[x+13,14.75,12.75],"seat")
        back["rotation"]={"origin":[x+6.5,7.75,12],"axis":"x","angle":22.5,"rescale":False}
    for index,x in enumerate((.5,15.5,31.5,46.5)):
        add(f"armrest_{index}_support",[x,5.5,9.75],[x+1,8.75,10.5],"metal")
        add(f"armrest_{index}_rail",[x,8.75,4.5],[x+1,9.375,11],"metal")
        add(f"armrest_{index}_dark_pad",[x,9.375,4.5],[x+1,9.625,10.5],"dark")
    return e


def split_part(element,index):
    """Crop X and outward UVs. X-axis tilts commute with this X clipping."""
    offset=index*16
    a,b=element["from"],element["to"]
    low,high=max(a[0],offset),min(b[0],offset+16)
    if low >= high: return None
    p=copy.deepcopy(element)
    if "rotation" in p:
        assert p["rotation"]["axis"] == "x"
        p["rotation"]["origin"][0]-=offset
    p["from"][0],p["to"][0]=low-offset,high-offset
    if low != a[0]: p["faces"].pop("west",None)
    if high != b[0]: p["faces"].pop("east",None)
    for face in ("north","south","up","down"):
        if face not in p["faces"]: continue
        uv=p["faces"][face]["uv"]
        if face == "north": f0,f1=(b[0]-high)/(b[0]-a[0]),(b[0]-low)/(b[0]-a[0])
        else: f0,f1=(low-a[0])/(b[0]-a[0]),(high-a[0])/(b[0]-a[0])
        p["faces"][face]["uv"]=[round(uv[0]+(uv[2]-uv[0])*f0,6),uv[1],round(uv[0]+(uv[2]-uv[0])*f1,6),uv[3]]
    return p if p["faces"] else None


def arrangement(model_id):
    elements={IDS[0]:reception_desk,IDS[1]:department_sign,IDS[2]:waiting_bench}[model_id]()
    collision=[]
    for e in elements:
        lo,hi=bounds(e)
        assert -1e-6 <= lo[0] < hi[0] <= (48 if model_id==IDS[2] else 32)+1e-6
        assert -1e-6 <= lo[1] < hi[1] <= (24 if model_id==IDS[0] else 16)+1e-6,e["name"]
        assert -1e-6 <= lo[2] < hi[2] <= 16+1e-6,e["name"]
        collision.append((lo,hi))
    omit_hidden_faces(elements)
    validate_model_elements(elements)
    return elements,collision


def used_textures(elements):
    keys={f["texture"][1:] for e in elements for f in e["faces"].values()} | {"particle"}
    return {k:v for k,v in MATERIALS.items() if k in keys}


def write_shapes(all_shapes):
    rows=[]
    for model_id,collision in all_shapes.items():
        rows.extend([f"            // {model_id}","            {"])
        for part in range(3 if model_id==IDS[2] else 2):
            pieces=[]
            for lo,hi in collision:
                low,high=max(lo[0],part*16),min(hi[0],part*16+16)
                if low < high: pieces.append(([low-part*16,*lo[1:]],[high-part*16,*hi[1:]]))
            rows.append("                rotations(Shapes.or(")
            for i,(lo,hi) in enumerate(pieces):
                coords=", ".join(f"{v:.6f}".rstrip("0").rstrip(".") for v in lo+hi)
                rows.append("                    Block.box("+coords+")"+("," if i < len(pieces)-1 else ").optimize()),"))
        rows.append("            },")
    source='''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_police_entrance.py; variant, part, facing. */
final class PoliceEntranceShapes {
    static final VoxelShape[][][] SHAPES = {
'''+"\n".join(rows)+'''
    };

    private PoliceEntranceShapes() {
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
    (ROOT / "src/main/java/com/craftdistrict/block/PoliceEntranceShapes.java").write_text(source,encoding="utf-8")


def main():
    textures(); shapes={}
    for model_id in IDS:
        elements,collision=arrangement(model_id); states={}; counts=[]
        parts=((0,"primary"),(1,"extension"),(2,"end")) if model_id==IDS[2] else ((0,"primary"),(1,"extension"))
        for part,label in parts:
            pieces=[p for e in elements if (p:=split_part(e,part)) is not None]
            validate_model_elements(pieces); counts.append(len(pieces))
            write_json(MODELS / f"{model_id}_{label}.json",{"parent":"minecraft:block/block","textures":used_textures(pieces),"elements":pieces})
            for face,angle in (("north",0),("east",90),("south",180),("west",270)):
                key=f"facing={face},primary={str(part == 0).lower()}"
                if model_id==IDS[2]: key=f"end={str(part == 2).lower()},"+key
                states[key]={"model":f"craftdistrict:block/police/{model_id}_{label}","y":angle}
                if model_id==IDS[2] and part==0:
                    states[f"end=true,facing={face},primary=true"]=copy.deepcopy(states[key])
        write_json(ASSETS / f"blockstates/{model_id}.json",{"variants":states})
        item=copy.deepcopy(elements)
        for e in item:
            center_offset=16 if model_id==IDS[2] else 8
            for key in ("from","to"): e[key][0]-=center_offset
            if "rotation" in e: e["rotation"]["origin"][0]-=center_offset
        validate_model_elements(item)
        assert all(-16 <= v <= 32 for e in item for key in ('from','to') for v in e[key])
        scale=.32 if model_id==IDS[2] else .43
        gui={"rotation":[20,135,0],"translation":[0,.75,0],"scale":[scale,scale,scale]} if model_id==IDS[2] else {
            "rotation":[25,135,0],"scale":[scale,scale,scale]}
        write_json(ASSETS / f"models/item/{model_id}.json",{"parent":"minecraft:block/block","textures":used_textures(item),"elements":item,"display":{
            "gui":gui,
            "ground":{"translation":[0,3,0],"scale":[.25,.25,.25]},
            "fixed":{"rotation":[0,180,0],"scale":[.4,.4,.4]},
            "thirdperson_righthand":{"rotation":[75,45,0],"scale":[.3,.3,.3]},
            "thirdperson_lefthand":{"rotation":[75,45,0],"scale":[.3,.3,.3]},
            "firstperson_righthand":{"rotation":[0,45,0],"scale":[.3,.3,.3]},
            "firstperson_lefthand":{"rotation":[0,225,0],"scale":[.3,.3,.3]}}})
        write_json(ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{model_id}.json",{
            "type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"craftdistrict:{model_id}"}],
            "conditions":[{"condition":"minecraft:survives_explosion"}]}]})
        shapes[model_id]=collision
        print(f"{model_id}: {len(elements)} cuboids; {len(parts)} parts; {len(states)} states.")
    write_shapes(shapes)
    print("Three existing IDs; seven block models; 28 cached part/facing shapes.")


def previews():
    """Use the established JSON renderer; previews are ignored build artifacts."""
    from preview_bakery_model import render, BACKGROUND
    directory=ROOT/'build/asset-previews'
    directory.mkdir(parents=True,exist_ok=True)
    try: font=ImageFont.truetype('arial.ttf',22)
    except OSError: font=ImageFont.load_default()
    family=Image.new('RGB',(1320,1500),BACKGROUND[:3])
    family_draw=ImageDraw.Draw(family)
    for row,model_id in enumerate(IDS):
        sheet=Image.new('RGB',(1440,1390),BACKGROUND[:3]); draw=ImageDraw.Draw(sheet)
        for n,view in enumerate(('front','side','rear','three-quarter')):
            panel=render(model_id,view,(700,625))
            panel.save(directory/f'{model_id}_{"isometric" if view=="three-quarter" else view}.png')
            x=(n%2)*720; y=(n//2)*695
            sheet.paste(panel,(x,y+55)); draw.text((x+25,y+15),view.upper(),font=font,fill=(55,62,76))
        sheet.save(directory/f'{model_id}_all_views.png')
        family_draw.text((20,row*500+10),model_id.replace('police_','').replace('_',' ').title(),font=font,fill=(55,62,76))
        for col,view in enumerate(('front','side','three-quarter')):
            family.paste(render(model_id,view,(440,425)),(col*440,row*500+70))
            family_draw.text((col*440+15,row*500+42),view.upper(),font=font,fill=(55,62,76))
    family.save(directory/'police_entrance_family.png')
    print(f'Front, side, rear and isometric previews: {directory}')


if __name__ == "__main__":
    main()
    if '--previews' in sys.argv: previews()
