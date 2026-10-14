"""Editable three-column dispatch console and tall evidence shelf; -Z is front.

Only this batch's resources and PoliceWorkroomShapes.java are written. Existing
Police palette textures, two-cell clipping and hidden-face helpers are reused.
"""

import copy
import sys

sys.dont_write_bytecode = True
from PIL import Image
from generate_police_entrance import ASSETS, ROOT, MATERIALS as ENTRANCE, box, split_part
from generate_flour_sacks import bounds, omit_hidden_faces, rounded_prism, validate_model_elements
from generate_industrial_dough_mixer import part as vertical_part
from generate_bakery_display_counter import write_json

IDS=("police_dispatch_console","police_evidence_storage_shelf")
TILES=("map","calls","comms","radio","cardboard","tape","sealed","label","bin")
MATERIALS={name:f"craftdistrict:block/police/workroom_{name}" for name in TILES}
MATERIALS.update({name:ENTRANCE[name] for name in ("navy","dark","metal","light","keyboard","folder","paper")})
MATERIALS["particle"]=MATERIALS["metal"]
MODELS=ASSETS/'models/block/police'


def textures():
    for name in TILES:
        image=Image.new('RGBA',(16,16))
        for y in range(16):
            for x in range(16):
                if name=='map':
                    color=(25,43,58)
                    if x in (4,10) or y in (4,10): color=(92,138,158)
                    elif x in (3,9) or y in (3,9): color=(49,78,96)
                    if (x,y) in ((7,6),(12,12),(2,8)): color=(106,189,161)
                elif name=='calls':
                    color=(25,43,58)
                    if y in (2,3) and 2<=x<=13: color=(70,119,153)
                    if y in (6,9,12) and 5<=x<=12: color=(140,180,195)
                    if x in (2,3) and y in (6,9,12): color=(97,166,142)
                elif name=='comms':
                    color=(25,43,58)
                    if y==3 and 2<=x<=12: color=(115,167,185)
                    if 3<=x<=12 and y==11-(x%4): color=(100,180,157)
                    if y==14 and x in (3,6,9,12): color=(58,106,143)
                elif name=='radio':
                    color=(45,54,64)
                    if 1<=x<=9 and 2<=y<=6: color=(100,160,149)
                    if x in (2,5,8) and y in (10,12): color=(170,185,194)
                    if x in (12,13) and 9<=y<=12: color=(88,97,105)
                elif name=='cardboard':
                    v=-10 if x in (1,14) else 3 if (x*7+y*3)%17==0 else 0
                    color=(162+v,124+v,82+v)
                    if y in (2,13): color=(139,104,69)
                elif name=='tape':
                    color=(157,119,78) if not 6<=x<=9 else (197,181,138)
                    if 6<=x<=9 and y%4==0: color=(183,166,125)
                elif name=='sealed':
                    color=(186,192,187)
                    if x in (2,13) or y in (2,13): color=(136,149,144)
                    if (x*7+y*3)%19==0: color=(172,181,173)
                elif name=='label':
                    color=(221,227,225)
                    if y in (2,3) and 2<=x<=13: color=(61,82,105)
                    if y in (7,10,13) and 3<=x<=12: color=(92,107,119)
                    if x in (2,3) and y in (7,8,9): color=(60,80,98)
                else:
                    color=(93,109,121)
                    if y in (2,13): color=(135,148,157)
                    if x in (2,13) and 4<=y<=11: color=(75,92,106)
                image.putpixel((x,y),(*color,255))
        path=ASSETS/f'textures/block/police/workroom_{name}.png'
        path.parent.mkdir(parents=True,exist_ok=True); image.save(path)


def dispatch():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    add('low_equipment_plinth',[1,0,2],[47,1,14],'dark')
    for side,x in (('left',1),('right',40)):
        add(side+'_equipment_pedestal',[x,1,3],[x+7,13.5,14],'navy')
        add(side+'_front_panel',[x+.25,1.5,2.875],[x+6.75,12.875,3],'dark')
        for row,y in enumerate((2.5,6.5,10.5)):
            add(f'{side}_module_{row}',[x+.5,y,2.75],[x+6.5,y+1,2.875],'metal')
            add(f'{side}_module_{row}_handle',[x+2,y+.25,2.5],[x+5,y+.625,2.75],'dark')
    add('rear_equipment_crossbeam',[8,3,12.75],[40,4,14],'metal')
    add('metal_work_surface',[0,13.5,1],[48,14.5,15],'light')
    add('front_work_surface_edge',[0,13.5,.875],[48,14,1],'metal')
    add('navy_front_apron',[.5,12.75,1.5],[47.5,13.5,2],'navy')
    props_start=len(e)
    add('rear_cable_trough',[1,10.5,12.75],[47,11.25,14],'dark')
    # Each monitor owns a column. Angled screens and supports remain inside it.
    for name,x0,x1,y0,y1,z,tile,yaw in (
        ('left_calls',2,14,12.25,19.25,9.5,'calls',-22.5),
        ('main_map',18,30,12.25,19.25,11.5,'map',0),
        ('right_comms',34,46,12.25,19.25,9.5,'comms',22.5)):
        cx=(x0+x1)/2
        foot=add(name+'_foot',[cx-1.75,10.5,z-.5],[cx+1.75,10.875,z+1.75],'dark')
        stand=add(name+'_stand',[cx-.5,10.875,z+.25],[cx+.5,y0+.5,z+1],'metal')
        housing=add(name+'_housing',[x0,y0,z],[x1,y1,z+1],'dark')
        screen=add(name+'_screen',[x0+.375,y0+.375,z-.0625],[x1-.375,y1-.375,z],'dark')
        screen['faces']['north']['texture']='#'+tile
        if yaw:
            for p in (foot,stand,housing,screen):
                p['rotation']={'axis':'y','angle':yaw,'origin':[cx,(y0+y1)/2,z+.5],'rescale':False}
    keyboard=add('operator_keyboard',[19,10.5,2.5],[29,10.875,6],'dark')
    keyboard['faces']['up']['texture']='#keyboard'
    # Operator stands at -Z; rotate only this upward face so the space bar
    # (the bottom texture row) is nearest that operator, unlike the rear desk.
    keyboard['faces']['up']['rotation']=180
    add('radio_body',[21,10.5,2.5],[26,12.25,6],'dark')
    panel=add('radio_front_controls',[21.25,10.875,2.375],[25.75,12,2.5],'dark')
    panel['faces']['north']['texture']='#radio'
    add('radio_tuning_knob',[25,10.9375,2.125],[25.625,11.5625,2.375],'metal')
    add('microphone_dock',[27,10.5,3],[28.5,11,4.5],'metal')
    add('microphone_neck',[27.5,11,3.5],[28,12.5,4],'dark')
    add('handheld_microphone',[27.125,12,3.25],[28.375,14,4.25],'dark')
    add('left_communication_speaker',[2,10.5,3.5],[4,13.5,5],'dark')
    grille=add('speaker_grille',[2.125,10.875,3.4375],[3.875,13.25,3.5],'metal')
    grille['faces']['north']['texture']='#radio'
    add('console_control_pad',[5,10.5,3],[9,10.875,5.75],'navy')
    for i,x in enumerate((5.25,6.25,7.25,8.25)):
        add(f'console_key_{i}',[x,10.875,4.0625],[x+.5,11.125,4.6875],'light' if i<3 else 'sealed')
    add('notepad_backing',[10.75,10.5,2.5],[15.25,10.625,6],'folder')
    paper=add('dispatch_notepad',[11,10.625,2.75],[15,10.75,5.75],'light')
    paper['faces']['up']['texture']='#paper'
    # Lift the entire electronic layout together; stands stay on the worktop.
    for p in e[props_start:]:
        dx=11 if p['name'].startswith(('radio_','microphone_','handheld_microphone')) else 0
        for key in ('from','to'):
            p[key][0]+=dx
            p[key][1]+=4
        if 'rotation' in p: p['rotation']['origin'][1]+=4
    monitors=[p for p in e if p['name'].endswith('_housing')]
    for left,right in zip(monitors,monitors[1:]):
        assert bounds(left)[1][0] < bounds(right)[0][0], 'Monitor housings must not intersect.'
    return e


def evidence():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    for x in (1,14):
        for z in (1.5,13.5):
            add(f'frame_{x}_{z}_foot',[x-.25,0,z-.25],[x+1.25,.75,z+1.25],'dark')
            add(f'frame_{x}_{z}_upright',[x,.75,z],[x+1,31.5,z+1],'metal')
    for z in (1.5,13.5):
        add(f'top_crossbar_{z}',[2,30.75,z],[14,31.5,z+1],'metal')
    for i,y in enumerate((2.5,9.5,17,24.5)):
        add(f'shelf_{i}',[1.5,y-.5,2],[14.5,y,14],'metal')
        add(f'shelf_{i}_front_lip',[1.5,y,2],[14.5,y+.375,2.5],'light')
    def label(name,x,y,z,width=2.5):
        add(name+'_evidence_tag',[x,y,z-.0625],[x+width,y+1.25,z],'label')
    def carton(name,x,y,z,w,h,d):
        add(name+'_body',[x,y,z],[x+w,y+h,z+d],'cardboard')
        lid=add(name+'_sealed_lid',[x-.0625,y+h,z-.0625],[x+w+.0625,y+h+.25,z+d+.0625],'cardboard')
        lid['faces']['up']['texture']='#tape'
        label(name,x+.5,y+h*.5,z,min(2.5,w-1))
    carton('large_lower_box',2.5,2.5,4,6.25,4.75,8)
    add('lower_sealed_bin',[9.5,2.5,5],[13.5,6.25,12],'bin')
    add('lower_bin_lid',[9.375,6.25,4.875],[13.625,6.625,12.125],'light')
    label('lower_bin',10,4,5,2.5)
    carton('middle_evidence_box',2.75,9.5,4,5.75,4.5,7)
    add('flat_sealed_package',[9.25,9.5,5],[13.5,11.25,12],'sealed')
    add('package_fold',[9.5,11.25,5.25],[13.25,11.5,11.75],'sealed')
    label('flat_package',9.75,9.75,5,2.5)
    add('upper_small_container',[2.5,17,5],[7.25,20.75,12],'bin')
    add('upper_container_lid',[2.375,20.75,4.875],[7.375,21.125,12.125],'light')
    label('upper_container',3,18.25,5,2.5)
    e.extend(rounded_prism('sealed_evidence_bag',[9.25,17,6],[13.5,20.75,11],'y','sealed'))
    add('bag_folded_seal',[9.75,20.75,7],[13,21.125,10],'sealed')
    label('sealed_bag',10,18.5,6,2.5)
    for i,(x,h) in enumerate(((2.5,3.5),(3.375,4.25),(4.25,3.875),(5.125,4.125))):
        add(f'top_document_file_{i}',[x,24.5,6],[x+.875,24.5+h,11],'folder' if i!=1 else 'navy')
        add(f'top_file_{i}_spine_mark',[x+.125,25.25,5.9375],[x+.75,26,6],'paper')
    carton('top_compact_box',8,24.5,5.5,5.5,3.25,6)
    return e


def materials(e):
    keys={f['texture'][1:] for p in e for f in p['faces'].values()} | {'particle'}
    return {k:v for k,v in MATERIALS.items() if k in keys}


def chunks(e,index,vertical):
    if vertical: return vertical_part(e,index)
    result=[]
    for p in e:
        if 'rotation' in p:
            lo,hi=bounds(p)
            if not index*16 <= lo[0] < hi[0] <= (index+1)*16: continue
            p=copy.deepcopy(p)
            for key in ('from','to'): p[key][0]-=index*16
            p['rotation']['origin'][0]-=index*16
        else:
            p=split_part(p,index)
        if p is not None and p['faces']: result.append(p)
    validate_model_elements(result)
    return result


def shapes_source(layouts):
    variants=[]
    for index,e in enumerate(layouts):
        axis=1 if index else 0; halves=[]
        for part in range(2 if index else 3):
            cells=[]
            for p in e:
                lo,hi=bounds(p); lo[axis]=max(lo[axis],part*16); hi[axis]=min(hi[axis],(part+1)*16)
                if lo[axis]>=hi[axis]: continue
                lo[axis]-=part*16; hi[axis]-=part*16
                cells.append('                Block.box('+', '.join(f'{v:.6f}'.rstrip('0').rstrip('.') for v in lo+hi)+')')
            halves.append('            rotations(Shapes.or(\n'+',\n'.join(cells)+').optimize())')
        variants.append('        {\n'+',\n'.join(halves)+'\n        }')
    source='''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_police_workroom.py; variant, part, facing. */
final class PoliceWorkroomShapes {
    static final VoxelShape[][][] SHAPES = {
'''+',\n'.join(variants)+'''
    };

    private PoliceWorkroomShapes() {
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
    (ROOT/'src/main/java/com/craftdistrict/block/PoliceWorkroomShapes.java').write_text(source,encoding='utf-8')


def main():
    textures(); layouts=[]
    for index,(id,create) in enumerate(zip(IDS,(dispatch,evidence))):
        e=create(); layouts.append(copy.deepcopy(e))
        for p in e:
            lo,hi=bounds(p); limits=(16,32,16) if index else (48,24,16)
            assert all(-1e-6 <= lo[i] < hi[i] <= limits[i]+1e-6 for i in range(3)),p['name']
        omit_hidden_faces(e); validate_model_elements(e); states={}
        for part,name in enumerate(('lower','upper') if index else ('primary','extension','end')):
            pieces=chunks(e,part,bool(index))
            for p in pieces:
                assert all(-16<=v<=32 for k in ('from','to') for v in p[k]),p['name']
                for face in p['faces'].values():
                    assert all(0<=v<=16 for v in face['uv'])
            write_json(MODELS/f'{id}_{name}.json',{'parent':'minecraft:block/block','textures':materials(pieces),'elements':pieces})
            for facing,angle in (('north',0),('east',90),('south',180),('west',270)):
                key=f'facing={facing},primary={str(part==0).lower()}'
                if not index: key=f'end={str(part==2).lower()},'+key
                states[key]={'model':f'craftdistrict:block/police/{id}_{name}','y':angle}
                if not index and part==0:
                    states[f'end=true,facing={facing},primary=true']=copy.deepcopy(states[key])
        write_json(ASSETS/f'blockstates/{id}.json',{'variants':states})
        item=copy.deepcopy(e)
        for p in item:
            axis=1 if index else 0
            offset=8 if index else 16
            for k in ('from','to'): p[k][axis]-=offset
            if 'rotation' in p: p['rotation']['origin'][axis]-=offset
            assert all(-16<=v<=32 for key in ('from','to') for v in p[key]),p['name']
        gui_scale=.43 if index else .3
        write_json(ASSETS/f'models/item/{id}.json',{'parent':'minecraft:block/block','textures':materials(item),'elements':item,'display':{
            'gui':{'rotation':[25,135,0],'scale':[gui_scale,gui_scale,gui_scale]},
            'ground':{'translation':[0,3,0],'scale':[.25,.25,.25]},
            'fixed':{'rotation':[0,180,0],'scale':[.4,.4,.4]},
            'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.3,.3,.3]},
            'thirdperson_lefthand':{'rotation':[75,45,0],'scale':[.3,.3,.3]},
            'firstperson_righthand':{'rotation':[0,45,0],'scale':[.3,.3,.3]},
            'firstperson_lefthand':{'rotation':[0,225,0],'scale':[.3,.3,.3]}}})
        write_json(ROOT/f'src/main/resources/data/craftdistrict/loot_tables/blocks/{id}.json',{
            'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'craftdistrict:{id}'}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
        for texture in materials(e).values():
            assert (ASSETS/f'textures/{texture.split(":")[1]}.png').is_file(),texture
        print(f'{id}: {len(e)} static cuboids, {2 if index else 3} linked parts, four orientations.')
    shapes_source(layouts)


if __name__=='__main__': main()
