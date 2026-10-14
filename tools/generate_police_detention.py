"""Detention/booking assets only; named geometry also generates cached shapes."""
import copy
import math
import sys

sys.dont_write_bytecode=True
from PIL import Image, ImageDraw
from generate_police_entrance import ASSETS, ROOT, MATERIALS as PALETTE, box
from generate_police_package2 import crop
from generate_flour_sacks import bounds, rounded_prism, omit_hidden_faces, validate_model_elements
from generate_bakery_display_counter import write_json

IDS=('police_holding_cell_bed','police_holding_cell_toilet_sink','police_one_way_glass',
     'police_mugshot_height_chart','booking_camera')
REGIONS=(({0:(0,16)},{0:(16,32)}),({},),({},),({1:(0,16)},{1:(16,32)}),({1:(0,16)},{1:(16,32)}))
MATERIALS={name:PALETTE[name] for name in ('navy','metal','light','dark','folder','seat')}
MATERIALS.update({name:f'craftdistrict:block/police/detention_{name}'
                  for name in ('mattress','mirror_front','mirror_rear','height_chart')})
MATERIALS.update(lens='craftdistrict:block/police/package2_lens',
                 display='craftdistrict:block/police/package2_booking',particle=PALETTE['metal'])
GLYPHS={'0':('111','101','101','101','111'),'1':('010','110','010','010','111'),
        '2':('111','001','111','100','111'),'5':('111','100','111','001','111'),
        '.':('0','0','0','0','1')}


def textures():
    for name in ('mattress','mirror_front','mirror_rear','height_chart'):
        if name=='height_chart' and (ASSETS/'textures/block/police/detention_height_chart.png').is_file():
            continue  # Approved chart pixels remain untouched.
        size=(32,64) if name=='height_chart' else (16,16)
        im=Image.new('RGBA',size)
        for y in range(size[1]):
            for x in range(size[0]):
                if name=='mattress':
                    s=-5 if (x*3+y*7)%23==0 else 2 if (x+y)%7==0 else 0
                    rgba=(102+s,119+s,134+s,255)
                elif name.startswith('mirror'):
                    shine=round(3*math.cos((x+y)*math.tau/16))
                    # Opaque front + one-sided winding/back-face culling, not a darker translucent tint.
                    base=(39,48,58,255) if name=='mirror_front' else (73,94,109,55)
                    rgba=tuple(c+shine for c in base[:3])+(base[3],)
                else: rgba=(220,225,227,255)
                im.putpixel((x,y),rgba)
        if name=='height_chart':
            draw=ImageDraw.Draw(im)
            draw.line((2,0,2,63),fill=(58,68,78,255))
            for y in range(0,64,4):
                draw.line((2,y,20 if y%16==0 else 15 if y%8==0 else 8,y),fill=(49,60,70,255))
            for label,y in (('2.0',1),('1.5',17),('1.0',33),('0.5',49)):
                x=21
                for ch in label:
                    rows=GLYPHS[ch]
                    for dy,row in enumerate(rows):
                        for dx,pixel in enumerate(row):
                            if pixel=='1': im.putpixel((x+dx,y+dy),(39,49,59,255))
                    x+=len(rows[0])+1
        path=ASSETS/f'textures/block/police/detention_{name}.png'
        path.parent.mkdir(parents=True,exist_ok=True); im.save(path)


def add_to(e):
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    return add


def bed():
    e=[]; add=add_to(e)
    for z in (1.5,13.5):
        add(f'long_steel_rail_{z}',[.75,2.875,z],[31.25,3.75,z+1],'metal')
    for x in (.75,30.25):
        add(f'end_rail_{x}',[x,2.875,2.5],[x+1,3.75,13.5],'dark')
    for x in (2,28.75):
        for z in (2,12.75):
            add(f'sturdy_leg_{x}_{z}',[x,0,z],[x+1.25,2.875,z+1.25],'dark')
    add('under_mattress_brace',[2,2.375,7.5],[30,3,8.5],'metal')
    add('thin_institutional_mattress',[1.25,3.75,2.25],[30.75,4.875,13.75],'mattress')
    add('slightly_inset_mattress_top',[1.625,4.875,2.625],[30.375,5.125,13.375],'mattress')
    add('flat_pillow_base',[2,5.125,4.25],[7,5.4375,11.75],'light')
    add('flat_pillow_top',[2.375,5.4375,4.625],[6.625,5.6875,11.375],'light')
    for z in (2,13):
        add(f'low_head_stop_post_{z}',[.75,3.75,z],[1.5,6,z+1],'metal')
    add('low_head_stop',[.75,5.5,3],[1.5,6.125,13],'metal')
    for p in e:
        if p['name'].startswith('sturdy_leg_'):
            p['to'][1]+=2.875
        else:
            p['from'][1]+=2.875; p['to'][1]+=2.875
    return e


def toilet_sink():
    e=[]; add=add_to(e)
    add('integrated_steel_housing',[3,0,10.5],[13,11.75,15.5],'metal')
    add('front_service_panel',[3.5,6.375,10.375],[12.5,8.875,10.5],'folder')
    add('service_seam',[4,6.75,10.3125],[12,6.9375,10.375],'dark')
    # Disjoint depth bands build a closed tapered body. No rotated corner slabs
    # overlap the rim's top plane, which caused the former flickering.
    for name,x0,x1,z0,z1 in (
        ('toe',5.5,10.5,3,4),('base',4.5,11.5,4,9),('rear',5,11,9,10.5)):
        add('solid_lower_body_'+name,[x0,0,z0],[x1,2,z1],'metal')
    for name,x0,x1,z0,z1 in (
        ('nose',5,11,1.5,2.5),('front_shoulder',4,12,2.5,3.5),
        ('middle',3,13,3.5,8.5),('rear_shoulder',4,12,8.5,9.5),('rear',5,11,9.5,10.5)):
        add('solid_upper_body_'+name,[x0,2,z0],[x1,4.75,z1],'metal')
    for name,x0,x1,z0,z1 in (
        ('nose',5,11,1.5,2.5),('front_left',4,6,2.5,3.5),('front_right',10,12,2.5,3.5),
        ('left',3,4.25,3.5,8.5),('right',11.75,13,3.5,8.5),
        ('rear_left',4,5.75,8.5,9.5),('rear_right',10.25,12,8.5,9.5),('rear',5,11,9.5,10.5)):
        add('continuous_bowl_rim_'+name,[x0,4.75,z0],[x1,6.25,z1],'metal')
    for name,x0,x1,z0,z1 in (
        ('front',6,10,2.5,3.5),('middle',4.25,11.75,3.5,8.5),('rear',5.75,10.25,8.5,9.5)):
        add('recessed_dry_basin_'+name,[x0,4.75,z0],[x1,4.9375,z1],'dark')
    add('recessed_sink_floor',[4.25,11.75,11.25],[11.75,12.25,14.5],'dark')
    for name,a,b in (
        ('left',[3.5,11.75,10.5],[4.25,13,15.25]),('right',[11.75,11.75,10.5],[12.5,13,15.25]),
        ('front',[4.25,11.75,10.5],[11.75,13,11.25]),('rear',[4.25,11.75,14.5],[11.75,13,15.25])):
        add('sink_rim_'+name,a,b,'metal')
    add('compact_faucet_neck',[7.5,13,14.375],[8.5,15.375,15.25],'metal')
    add('faucet_spout',[7.5,14.875,12.5],[8.5,15.625,14.375],'metal')
    add('static_faucet_control',[10,13,14.625],[11,13.5,15.125],'dark')
    add('flush_control_plate',[11,9.25,10.3125],[12,10.25,10.5],'dark')
    return e


def glass():
    p=box('centered_asymmetric_observation_panel',[0,0,7.75],[16,16,8.25],'mirror_front')
    p['faces']['south']['texture']='#mirror_rear'
    for face in ('up','down','east','west'): p['faces'][face]['cullface']=face
    return [p]


def chart():
    e=[]; add=add_to(e)
    add('thin_measurement_backing',[0,0,15.5],[16,32,16],'metal')
    p=add('measurement_surface',[.375,.375,15.375],[15.625,31.625,15.5],'light')
    p['faces']['north']['texture']='#height_chart'
    return e


def camera():
    e=[]; add=add_to(e)
    for angle in (-22.5,22.5):
        p=add(f'tripod_side_leg_{angle}',[7.625,-.75,7.625],[8.375,14,8.375],'metal')
        p['rotation']={'axis':'z','angle':angle,'origin':[8,14,8],'rescale':False}
    p=add('tripod_rear_leg',[7.625,-.75,7.625],[8.375,14,8.375],'metal')
    p['rotation']={'axis':'x','angle':-22.5,'origin':[8,14,8],'rescale':False}
    for name,a,b in (
        ('left',[1.75,0,7.25],[3,.625,8.75]),('right',[13,0,7.25],[14.25,.625,8.75]),
        ('rear',[7.25,0,13],[8.75,.625,14.25])):
        add('rubber_foot_'+name,a,b,'dark')
    add('tripod_spreader_hub',[7,13.25,7],[9,14.75,9],'dark')
    add('adjustable_stand_column',[7.375,14.75,7.375],[8.625,24.5,8.625],'metal')
    props_start=len(e)
    add('camera_mounting_plate',[6.25,9.5,6.5],[9.75,10,9.5],'dark')
    add('photographic_camera_body',[3.5,10,6],[12.5,14,9.75],'dark')
    add('camera_hand_grip',[11.5,9.75,5.625],[13.25,14.25,10],'folder')
    add('raised_viewfinder_hump',[6.25,14,6.5],[9.75,15.25,9],'dark')
    add('small_viewfinder_window',[7,14.3125,9],[9,14.9375,9.125],'lens')
    e.extend(rounded_prism('large_photographic_lens',[5.25,9.625,3.375],[10.75,14.125,6.25],'z','metal'))
    lens=add('front_optical_glass',[5.875,10,3.25],[10.125,13.75,3.375],'dark')
    lens['faces']['north']['texture']='#lens'
    screen=add('rear_booking_display',[4.25,10.625,9.75],[9.75,13.25,9.875],'dark')
    screen['faces']['south']['texture']='#display'
    add('shutter_button',[11.875,14.25,6.875],[12.625,14.5625,7.625],'metal')
    add('rear_adjustment_button',[10.5,11.75,9.75],[11.25,12.5,10],'metal')
    for p in e[props_start:]:
        for key in ('from','to'): p[key][1]+=15
        if 'rotation' in p: p['rotation']['origin'][1]+=15
    return e


def materials(e):
    used={f['texture'][1:] for p in e for f in p['faces'].values()}|{'particle'}
    return {key:value for key,value in MATERIALS.items() if key in used}


def write_shapes(layouts):
    decorations=[]
    for index,e in enumerate(layouts):
        parts=[]
        for region in REGIONS[index]:
            cells=[]
            for p in e:
                lo,hi=bounds(p)
                if any(hi[a]<=low or lo[a]>=high for a,(low,high) in region.items()): continue
                for a,(low,high) in region.items(): lo[a],hi[a]=max(lo[a],low)-low,min(hi[a],high)-low
                cells.append('                Block.box('+', '.join(f'{v:.6f}'.rstrip('0').rstrip('.') for v in lo+hi)+')')
            parts.append('            rotations(Shapes.or(\n'+',\n'.join(cells)+').optimize())')
        decorations.append('        {\n'+',\n'.join(parts)+'\n        }')
    source='''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_police_detention.py; decoration, part, facing. */
final class PoliceDetentionShapes {
    private static final VoxelShape[][][] SHAPES = {
'''+',\n'.join(decorations)+'''
    };

    private PoliceDetentionShapes() { }

    static VoxelShape get(int decoration, int part, BlockState state) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return SHAPES[decoration][part][turn];
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] result = new VoxelShape[4];
        result[0] = north;
        for (int turn = 1; turn < 4; turn++) {
            VoxelShape rotated = Shapes.empty();
            for (AABB box : result[turn - 1].toAabbs()) {
                rotated = Shapes.or(rotated, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            result[turn] = rotated.optimize();
        }
        return result;
    }
}
'''
    (ROOT/'src/main/java/com/craftdistrict/block/PoliceDetentionShapes.java').write_text(source,encoding='utf-8')


def main():
    textures(); layouts=[]
    for index,(id,create) in enumerate(zip(IDS,(bed,toilet_sink,glass,chart,camera))):
        e=create(); layouts.append(copy.deepcopy(e)); states={}
        if index==3:
            continue  # Approved height-chart resources are not regenerated.
        limits=(32,16,16) if index==0 else (16,32,16) if index in (3,4) else (16,16,16)
        for p in e:
            lo,hi=bounds(p)
            assert all(-1e-6<=lo[a]<hi[a]<=limits[a]+1e-6 for a in range(3)),p['name']
        omit_hidden_faces(e); validate_model_elements(e)
        for part,region in enumerate(REGIONS[index]):
            pieces=[p for source in e if (p:=crop(source,region)) is not None]
            validate_model_elements(pieces)
            assert all(-16<=v<=32 for p in pieces for k in ('from','to') for v in p[k])
            assert all(0<=v<=16 for p in pieces for f in p['faces'].values() for v in f['uv'])
            name=f'{id}_{part}' if index in (0,3,4) else id
            write_json(ASSETS/f'models/block/police/{name}.json',{
                'parent':'minecraft:block/block','textures':materials(pieces),'elements':pieces})
            for facing,angle in (('north',0),('east',90),('south',180),('west',270)):
                key=f'facing={facing}'
                if index in (0,3,4): key+=f',primary={str(part==0).lower()}'
                states[key]={'model':f'craftdistrict:block/police/{name}','y':angle}
        write_json(ASSETS/f'blockstates/{id}.json',{'variants':states})
        item=copy.deepcopy(e)
        if index in (0,3,4):
            axis=0 if index==0 else 1
            for p in item:
                for k in ('from','to'): p[k][axis]-=8
                if 'rotation' in p: p['rotation']['origin'][axis]-=8
        assert all(-16<=v<=32 for p in item for k in ('from','to') for v in p[k])
        scale=.55 if index in (0,3,4) else .75
        write_json(ASSETS/f'models/item/{id}.json',{
            'parent':'minecraft:block/block','textures':materials(item),'elements':item,'display':{
            'gui':{'rotation':[25,135,0],'scale':[scale]*3},
            'ground':{'translation':[0,3,0],'scale':[.25]*3},
            'fixed':{'rotation':[0,180,0],'scale':[.4]*3},
            'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.3]*3},
            'thirdperson_lefthand':{'rotation':[75,45,0],'scale':[.3]*3},
            'firstperson_righthand':{'rotation':[0,45,0],'scale':[.4]*3},
            'firstperson_lefthand':{'rotation':[0,225,0],'scale':[.4]*3}}})
        write_json(ROOT/f'src/main/resources/data/craftdistrict/loot_tables/blocks/{id}.json',{
            'type':'minecraft:block','pools':[{'rolls':1,
            'entries':[{'type':'minecraft:item','name':f'craftdistrict:{id}'}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
        for path in materials(e).values(): assert (ASSETS/f'textures/{path.split(":")[1]}.png').is_file(),path
        print(f'{id}: {len(e)} cuboids, {len(REGIONS[index])} parts, four facings.')
    write_shapes(layouts)
    (ASSETS/'models/block/police/booking_camera.json').unlink(missing_ok=True)


if __name__=='__main__': main()
