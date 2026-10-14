"""Four static armory props only; named parts generate models and cached shapes."""
import copy
import sys

sys.dont_write_bytecode=True
from PIL import Image
from generate_police_entrance import ASSETS, ROOT, MATERIALS as PALETTE, box
from generate_police_package2 import crop
from generate_flour_sacks import bounds, rounded_prism, omit_hidden_faces, validate_model_elements
from generate_bakery_display_counter import write_json

IDS=('police_radio_charging_station','police_equipment_rack','police_pistol_rack','police_rifle_rack')
REGIONS=({},),({1:(0,16)},{1:(16,32)}),({},),({1:(0,16)},{1:(16,32)})
MATERIALS={name:PALETTE[name] for name in ('navy','metal','light','dark','folder','seat')}
MATERIALS.update({name:f'craftdistrict:block/police/armory_{name}' for name in ('radio_face','armor','panel')})
MATERIALS.update(status='craftdistrict:block/police/package2_status',particle=PALETTE['metal'])
MATERIALS['optic_glass']='craftdistrict:block/police/package2_lens'


def textures():
    for name in ('radio_face','armor','panel'):
        if (ASSETS/f'textures/block/police/armory_{name}.png').is_file():
            continue  # Preserve the existing palette/pixel assets in this geometry pass.
        im=Image.new('RGBA',(16,16))
        for y in range(16):
            for x in range(16):
                if name=='radio_face':
                    color=(32,39,47)
                    if 3<=x<=12 and 2<=y<=6: color=(58,110,112)
                    if y==4 and 5<=x<=10: color=(143,185,174)
                    if y in (9,11,13) and 3<=x<=12: color=(108,119,126)
                elif name=='armor':
                    s=-4 if (x+y)%3==0 else 1
                    color=(49+s,65+s,83+s)
                    if x in (1,14) or y in (1,14): color=(78,90,102)
                    if y in (5,10) and 3<=x<=12: color=(36,48,63)
                else:
                    color=(72,85,97)
                    if x%4 in (1,2) and y%4 in (1,2): color=(35,43,52)
                im.putpixel((x,y),(*color,255))
        path=ASSETS/f'textures/block/police/armory_{name}.png'
        path.parent.mkdir(parents=True,exist_ok=True); im.save(path)


def add_to(e):
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    return add


def radios():
    e=[]; add=add_to(e)
    add('compact_charging_dock',[1.5,0,5.5],[14.5,1.25,9.75],'dark')
    add('front_dock_trim',[1.75,.5,5.375],[14.25,.875,5.5],'metal')
    for i,x in enumerate((4,8,12)):
        height=6.875 if i==1 else 6.625
        add(f'charging_slot_{i}',[x-1.25,1.25,6.125],[x+1.25,1.75,8.875],'metal')
        add(f'dark_cradle_inset_{i}',[x-1.0625,1.75,6.375],[x+1.0625,2,8.625],'dark')
        add(f'handheld_radio_{i}',[x-.875,2,6.75],[x+.875,height,8],'dark')
        face=add(f'radio_screen_and_speaker_{i}',[x-.6875,2.375,6.6875],[x+.6875,height-.375,6.75],'dark')
        face['faces']['north']['texture']='#radio_face'
        add(f'antenna_{i}',[x-.6875,height,7.5],[x-.375,height+2.125,7.8125],'dark')
        add(f'top_rotary_knob_{i}',[x+.1875,height,7.0625],[x+.75,height+.4375,7.625],'folder')
        add(f'side_push_button_{i}',[x+.875,4,7],[x+1.125,5,7.5],'folder')
        add(f'charging_indicator_{i}',[x-.375,1.25,5.5625],[x+.375,1.5,6],'status')
    add('rear_power_socket',[11.5,.375,9.75],[13,.875,10.125],'metal')
    return e


def rack_frame():
    e=[]; add=add_to(e)
    add('stable_rack_base',[.5,0,4],[15.5,1,13.5],'dark')
    for x in (1,14):
        add(f'full_height_rear_post_{x}',[x,1,11.75],[x+1,31.75,12.75],'metal')
        add(f'front_support_foot_{x}',[x,0,4],[x+1,2.75,5.5],'metal')
    add('top_frame',[1,30.75,11.5],[15,31.75,12.75],'dark')
    add('lower_storage_ledge',[2,3.5,4.75],[14,4.125,11.75],'metal')
    return e


def equipment():
    e=rack_frame(); add=add_to(e)
    add('thin_equipment_pegboard',[2,4.125,11.25],[14,27.25,11.75],'panel')
    add('vest_hanger_bar',[2,26.5,8.5],[14,27.25,9.5],'metal')
    for x in (1.375,13.375):
        add(f'vest_hanger_return_arm_{x}',[x,26.5,9.5],[x+1.25,27.25,12.75],'metal')
    add('vest_backing',[4,17,8],[12,25,9.75],'navy')
    add('tapered_vest_lower',[4.75,12.25,8],[11.25,17,9.75],'navy')
    for x in (4,10):
        add(f'vest_shoulder_strap_{x}',[x,24,7.75],[x+2,26.75,9.5],'armor')
    add('front_armor_plate',[4.75,16.75,7.25],[11.25,23.75,8],'armor')
    add('lower_armor_plate',[5,13.25,7.25],[11,16.75,8],'armor')
    for x in (5,8.75):
        add(f'utility_pouch_{x}',[x,13.75,6.75],[x+2.25,16,7.25],'dark')
    add('helmet_shelf',[2,28,5],[14,28.625,12],'metal')
    for x in (1.5,13.25):
        add(f'helmet_shelf_support_{x}',[x,27.25,8.5],[x+1.25,28,12.25],'metal')
    e.extend(rounded_prism('protective_helmet_shell',[5,28.625,6],[11,31.5,11.5],'y','folder'))
    add('helmet_crown',[5.75,31.5,6.75],[10.25,31.875,10.75],'folder')
    add('helmet_lower_rim',[4.75,28.625,5.75],[11.25,29.125,11.75],'dark')
    add('helmet_front_visor',[5.25,29.3125,5.875],[10.75,30.375,6],'dark')
    add('stored_baton',[2.75,4.125,6.875],[3.5,21,7.625],'dark')
    add('baton_grip',[2.625,17.5,6.75],[3.625,20.25,7.75],'folder')
    add('baton_side_handle',[2.75,16.5,6.875],[4.5,17.25,7.625],'dark')
    add('baton_mount_arm',[2.625,18.5,7.75],[3.625,19.25,11.25],'metal')
    for x in (2.25,3.625):
        add(f'baton_grip_clip_{x}',[x,18.5,6.75],[x+.375,19.25,7.875],'metal')
    add('flashlight_body',[12.75,10.5,6.875],[13.75,17,7.875],'dark')
    add('flashlight_head',[12.5,9,6.625],[14,10.5,8.125],'metal')
    add('flashlight_lens',[12.6875,8.9375,6.8125],[13.8125,9,7.9375],'light')
    add('flashlight_mount_arm',[12.75,14,7.875],[13.75,14.75,11.25],'metal')
    for x in (12.375,13.75):
        add(f'flashlight_clip_{x}',[x,14,6.625],[x+.375,14.75,8],'metal')
    # Two complete pairs hang behind the vest's front plane, below its hem.
    for pair,x in enumerate((4.75,8.75)):
        for ring,(rx,y) in enumerate(((x,8),(x+(.375 if pair==0 else -.375),4.5))):
            for segment,(x0,x1,y0,y1) in enumerate((
                (.625,1.875,0,.375),(.25,.875,.375,.75),(1.625,2.25,.375,.75),
                (0,.5,.75,2.25),(2,2.5,.75,2.25),(.25,.875,2.25,2.625),
                (1.625,2.25,2.25,2.625),(.625,1.875,2.625,3))):
                add(f'cuff_pair_{pair}_ring_{ring}_{segment}',
                    [rx+x0,y+y0,9.5],[rx+x1,y+y1,10.125],'metal')
        add(f'cuff_pair_{pair}_link',[x+1,7.3125,9.625],[x+1.5,8.1875,10],'metal')
        add(f'cuff_hook_arm_{pair}',[x+1,11.375,10.125],[x+1.5,11.875,11.25],'metal')
        add(f'cuff_hook_drop_{pair}',[x+1,10.5,10.125],[x+1.5,11.875,10.5],'metal')
        add(f'cuff_hook_tip_{pair}',[x+1,10.5,9.875],[x+1.5,11,10.125],'metal')
    return e


def pistols():
    e=[]; add=add_to(e)
    add('perforated_secure_backboard',[1,1,14.75],[15,15,16],'panel')
    for x in (.75,14.5):
        add(f'mounting_frame_{x}',[x,.75,14.25],[x+.75,15.25,16],'metal')
    for y in (.75,14.5):
        add(f'frame_crossbar_{y}',[1.5,y,14.25],[14.5,y+.75,16],'dark')
    for i,y in enumerate((1.625,8)):
        add(f'large_pistol_slide_{i}',[4,y+4.125,12],[12.5,y+5.25,13.375],'dark')
        add(f'pistol_slide_edge_{i}',[4.25,y+5.25,12.125],[12.25,y+5.5,13.25],'metal')
        add(f'rear_iron_sight_{i}',[4.5,y+5.5,12.375],[5.25,y+5.9375,13.125],'dark')
        add(f'front_iron_sight_{i}',[11.5,y+5.5,12.5],[12.125,y+5.875,13],'dark')
        add(f'short_muzzle_{i}',[12.5,y+4.125,12.125],[13,y+5,13.25],'dark')
        add(f'pistol_lower_frame_{i}',[5,y+3.125,12.25],[10.5,y+4.125,13.25],'folder')
        grip=add(f'prominent_sloping_grip_{i}',[4.25,y+.375,12.125],[6.25,y+4.625,13.375],'dark')
        grip['rotation']={'axis':'z','angle':-22.5,'origin':[5.25,y+4.25,12.75],'rescale':False}
        for suffix,a,b in (
            ('lower',[6.875,y+2,12.375],[9.375,y+2.5,13.125]),
            ('front',[8.875,y+2.5,12.375],[9.375,y+3.125,13.125]),
            ('rear',[6.875,y+2.5,12.375],[7.375,y+3.125,13.125])):
            add(f'trigger_guard_{i}_{suffix}',a,b,'dark')
        add(f'rear_slide_mount_{i}',[4.25,y+4,13.375],[5.25,y+4.875,14.75],'metal')
        add(f'barrel_support_{i}',[11,y+3.75,13.25],[12,y+4.5,14.75],'metal')
        add(f'grip_lower_rest_{i}',[2.875,y+.5,13.125],[5.25,y+1,14.75],'metal')
    return e


def rifles():
    e=rack_frame(); add=add_to(e)
    for y in (8,23.5):
        add(f'secure_back_rail_{y}',[2,y,8.875],[14,y+.75,11.75],'metal')
    weapon_start=len(e)
    # One enlarged, coherent side profile. Grip and magazine both project from
    # the same underside of the receiver, rather than opposite sides of a rod.
    add('broad_stock_butt',[7.5,4.125,6.5],[12.25,6,8.25],'dark')
    add('tapered_stock_body',[8.25,6,6.625],[12,8.5,8.125],'dark')
    add('narrow_stock_neck',[9.375,8.5,6.875],[10.875,11.5,8.125],'folder')
    add('wide_receiver',[9.25,11.5,6.375],[12.25,16.75,8.125],'dark')
    grip=add('large_angled_pistol_grip',[6.25,9.375,6.625],[9.875,12.25,8],'dark')
    grip['rotation']={'axis':'z','angle':22.5,'origin':[9.25,12,7.3125],'rescale':False}
    add('magazine_upper_mass',[5.75,14,6.5],[9.25,16,8],'folder')
    add('magazine_lower_step',[5,12.75,6.5],[8.5,14,8],'folder')
    add('long_handguard',[10,16.75,6.625],[12,23,8.125],'folder')
    add('long_barrel',[10.5,23,6.875],[11.5,30,7.875],'dark')
    add('restrained_muzzle',[10.375,30,6.75],[11.625,30.5,8],'metal')
    add('receiver_side_edge',[9.5,15.25,6.3125],[12,15.5,6.375],'metal')
    # The receiver's top is +X for this upright side profile; the optic runs
    # parallel to the barrel, leaving the grip/magazine outline on -X clear.
    for y in (12.625,15.5):
        add(f'optic_mount_{y}',[12.25,y,6.875],[13.25,y+.75,7.625],'metal')
    add('compact_scope_tube',[13.125,12.25,6.75],[14.625,17.75,7.875],'dark')
    for name,y0,y1 in (('ocular',12,13),('objective',17,18.125)):
        e.extend(rounded_prism('scope_'+name,[13, y0,6.375],[14.875,y1,8.25],'y','dark'))
    ocular=add('scope_rear_glass',[13.4375,11.9375,6.8125],[14.4375,12,7.8125],'dark')
    ocular['faces']['down']['texture']='#optic_glass'
    objective=add('scope_front_glass',[13.4375,18.125,6.8125],[14.4375,18.1875,7.8125],'dark')
    objective['faces']['up']['texture']='#optic_glass'
    add('stock_retention_return',[10.375,8,8.125],[11.375,8.75,8.875],'metal')
    add('barrel_retention_return',[10.625,23.5,7.875],[11.375,24.25,8.875],'metal')
    add('stock_base_stop',[7.5,4.125,8.25],[12.25,4.75,11.25],'metal')
    # Center the complete rifle/optic silhouette, moving its mounts with it.
    assembly=e[weapon_start:]
    left=min(bounds(p)[0][0] for p in assembly)
    right=max(bounds(p)[1][0] for p in assembly)
    shift=8-(left+right)/2
    for p in assembly:
        for key in ('from','to'): p[key][0]+=shift
        if 'rotation' in p: p['rotation']['origin'][0]+=shift
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

/** Generated by tools/generate_police_armory.py; decoration, part, facing. */
final class PoliceArmoryShapes {
    private static final VoxelShape[][][] SHAPES = {
'''+',\n'.join(decorations)+'''
    };

    private PoliceArmoryShapes() { }

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
    (ROOT/'src/main/java/com/craftdistrict/block/PoliceArmoryShapes.java').write_text(source,encoding='utf-8')


def main(selected=None):
    if selected:
        assert selected<=set(IDS),f'Unknown decoration: {selected-set(IDS)}'
    textures(); layouts=[]
    for index,(id,create) in enumerate(zip(IDS,(radios,equipment,pistols,rifles))):
        e=create(); layouts.append(copy.deepcopy(e)); states={}
        if index in (0,1):
            continue  # Approved radio stand and equipment rack stay untouched.
        if selected and id not in selected:
            continue
        tall=index in (1,3); limits=(16,32 if tall else 16,16)
        for p in e:
            lo,hi=bounds(p)
            assert all(-1e-6<=lo[a]<hi[a]<=limits[a]+1e-6 for a in range(3)),p['name']
        omit_hidden_faces(e); validate_model_elements(e)
        for part,region in enumerate(REGIONS[index]):
            pieces=[p for original in e if (p:=crop(original,region)) is not None]
            validate_model_elements(pieces)
            assert all(-16<=v<=32 for p in pieces for k in ('from','to') for v in p[k])
            assert all(0<=v<=16 for p in pieces for f in p['faces'].values() for v in f['uv'])
            name=f'{id}_{part}' if tall else id
            write_json(ASSETS/f'models/block/police/{name}.json',{
                'parent':'minecraft:block/block','textures':materials(pieces),'elements':pieces})
            for facing,angle in (('north',0),('east',90),('south',180),('west',270)):
                key=f'facing={facing}'
                if tall: key+=f',primary={str(part==0).lower()}'
                states[key]={'model':f'craftdistrict:block/police/{name}','y':angle}
        write_json(ASSETS/f'blockstates/{id}.json',{'variants':states})
        item=copy.deepcopy(e)
        if tall:
            for p in item:
                for k in ('from','to'): p[k][1]-=8
                if 'rotation' in p: p['rotation']['origin'][1]-=8
        scale=.55 if tall else .85
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


if __name__=='__main__': main(set(sys.argv[1:]) or None)
