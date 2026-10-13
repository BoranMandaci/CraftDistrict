"""Editable flour-sack family. Python standard library only; NORTH (-Z) is front.

Layout entries drive both named vanilla model elements and cached Java shapes.
Upright cloth bags and compressed horizontal pillows have separate profiles.
Diagonal pillows use a bevelled footprint so every element has one legal rotation.
Only these three decorations and FlourSackShapes.java are generated.
"""

import copy
import itertools
import math
import struct
import sys
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, write_json

FACES = ("north", "south", "west", "east", "up", "down")
# Face normal, increasing U, increasing V: Minecraft's outward UV convention.
BASES = {
    "north": ((0,0,-1),(-1,0,0),(0,-1,0)),
    "south": ((0,0,1),(1,0,0),(0,-1,0)),
    "west": ((-1,0,0),(0,0,1),(0,-1,0)),
    "east": ((1,0,0),(0,0,-1),(0,-1,0)),
    "up": ((0,1,0),(1,0,0),(0,0,1)),
    "down": ((0,-1,0),(1,0,0),(0,0,-1)),
}
TILES = ("cloth", "cloth_light", "gathers", "tie", "stamp")
MATERIALS = {name:f"craftdistrict:block/bakery/flour_sacks_{name}" for name in TILES}
MATERIALS["particle"] = MATERIALS["cloth"]

# Name, lower corner, actual (width,height,depth), profile, light cloth,
# wheat mark, horizontal turn. Dimensions include the cloth opening.
# Keep this order synchronized with FlourSacksBlock.Arrangement.
LAYOUTS = {
    "small": (
        ("full_upright", (1,0,5.5), (6.75,10,6.5), "upright", False, True, 0),
        ("short_upright", (8.25,0,3.5), (6.25,8.5,6), "upright", True, False, 0),
    ),
    "stacked": (
        ("front_ground", (2,0,1.25), (11.5,3.75,5.5), "horizontal", False, True, 0),
        ("rear_ground", (2.5,0,8.5), (11,3.6,5.5), "horizontal", True, False, 0),
        ("diagonal_top", (2.3,3.45,5.5), (11.4,3.5,5), "horizontal", False, False, 22.5),
    ),
    "pile": (
        ("front_lower", (1.5,0,.5), (12,3.3,4.3), "horizontal", False, True, 0),
        ("middle_lower", (2,0,5.85), (12,3.15,4.3), "horizontal", True, False, 0),
        ("rear_lower", (1.25,0,11.2), (12,3.3,4.3), "horizontal", False, False, 0),
        ("front_upper", (2.5,2.95,2.4), (10.4,3.55,3.8), "horizontal", True, False, 22.5),
        ("rear_upper", (2.9,3,9.8), (10.2,3.4,3.8), "horizontal", False, True, -22.5),
    ),
}


def png(name, sample):
    def chunk(kind, data):
        return struct.pack(">I",len(data))+kind+data+struct.pack(">I",zlib.crc32(kind+data))
    rows=b"".join(b"\0"+bytes(c for x in range(16) for c in sample(x,y)) for y in range(16))
    data=b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",16,16,8,6,0,0,0))
    data+=chunk(b"IDAT",zlib.compress(rows,9))+chunk(b"IEND",b"")
    path=ASSETS/f"textures/block/bakery/flour_sacks_{name}.png"
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(data)


def cloth(x,y,light=False):
    # Restrained, broken creases rather than a hard border around every face.
    shade = 0
    if (x,y) in {(2,12),(3,11),(3,10),(4,9),(12,3),(11,4),(11,5),(10,6),
                 (13,13),(12,12),(12,11),(2,3),(3,4)}:
        shade = -13
    elif (x,y) in {(4,11),(5,10),(12,5),(11,6),(11,12),(3,3)}:
        shade = 7
    elif x==1 and 5<=y<=11 and y%3!=0:
        shade = -9  # One faint stitched edge, not four framed edges.
    elif (x*7+y*11)%23==0:
        shade = -4
    elif (x+y)%5==0:
        shade = 2
    base = (232,220,192) if light else (219,202,170)
    return (*(c+shade for c in base),255)


def textures():
    def gathers(x,y):
        # Cloth converges toward a gathered mouth; no tiny fold cuboids needed.
        crease = {(3,13),(4,12),(5,10),(6,9),(7,7),(7,6),
                  (12,13),(11,11),(10,10),(9,8),(8,7),(8,5)}
        if (x,y) in crease:
            return (195,176,141,255)
        return cloth(x,y)
    def tie(x,y):
        if y in (7,8) and 3<=x<=12:
            return (156,128,90,255) if (x+y)%3 else (185,155,111,255)
        return gathers(x,y)
    def stamp(x,y):
        # Small upright wheat stencil, no letters or numbers to mirror.
        wheat={(8,y) for y in range(7,12)}|{(6,7),(7,8),(10,8),(9,9),(6,9),(7,10)}
        return (185,159,119,255) if (x,y) in wheat else cloth(x,y)
    png("cloth",cloth)
    png("cloth_light",lambda x,y:cloth(x,y,True))
    png("gathers",gathers)
    png("tie",tie)
    png("stamp",stamp)


def box(name,start,end,material,mark=False):
    result={"name":name,"from":start,"to":end,"faces":{
        f:{"texture":"#"+material,"uv":[0,0,16,16]} for f in FACES}}
    if mark:
        result["faces"]["north"]["texture"]="#stamp"
    return result


def rotated_box(name,center,size,material,axis,angle):
    part=box(name,[center[i]-size[i]/2 for i in range(3)],
             [center[i]+size[i]/2 for i in range(3)],material)
    part['rotation']={'axis':axis,'angle':angle,'origin':list(center),'rescale':False}
    return part


def rounded_prism(name,start,end,axis,material):
    """Six broad cuboids make an octagonal cross-section, with real bevel faces.

    Corner diamonds overlap the core internally; they never extend past the
    requested silhouette. This avoids both tiny staircase cubes and square bags.
    """
    along='xyz'.index(axis)
    a,b=[i for i in range(3) if i!=along]
    k=min(end[a]-start[a],end[b]-start[b])*.22
    pieces=[]
    for clipped in (a,b):
        lo,hi=list(start),list(end)
        lo[clipped]+=k; hi[clipped]-=k
        # End faces partially overlap in this union. Separate their planes by
        # an invisible amount to avoid coplanar faces fighting in Minecraft.
        inset=len(pieces)*.0002
        lo[along]+=inset; hi[along]-=inset
        pieces.append(box(f'{name}_core_{clipped}',lo,hi,material))
    for side_a,side_b in itertools.product((0,1),repeat=2):
        center=[(start[i]+end[i])/2 for i in range(3)]
        center[a]=end[a]-k if side_a else start[a]+k
        center[b]=end[b]-k if side_b else start[b]+k
        size=[k*math.sqrt(2)]*3
        size[along]=end[along]-start[along]-.0008
        pieces.append(rotated_box(f'{name}_soft_corner_{side_a}{side_b}',center,size,material,axis,45))
    return pieces


def cloth_pillow(name,size,material):
    """Three large cores and twelve broad edge bevels soften all six views.

    The pinched corners intentionally remain recessed like gathered fabric;
    there are no eight tiny corner filler cubes or extra surface details.
    """
    k=min(size)*.22
    pieces=[]
    for along,axis in enumerate('xyz'):
        lo,hi=[k]*3,[v-k for v in size]
        lo[along]=0; hi[along]=size[along]
        pieces.append(box(f'{name}_cloth_core_{axis}',lo,hi,material))
    for along,axis in enumerate('xyz'):
        a,b=[i for i in range(3) if i!=along]
        for side_a,side_b in itertools.product((0,1),repeat=2):
            center=[v/2 for v in size]
            center[a]=size[a]-k if side_a else k
            center[b]=size[b]-k if side_b else k
            dimensions=[k*math.sqrt(2)]*3
            dimensions[along]=size[along]-2*k
            pieces.append(rotated_box(f'{name}_soft_edge_{axis}{side_a}{side_b}',center,dimensions,material,axis,45))
    return pieces


def upright(name,size,light,mark):
    w,h,d=size; mat='cloth_light' if light else 'cloth'
    # One broad pillow volume, with genuinely sloping heels and shoulders.
    # Avoid separate necks, shoulder caps and protruding decorative appendages.
    body=cloth_pillow(name+'_filled_cloth',(w,h*.88,d),mat)
    opening=[
        rotated_box(name+'_gathered_cloth',[w*.51,h*.91,d*.49],[w*.38,h*.13,d*.18],'tie','z',-22.5),
        rotated_box(name+'_folded_cloth',[w*.67,h*.9,d*.47],[w*.21,h*.1,d*.15],'gathers','z',22.5),
    ]
    if mark:
        body[2]['faces']['north']['texture']='#stamp'
    # Three conservative height bands: the chamfer's widest point within each
    # band determines its width. The narrow end bands retain some corner space.
    k=min(w,h*.88,d)*.22
    groups=(
        [{'from':[k/2,0,0],'to':[w-k/2,k/2,d]}],
        [{'from':[0,k/2,0],'to':[w,h*.88-k/2,d]}],
        [{'from':[k/2,h*.88-k/2,0],'to':[w-k/2,h*.88,d]},*opening],
    )
    return body+opening,groups


def horizontal(name,size,light,mark,diagonal):
    w,h,d=size; mat='cloth_light' if light else 'cloth'
    if diagonal:
        # One low, broad pillow without a separate raised lid. Only Y-axis
        # bevels here: vanilla JSON cannot compose rotations about two axes.
        body=rounded_prism(name+'_flattened_belly',[0,h*.08,0],[w,h*.9,d],'y',mat)
        pads=[box(name+'_compressed_contact',[w*.09,0,d*.12],[w*.91,h*.22,d*.88],mat)]
    else:
        # Horizontal pillow authored along X, with bevelled Y/Z cross-section
        # and sloping, compressed ends. No upright neck has been rotated over.
        body=cloth_pillow(name+'_pillow',size,mat)
        pads=[]
    # Gathered sewn end lies against the pillow, without a protruding cap/neck.
    opening=[box(name+'_pressed_end_fold',[w*.87,h*.36,d*.36],[w*.985,h*.65,d*.65],'gathers'),
             box(name+'_turned_cloth_corner',[w*.84,h*.7,d*.4],[w*.94,h*.98,d*.57],mat)]
    if mark:
        body[0 if diagonal else 1]['faces']['up']['texture']='#stamp'
    if diagonal:
        groups=(body,pads,opening)
    else:
        k=min(size)*.22
        groups=(
            [{'from':[k/2,0,k/2],'to':[w-k/2,k/2,d-k/2]}],
            [{'from':[0,k/2,0],'to':[w,h-k/2,d]}],
            [{'from':[k/2,h-k/2,k/2],'to':[w-k/2,h,d-k/2]},*opening],
        )
    return body+pads+opening,groups


def cloth_uvs(elements,size):
    w,h,d=size
    for part in elements:
        x0,y0,z0=part['from']; x1,y1,z1=part['to']
        uv={
            'north':[16*(1-x1/w),16*(1-y1/h),16*(1-x0/w),16*(1-y0/h)],
            'south':[16*x0/w,16*(1-y1/h),16*x1/w,16*(1-y0/h)],
            'west':[16*z0/d,16*(1-y1/h),16*z1/d,16*(1-y0/h)],
            'east':[16*(1-z1/d),16*(1-y1/h),16*(1-z0/d),16*(1-y0/h)],
            'up':[16*x0/w,16*z0/d,16*x1/w,16*z1/d],
            'down':[16*x0/w,16*(1-z1/d),16*x1/w,16*(1-z0/d)],
        }
        for face in part['faces'].values():
            if face['texture'] in ('#tie','#gathers'):
                face['uv']=[0,0,16,16]
        for side,face in part['faces'].items():
            if face['texture'] not in ('#tie','#gathers'):
                face['uv']=[round(max(0,min(16,v)),6) for v in uv[side]]


def rotate_point(point,origin,axis,angle):
    x,y,z=[point[i]-origin[i] for i in range(3)]
    c,s=math.cos(math.radians(angle)),math.sin(math.radians(angle))
    p={'x':(x,c*y-s*z,s*y+c*z),'y':(c*x+s*z,y,-s*x+c*z),'z':(c*x-s*y,s*x+c*y,z)}[axis]
    return [p[i]+origin[i] for i in range(3)]


def bake_y_quarter_turn(element,origin,angle):
    """Normalize compound Y turns to legal JSON angles, preserving face UVs."""
    result=copy.deepcopy(element)
    corners=[rotate_point(p,origin,'y',angle) for p in itertools.product(*zip(element['from'],element['to']))]
    result['from']=[min(p[i] for p in corners) for i in range(3)]
    result['to']=[max(p[i] for p in corners) for i in range(3)]
    faces={}
    for old,face in element['faces'].items():
        normal,u,v=(tuple(round(n) for n in rotate_point(a,(0,0,0),'y',angle)) for a in BASES[old])
        new=next(name for name,basis in BASES.items() if basis[0]==normal)
        nu,nv=BASES[new][1:]; negative=lambda a:tuple(-n for n in a)
        orientations=((nu,nv),(nv,negative(nu)),(negative(nu),negative(nv)),(negative(nv),nu))
        rotation=orientations.index((u,v))*90
        faces[new]=copy.deepcopy(face)
        if rotation:
            faces[new]['rotation']=rotation
    result['faces']=faces
    return result


def place(element,position,size,yaw):
    result=copy.deepcopy(element)
    if yaw:
        pivot=[size[0]/2,size[1]/2,size[2]/2]
        r=result.get('rotation',{'axis':'y','angle':0,'origin':pivot})
        assert r['axis']=='y','Diagonal pillows must use only Y-axis local bevels.'
        # Relocate a local rotation to the common sack pivot. This moves the
        # unrotated cuboid center, but the final rendered geometry stays exact.
        center=[(result['from'][i]+result['to'][i])/2 for i in range(3)]
        rendered_center=rotate_point(center,r['origin'],'y',r['angle'])
        new_center=rotate_point(rendered_center,pivot,'y',-r['angle'])
        for key in ('from','to'):
            result[key]=[result[key][i]+new_center[i]-center[i] for i in range(3)]
        angle=r['angle']+yaw
        if angle>45:
            result=bake_y_quarter_turn(result,pivot,90); angle-=90
        elif angle < -45:
            result=bake_y_quarter_turn(result,pivot,-90); angle+=90
        result['rotation']={'axis':'y','angle':angle,'origin':pivot,'rescale':False}
    for key in ('from','to'):
        result[key]=[round(result[key][i]+position[i],6) for i in range(3)]
    if 'rotation' in result:
        result['rotation']['origin']=[round(result['rotation']['origin'][i]+position[i],6) for i in range(3)]
    return result


def bounds(element):
    points=list(itertools.product(*zip(element['from'],element['to'])))
    if 'rotation' in element:
        r=element['rotation']
        points=[rotate_point(p,r['origin'],r['axis'],r['angle']) for p in points]
    return ([min(p[i] for p in points) for i in range(3)], [max(p[i] for p in points) for i in range(3)])


def omit_hidden_faces(elements):
    for e in elements:
        for face,(normal,_,_) in BASES.items():
            if face not in e['faces']:
                continue
            axis=next(i for i,n in enumerate(normal) if n)
            sign=normal[axis]; plane=e['to' if sign>0 else 'from'][axis]
            for other in elements:
                if other is e or other.get('rotation')!=e.get('rotation'):
                    continue
                a,b=other['from'],other['to']
                crosses=a[axis]<=plane<b[axis] if sign>0 else a[axis]<plane<=b[axis]
                if crosses and all(a[i]<=e['from'][i] and b[i]>=e['to'][i] for i in range(3) if i!=axis):
                    del e['faces'][face]
                    break
    # Minecraft rejects an element with zero faces, even when it is completely
    # invisible. Collision envelopes were already derived from the full layout;
    # removing these render-only elements does not alter shape or placement data.
    elements[:] = [element for element in elements if element['faces']]


def validate_model_elements(elements):
    """Enforce the loader's face requirement before either model is exported."""
    for element in elements:
        faces=element.get('faces',{})
        if not 1<=len(faces)<=6 or not set(faces)<=set(FACES):
            raise ValueError(f"{element.get('name','unnamed')}: expected 1-6 unique Minecraft faces")


def arrangement(entries):
    elements,collision=[],[]
    for name,pos,size,profile,light,mark,yaw in entries:
        pieces,groups=(upright(name,size,light,mark) if profile=='upright'
                       else horizontal(name,size,light,mark,bool(yaw)))
        cloth_uvs(pieces,size)
        elements.extend(place(p,pos,size,yaw) for p in pieces)
        for group in groups:
            envelopes=[bounds(place(p,pos,size,yaw)) for p in group]
            collision.append(([min(a[i] for a,b in envelopes) for i in range(3)],
                              [max(b[i] for a,b in envelopes) for i in range(3)]))
    for e in elements:
        a,b=bounds(e)
        assert all(-1e-6<=a[i]<=b[i]<=16+1e-6 for i in range(3)),(e['name'],a,b)
    omit_hidden_faces(elements)
    validate_model_elements(elements)
    return elements,collision


def write_shapes(all_shapes):
    # Generated data are separate from the small hand-written behavior class.
    rows=[]
    for variant,boxes in all_shapes.items():
        rows.append(f"            // {variant}: three conservative bands per sack")
        rows.append("            rotations(Shapes.or(")
        for index,(a,b) in enumerate(boxes):
            coords=", ".join(str(round(v,6)) for v in a+b)
            rows.append("                    Block.box("+coords+")"+("," if index<len(boxes)-1 else ").optimize()),"))
    text='''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_flour_sacks.py; edit sack layouts there. */
final class FlourSackShapes {
    static final VoxelShape[][] SHAPES = {
'''+"\n".join(rows)+'''
    };

    private FlourSackShapes() {
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] shapes = {north, Shapes.empty(), Shapes.empty(), Shapes.empty()};
        for (int turn = 1; turn < 4; turn++) {
            for (AABB box : shapes[turn - 1].toAabbs()) {
                shapes[turn] = Shapes.or(shapes[turn], Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            shapes[turn] = shapes[turn].optimize();
        }
        return shapes;
    }
}
'''
    (ROOT/'src/main/java/com/craftdistrict/block/FlourSackShapes.java').write_text(text,encoding='utf-8')


def main():
    textures(); all_shapes={}
    for variant,entries in LAYOUTS.items():
        id=f"flour_sacks_{variant}"
        elements,collision=arrangement(entries)
        model={'parent':'minecraft:block/block','textures':MATERIALS,'elements':elements}
        write_json(MODELS/f'{id}.json',model)
        write_json(ASSETS/f'blockstates/{id}.json',{'variants':{
            f'facing={facing}':{'model':f'craftdistrict:block/bakery/{id}','y':angle}
            for facing,angle in (('north',0),('east',90),('south',180),('west',270))}})
        item=copy.deepcopy(model)
        item['display']={
            'gui':{'rotation':[30,135,0],'translation':[0,1,0],'scale':[.8,.8,.8]},
            'ground':{'translation':[0,3,0],'scale':[.5,.5,.5]},
            'fixed':{'rotation':[0,180,0],'scale':[.65,.65,.65]},
            'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.45,.45,.45]},
            'thirdperson_lefthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.45,.45,.45]},
            'firstperson_righthand':{'rotation':[0,45,0],'scale':[.6,.6,.6]},
            'firstperson_lefthand':{'rotation':[0,225,0],'scale':[.6,.6,.6]},
        }
        write_json(ASSETS/f'models/item/{id}.json',item)
        write_json(ROOT/f'src/main/resources/data/craftdistrict/loot_tables/blocks/{id}.json',{
            'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'craftdistrict:{id}'}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
        all_shapes[variant]=collision
        print(f'{id}: {len(entries)} sacks, {len(elements)} cuboids, {len(collision)} shape bands, four rotations.')
    write_shapes(all_shapes)
    print(f'{len(TILES)} shared opaque textures; generated matching cached Java shapes.')


if __name__=='__main__':
    main()
