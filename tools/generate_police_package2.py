"""Static police props; editable parts also drive cached collision data.

Only this package's assets and PolicePackage2Shapes.java are written. -Z is
front; the interrogation table occupies three floor cells lengthwise along +Z.
The approved investigation board is read for shapes but its assets are not rewritten.
"""

import copy
import math
import sys

sys.dont_write_bytecode=True
from PIL import Image
from generate_police_entrance import ASSETS, ROOT, MATERIALS as PALETTE, box
from generate_flour_sacks import bounds, omit_hidden_faces, validate_model_elements, rounded_prism, bake_y_quarter_turn
from generate_bakery_display_counter import write_json

IDS=('police_investigation_board','police_interrogation_table','police_fingerprint_station','police_interrogation_chair',
     'police_holding_cell_bars','police_holding_cell_door','police_security_camera','police_lockers')
TILES=('cork','map','photo_building','photo_vehicle','note','string','recorder','scanner','booking','status',
       'lens','grille','locker_label','case_notes')
MATERIALS={name:f'craftdistrict:block/police/package2_{name}' for name in TILES}
MATERIALS.update({name:PALETTE[name] for name in ('navy','metal','light','dark','folder','paper','seat')})
MATERIALS['particle']=MATERIALS['metal']
REGIONS=(({0:(0,16)},{0:(16,32)}),tuple({2:(i*16,i*16+16)} for i in range(3)),
         ({},),({},),({},),({1:(0,16)},{1:(16,32)}),({},),({},),({},))
# U axis/sign, V axis/sign, normal axis/sign, using outward Minecraft face bases.
FACE_AXES={'north':(0,-1,1,-1,2,-1),'south':(0,1,1,-1,2,1),
           'west':(2,1,1,-1,0,-1),'east':(2,-1,1,-1,0,1),
           'up':(0,1,2,1,1,1),'down':(0,1,2,-1,1,-1)}


def textures():
    for name in ('lens','grille','locker_label','case_notes'):
        path=ASSETS/f'textures/block/police/package2_{name}.png'
        if path.is_file():
            continue  # This revision changes geometry, not approved texture pixels.
        size=32 if name in ('map','photo_building','photo_vehicle','scanner') else 16
        image=Image.new('RGBA',(size,size))
        for y in range(size):
            for x in range(size):
                if name=='cork':
                    s=(-8,0,5)[(x*7+y*11)%3]; color=(117+s,99+s,75+s)
                elif name=='map':
                    color=(185,196,191)
                    if x in (6,7,18,19,26) or y in (7,8,21,22): color=(111,143,150)
                    if 11<=x<=15 and 12<=y<=17: color=(117,145,125)
                    if (x,y) in ((12,10),(13,10),(23,16),(23,17)): color=(166,67,65)
                elif name=='photo_building':
                    color=(118,141,151) if y<15 else (91,101,109)
                    if 6<=x<=25 and 8<=y<=25: color=(62,77,91)
                    if x in (9,10,15,16,21,22) and y in (11,12,17,18): color=(181,182,163)
                    if 13<=x<=18 and 21<=y<=27: color=(38,47,58)
                elif name=='photo_vehicle':
                    color=(119,137,143) if y<21 else (76,83,94)
                    if 7<=x<=24 and 15<=y<=22 or 11<=x<=21 and 11<=y<=15: color=(43,59,76)
                    if 12<=x<=19 and 12<=y<=14: color=(167,189,195)
                    if (x-10)**2+(y-23)**2<=5 or (x-23)**2+(y-23)**2<=5: color=(27,34,42)
                elif name=='note':
                    color=(228,222,203)
                    if y in (2,3) and 2<=x<=12: color=(88,110,132)
                    if y in (6,9,12) and 2<=x<=(13 if y==6 else 10): color=(128,138,142)
                elif name=='string': color=(190,49,46)
                elif name=='recorder':
                    color=(43,52,64)
                    if 2<=x<=10 and 2<=y<=6: color=(107,157,154)
                    if y in (10,11) and x in (3,4,7,8): color=(156,169,177)
                    if x in (12,13) and y in (10,11): color=(176,60,57)
                elif name=='scanner':
                    color=(21,46,50)
                    r=math.hypot((x-15.5)/11,(y-16)/13)
                    if r<=1 and int(r*9)%2==0 and not (x>18 and y>23): color=(105,187,164)
                    if y in (2,29) and 3<=x<=28 or x in (2,29) and 3<=y<=28: color=(126,169,177)
                elif name=='booking':
                    color=(27,46,61)
                    if 2<=x<=13 and y in (2,3): color=(64,121,145)
                    if 3<=x<=11 and y in (7,10,13): color=(138,178,184)
                    if x in (12,13) and y in (7,10): color=(83,166,130)
                elif name=='lens':
                    r=math.hypot(x-7.5,y-7.5)
                    color=(40,47,55) if r>6.5 else (97,111,121) if r>5 else (16,29,36)
                    if 5<=x<=7 and 4<=y<=6: color=(91,149,159)
                elif name=='grille':
                    color=(81,94,104)
                    if y in (3,6,9,12) and 2<=x<=13: color=(31,39,47)
                elif name=='locker_label':
                    color=(203,212,215)
                    if y in (5,9) and 3<=x<=12: color=(84,102,117)
                elif name=='case_notes':
                    color=(226,230,231)
                    if y in (2,3) and 2<=x<=13: color=(67,88,112)
                    if y in (6,9,12) and 2<=x<=(12 if y==6 else 10): color=(137,151,164)
                else: color=(87,153,138) if y<8 else (159,175,186)
                image.putpixel((x,y),(*color,255))
        path=ASSETS/f'textures/block/police/package2_{name}.png'
        path.parent.mkdir(parents=True,exist_ok=True); image.save(path)


def investigation():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    add('backing',[0,1,15.5],[32,15,16],'dark')
    add('cork_noticeboard',[1,1.75,14.75],[31,14.25,15.5],'cork')
    for name,a,b in (
        ('top_frame',[0,14.25,14.5],[32,15,15.5]),
        ('bottom_frame',[0,1,14.5],[32,1.75,15.5]),
        ('left_frame',[0,1.75,14.5],[1,14.25,15.5]),
        ('right_frame',[31,1.75,14.5],[32,14.25,15.5])):
        add(name,a,b,'metal')
    for name,x,y,w,h,z,tile in (
        ('central_city_map',11,3,11,9.5,14.5625,'map'),
        ('building_photo',2.5,8,7,5,14.5,'photo_building'),
        ('vehicle_photo',24,8.75,5.5,5,14.5,'photo_vehicle'),
        ('left_case_note',2.5,2,6,4.5,14.4375,'note'),
        ('right_case_note',24.5,2.5,5.5,4.75,14.4375,'note'),
        ('overlapping_note',20.5,1.75,4.5,2.75,14.3125,'note'),
        ('small_case_label',8.5,6.25,3,1.5,14.3125,'note')):
        add(name+'_paper',[x,y,z+.0625],[x+w,y+h,14.75],'paper')
        printed=add(name+'_image',[x+.25,y+.25,z],[x+w-.25,y+h-.25,z+.0625],'paper')
        printed['faces']['north']['texture']='#'+tile
    def link(name,x,y,length,angle):
        p=add(name,[x,y-.0625,14.1875],[x+length,y+.0625,14.25],'string')
        if angle: p['rotation']={'axis':'z','angle':angle,'origin':[x,y,14.21875],'rescale':False}
        return (x+length*math.cos(math.radians(angle)),y+length*math.sin(math.radians(angle)))
    endpoints=[(7,10),(19.5,9),(7,4),(13,11)]
    endpoints += [link('photo_to_map',7,10,9.2,-22.5),link('map_to_vehicle',19.5,9,5.95,22.5),
                  link('note_to_map',7,4,6,0),link('across_map',13,11,6,0)]
    for i,(x,y) in enumerate(endpoints):
        add(f'connection_pin_{i}',[x-.1875,y-.1875,14.0625],[x+.1875,y+.1875,14.1875],'string')
    return e


def aim_left(elements,cx,cz,z_shift=0):
    """Bake a quarter-turn toward -X, including bevel axes and texture faces."""
    def point(p): return [cx+p[2]-cz,p[1],cz-(p[0]-cx)+z_shift]
    for p in elements:
        turned=bake_y_quarter_turn(p,(cx,0,cz),90)
        for key in ('from','to'): turned[key][2]+=z_shift
        if 'rotation' in turned:
            r=turned['rotation']; r['origin']=point(r['origin'])
            if r['axis']=='z': r['axis']='x'
            elif r['axis']=='x': r['axis']='z'; r['angle']=-r['angle']
        p.clear(); p.update(turned)


def interrogation():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    add('light_institutional_tabletop',[1.5,12.5,1],[14.5,13.5,47],'light')
    for x in (1.5,13.875):
        add(f'long_edge_trim_{x}',[x,12.125,1],[x+.625,12.5,47],'metal')
        add(f'recessed_side_apron_{x}',[x+.0625,11,2],[x+.5625,12.125,46],'dark')
    for z in (2.5,43.5):
        for x in (2.5,12.25):
            add(f'square_steel_leg_{x}_{z}',[x,.375,z],[x+1.25,12.5,z+1.5],'dark')
            add(f'rubber_foot_{x}_{z}',[x-.125,0,z-.125],[x+1.375,.375,z+1.625],'dark')
        add(f'end_support_rail_{z}',[3.75,4,z+.375],[12.25,4.875,z+1.125],'metal')
        add(f'table_end_frame_{z}',[3.75,11,z+.375],[12.25,12.5,z+1.125],'dark')
    add('central_longitudinal_brace',[7.625,4.125,3.25],[8.375,4.75,44.25],'dark')
    add('recorder_rubber_mount',[3,13.5,18],[8.25,13.6875,23],'dark')
    device=add('audio_recorder',[3.25,13.6875,18.25],[8,14.5,22.75],'dark')
    device['faces']['up']['texture']='#recorder'
    add('recorder_side_trim',[3.125,13.6875,18.75],[3.25,14.25,22.25],'metal')
    mic_start=len(e)
    e.extend(rounded_prism('weighted_microphone_base',[9.5,13.5,20],[12.5,13.9375,22.75],'y','dark'))
    add('microphone_gooseneck',[10.75,13.9375,21.25],[11.25,15.125,21.75],'metal')
    e.extend(rounded_prism('microphone_capsule',[10.125,14.5,19.625],[11.875,16,21.5],'z','dark'))
    grille=add('microphone_front_mesh',[10.4375,14.8125,19.5625],[11.5625,15.6875,19.625],'grille')
    aim_left(e[mic_start:],11,21.375)
    camera_start=len(e)
    add('recording_camera_foot',[9.375,13.5,9.125],[13.125,13.875,12.25],'dark')
    add('recording_camera_short_stand',[10.875,13.875,10.375],[11.625,14.5,11.125],'metal')
    e.extend(rounded_prism('recording_camera_body',[9.25,14.5,9.25],[13.25,16,11.875],'z','metal'))
    add('recording_camera_rear_panel',[9.875,14.875,11.875],[12.625,15.625,12],'dark')
    lens=add('recording_camera_lens',[10.375,14.6875,8.875],[12.125,15.8125,9.25],'dark')
    lens['faces']['north']['texture']='#lens'
    add('recording_camera_status',[12.625,15.125,9.1875],[12.9375,15.4375,9.25],'string')
    # Both devices observe one subject seated beside the middle cell at -X.
    aim_left(e[camera_start:],11.25,10.5,16)
    add('aligned_case_file',[4,13.5,33],[12,13.75,42],'folder')
    add('square_paper_stack',[4.75,13.75,34],[11.25,13.9375,41],'paper')
    paper=add('aligned_interview_sheet',[4.75,13.9375,34],[11.25,14,41],'paper')
    paper['faces']['up']['texture']='#case_notes'
    return e


def fingerprint():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    for x in (2.75,12):
        for z in (3.25,11):
            add(f'rubber_pad_{x}_{z}',[x,0,z],[x+1,.375,z+1],'dark')
    add('compact_scanner_body',[2,.375,2.5],[14,1.875,12.75],'dark')
    add('front_bevel_step',[2.5,1.875,3],[13.5,2.125,9.25],'dark')
    add('scanner_metal_rim',[3,2.125,3.5],[10.75,2.3125,8.75],'metal')
    pad=add('fingerprint_scanning_pad',[3.3125,2.3125,3.8125],[10.4375,2.375,8.4375],'dark')
    pad['faces']['up']['texture']='#scanner'
    add('rear_display_riser',[4.75,1.875,10.25],[10.25,2.75,11.75],'dark')
    frame=add('compact_angled_display',[4.25,2.625,10],[10.75,5,11],'dark')
    screen=add('booking_display_image',[4.625,3,9.9375],[10.375,4.625,10],'dark')
    screen['faces']['north']['texture']='#booking'
    for p in (frame,screen): p['rotation']={'axis':'x','angle':22.5,'origin':[7.5,2.625,10.5],'rescale':False}
    for i,z in enumerate((4.125,5.75,7.375)):
        add(f'status_button_{i}',[11.5,2.125,z],[12.5,2.375,z+.75],'status')
    add('front_housing_seam',[3,.9375,2.4375],[13,1.125,2.5],'metal')
    add('rear_cable_socket',[7.125,.75,12.75],[8.875,1.375,13],'metal')
    return e


def interrogation_chair():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    for x in (3.25,11.75):
        for z in (3.25,11):
            add(f'plain_steel_leg_{x}_{z}',[x,.375,z],[x+1,7.375,z+1],'dark')
            add(f'non_marking_foot_{x}_{z}',[x-.0625,0,z-.0625],[x+1.0625,.375,z+1.0625],'dark')
        add(f'side_frame_rail_{x}',[x,3.5,4.25],[x+1,4.125,11],'metal')
        add(f'back_support_{x}',[x,7.375,11.25],[x+1,15,12.25],'dark')
    add('seat_shell',[3,7.375,2.75],[13,8,12.25],'metal')
    add('thin_muted_seat_pad',[3.375,8,3.125],[12.625,8.5,11.875],'seat')
    add('back_frame',[3,9.25,11.75],[13,15,12.625],'metal')
    add('plain_back_pad',[3.5,9.75,11.5],[12.5,14.5,11.75],'seat')
    add('rear_frame_crossbar',[4.25,5.5,11.125],[11.75,6.125,12],'dark')
    return e


def holding_bars():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    # Edge posts join directly to neighboring modules without off-cell geometry.
    for x in (0,15):
        add(f'outer_frame_{x}',[x,0,13],[x+1,16,16],'dark')
    for y in (0,14.75):
        add(f'horizontal_frame_{y}',[1,y,13],[15,y+1.25,16],'metal')
    for x in (2.5,5.5,9.5,12.5):
        add(f'vertical_steel_bar_{x}',[x,1.25,14],[x+1,14.75,15],'metal')
    add('bar_reinforcement',[1,7,13.5],[15,7.75,15.5],'dark')
    return e


def holding_door():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    for x in (0,15):
        add(f'door_edge_stile_{x}',[x,0,13],[x+1,32,16],'dark')
    for y in (0,30.75):
        add(f'end_rail_{y}',[1,y,13],[15,y+1.25,16],'metal')
    add('middle_door_rail',[1,15.5,13],[15,16.5,16],'dark')
    add('low_kickplate',[1,1.25,13.5],[15,5,15.5],'folder')
    for x in (2.5,5.5,9.5,12.5):
        add(f'lower_bar_{x}',[x,5,14],[x+1,15.5,15],'metal')
        add(f'upper_bar_{x}',[x,16.5,14],[x+1,30.75,15],'metal')
    for y in (6,24):
        add(f'hinge_knuckle_{y}',[0,y,13],[1.25,y+2,16],'metal')
    # Vanilla NORTH faces the player's looking direction; the visible front is +Z.
    add('lock_plate',[12,10.25,15.5],[15,14,16.25],'dark')
    add('lock_cylinder',[13.25,12.875,16.25],[14,13.625,16.5],'metal')
    add('short_pull_handle',[12.5,11.375,16.25],[14.5,11.875,16.75],'metal')
    add('rear_pull_handle',[12.5,11.375,12.5],[14.5,11.875,13],'metal')
    return e


def security_camera(ceiling=False):
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    if ceiling:
        add('ceiling_mount_plate',[6,15.25,7],[10,16,11],'metal')
        add('ceiling_drop_stem',[7.5,12.75,10],[8.5,15.25,11],'dark')
        add('ceiling_pivot_foot',[6.75,12.5,9.5],[9.25,12.875,11.25],'metal')
    else:
        add('wall_mount_plate',[6,4,15.375],[10,9,16],'metal')
        arm=add('angled_underside_support',[7.375,6.75,10.25],[8.625,7.5,15.125],'metal')
        arm['rotation']={'axis':'x','angle':45,'origin':[8,7.125,12],'rescale':False}
        add('wall_bracket_heel',[7.375,4.875,14],[8.625,5.625,15.375],'metal')
        add('underside_pivot_foot',[6.75,8.125,9.5],[9.25,8.625,11.25],'dark')
        add('bracket_pivot_bolt',[6.5,8.1875,10],[6.75,8.5625,10.75],'metal')
        add('rear_cable_run',[7.625,8.125,12.75],[8.375,8.5,14.75],'dark')
        add('cable_downturn',[7.625,6.875,14.375],[8.375,8.125,14.75],'dark')
        add('cable_wall_entry',[7.625,6.875,14.75],[8.375,7.5,15.375],'dark')
    # Reference-inspired single weatherproof enclosure: long body, beveled lower
    # corners, recessed dark optical window, deep protective hood and under-arm.
    e.extend(rounded_prism('long_weatherproof_housing',[5,8.5,3.25],[11,12.5,12],'z','metal'))
    add('front_metal_surround',[5.125,8.875,3.125],[10.875,12.125,3.25],'metal')
    add('recessed_optical_window',[5.5,9.25,3.0625],[10.5,11.75,3.125],'dark')
    lens=add('optical_lens',[6.75,9.3125,2.9375],[9.25,11.6875,3.0625],'dark')
    lens['faces']['north']['texture']='#lens'
    add('long_protective_top_hood',[4.75,12.5,2],[11.25,13,12.25],'metal')
    for x in (4.75,10.75):
        add(f'front_hood_cheek_{x}',[x,11.75,2],[x+.5,12.5,4],'metal')
    for x in (4.9375,11):
        add(f'long_side_service_seam_{x}',[x,10.125,4],[x+.0625,10.3125,11.5],'dark')
    add('rear_service_cover',[6,9.125,12],[10,11.875,12.125],'folder')
    add('cable_gland',[7.5,8.375,12.125],[8.5,9.25,12.75],'dark')
    add('small_status_light',[10.5625,11.375,3.0625],[10.8125,11.6875,3.125],'status')
    return e


def lockers():
    e=[]
    def add(name,a,b,mat):
        p=box(name,a,b,mat); e.append(p); return p
    add('full_height_cabinet_shell',[0,0,.75],[16,16,16],'folder')
    for x in (0,15.5):
        add(f'outer_front_frame_{x}',[x,0,0],[x+.5,16,.75],'metal')
    for y in (0,15.5):
        add(f'tileable_horizontal_edge_{y}',[.5,y,0],[15.5,y+.5,.75],'metal')
    add('center_divider',[7.75,.5,0],[8.25,15.5,.75],'dark')
    for x in (.5,8.25):
        add(f'locker_door_{x}',[x,.5,.25],[x+7.25,15.5,.75],'navy')
        vent=add(f'upper_louver_panel_{x}',[x+1,11.5,.1875],[x+6.25,14.25,.25],'dark')
        vent['faces']['north']['texture']='#grille'
        label=add(f'label_plate_{x}',[x+1.75,9.75,.1875],[x+5.5,10.875,.25],'metal')
        label['faces']['north']['texture']='#locker_label'
        add(f'handle_mount_{x}',[x+5.75,6.25,.125],[x+6.5,8.625,.25],'dark')
        add(f'vertical_pull_handle_{x}',[x+5.9375,6.625,0],[x+6.3125,8.25,.125],'metal')
        add(f'lower_door_vent_{x}',[x+1.125,2,.1875],[x+6.125,3.125,.25],'grille')
    return e


def crop(element,region):
    p=copy.deepcopy(element); a,b=element['from'],element['to']
    lo,hi=bounds(element)
    if any(hi[axis]<=low or lo[axis]>=high for axis,(low,high) in region.items()): return None
    if 'rotation' in element:
        assert all(low-1e-6<=lo[axis]<hi[axis]<=high+1e-6 for axis,(low,high) in region.items()),element['name']
    else:
        c,d=list(a),list(b)
        for axis,(low,high) in region.items(): c[axis],d[axis]=max(c[axis],low),min(d[axis],high)
        p['from'],p['to']=c,d
        for face,(ua,us,va,vs,normal,ns) in FACE_AXES.items():
            if face not in p['faces']: continue
            if ns<0 and c[normal]!=a[normal] or ns>0 and d[normal]!=b[normal]:
                del p['faces'][face]; continue
            uv=element['faces'][face]['uv']; spans=[]
            for axis,sign,u0,u1 in ((ua,us,uv[0],uv[2]),(va,vs,uv[1],uv[3])):
                f0,f1=((c[axis]-a[axis])/(b[axis]-a[axis]),(d[axis]-a[axis])/(b[axis]-a[axis])) if sign>0 else (
                    (b[axis]-d[axis])/(b[axis]-a[axis]),(b[axis]-c[axis])/(b[axis]-a[axis]))
                spans.append((u0+(u1-u0)*f0,u0+(u1-u0)*f1))
            p['faces'][face]['uv']=[round(v,6) for v in (spans[0][0],spans[1][0],spans[0][1],spans[1][1])]
    for axis,(low,high) in region.items():
        for k in ('from','to'): p[k][axis]-=low
        if 'rotation' in p: p['rotation']['origin'][axis]-=low
    return p if p['faces'] else None


def materials(e):
    used={f['texture'][1:] for p in e for f in p['faces'].values()}|{'particle'}
    return {k:v for k,v in MATERIALS.items() if k in used}


def door_pose(elements,hinge,opened):
    """Keep the hinge at the rear edge and reverse the leaf width when opened.

    This is the geometric equivalent of vanilla's distinct open-door UV templates.
    A center rotation alone puts the latch at the hinge end of the opened leaf.
    """
    geometry=copy.deepcopy(elements)
    if hinge=='right':
        for p in geometry:
            p['from'][0],p['to'][0]=16-p['to'][0],16-p['from'][0]
            faces=p['faces']
            p['faces']={('east' if f=='west' else 'west' if f=='east' else f):v for f,v in faces.items()}
            for face in p['faces'].values():
                uv=face['uv']; face['uv']=[uv[2],uv[1],uv[0],uv[3]]
    if opened:
        left=hinge=='left'
        names=({'north':'east','south':'west','west':'south','east':'north'} if left else
               {'north':'west','south':'east','west':'north','east':'south'})
        def point(p): return [16-p[2],p[1],16-p[0]] if left else [p[2],p[1],p[0]]
        for p in geometry:
            a,b=point(p['from']),point(p['to'])
            p['from']=[min(a[i],b[i]) for i in range(3)]
            p['to']=[max(a[i],b[i]) for i in range(3)]
            p['faces']={names.get(f,f):v for f,v in p['faces'].items()}
    return geometry


def write_holding_door(elements):
    """North is the closed south-edge plane, matching vanilla DoorBlock collision.

    Eight half/hinge/open models preserve front direction and latch/hinge symmetry.
    Powered is deliberately omitted from variant keys, as in vanilla door assets.
    """
    id='police_holding_cell_door'; states={}
    for p in elements:
        lo,hi=bounds(p)
        assert all(0<=lo[i]<hi[i]<=(16,32,17)[i] for i in range(3)),p['name']
    for hinge in ('left','right'):
        for opened in (False,True):
            geometry=door_pose(elements,hinge,opened)
            # Match vanilla's thin collision planes; small handles can protrude.
            frame=[p for p in geometry if p['name'].startswith('door_edge_stile')]
            if opened:
                lo=min(p['from'][0] for p in frame); hi=max(p['to'][0] for p in frame)
                assert (lo,hi)==((0,3) if hinge=='left' else (13,16))
            else:
                assert all(p['from'][2]==13 and p['to'][2]==16 for p in frame)
            omit_hidden_faces(geometry)
            for half,region in zip(('lower','upper'),REGIONS[5]):
                pieces=[p for original in geometry if (p:=crop(original,region)) is not None]
                validate_model_elements(pieces)
                assert all(-1<=v<=17 for p in pieces for k in ('from','to') for v in p[k])
                assert all(0<=v<=16 for p in pieces for f in p['faces'].values() for v in f['uv'])
                name=f'{id}_{half}_{hinge}'+('_open' if opened else '')
                write_json(ASSETS/f'models/block/police/{name}.json',{
                    'parent':'minecraft:block/block','textures':materials(pieces),'elements':pieces})
                for facing,turn in (('north',0),('east',90),('south',180),('west',270)):
                    key=f'facing={facing},half={half},hinge={hinge},open={str(opened).lower()}'
                    states[key]={'model':f'craftdistrict:block/police/{name}','y':turn}
    write_json(ASSETS/f'blockstates/{id}.json',{'variants':states})
    item=copy.deepcopy(elements); omit_hidden_faces(item)
    for p in item:
        p['from'][1]-=8; p['to'][1]-=8
    validate_model_elements(item)
    write_json(ASSETS/f'models/item/{id}.json',{
        'parent':'minecraft:block/block','textures':materials(item),'elements':item,'display':{
        'gui':{'rotation':[20,135,0],'scale':[.55]*3},
        'ground':{'translation':[0,4,0],'scale':[.25]*3},
        'fixed':{'rotation':[0,180,0],'scale':[.5]*3},
        'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.3]*3},
        'thirdperson_lefthand':{'rotation':[75,45,0],'scale':[.3]*3},
        'firstperson_righthand':{'rotation':[0,45,0],'scale':[.4]*3},
        'firstperson_lefthand':{'rotation':[0,225,0],'scale':[.4]*3}}})
    write_json(ROOT/f'src/main/resources/data/craftdistrict/loot_tables/blocks/{id}.json',{
        'type':'minecraft:block','pools':[{'rolls':1,
        'entries':[{'type':'minecraft:item','name':f'craftdistrict:{id}'}],
        'conditions':[{'condition':'minecraft:survives_explosion'},
                      {'condition':'minecraft:block_state_property','block':f'craftdistrict:{id}',
                       'properties':{'half':'lower'}}]}]})
    for path in materials(elements).values():
        assert (ASSETS/f'textures/{path.split(":")[1]}.png').is_file(),path
    (ASSETS/f'models/block/police/{id}.json').unlink(missing_ok=True)
    print(f'{id}: {len(elements)} cuboids, two vanilla door halves, 32 model states.')


def shapes_source(layouts):
    variants=[]
    for index,e in enumerate(layouts):
        parts=[]
        for region in REGIONS[index]:
            cells=[]
            for p in e:
                lo,hi=bounds(p)
                if any(hi[a]<=low or lo[a]>=high for a,(low,high) in region.items()): continue
                for axis,(low,high) in region.items(): lo[axis],hi[axis]=max(lo[axis],low)-low,min(hi[axis],high)-low
                cells.append('                Block.box('+', '.join(f'{v:.6f}'.rstrip('0').rstrip('.') for v in lo+hi)+')')
            parts.append('            rotations(Shapes.or(\n'+',\n'.join(cells)+').optimize())')
        variants.append('        {\n'+',\n'.join(parts)+'\n        }')
    source='''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_police_package2.py; decoration, part, facing. */
final class PolicePackage2Shapes {
    private static final VoxelShape[][][] SHAPES = {
'''+',\n'.join(variants)+'''
    };

    private PolicePackage2Shapes() {
    }

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
    (ROOT/'src/main/java/com/craftdistrict/block/PolicePackage2Shapes.java').write_text(source,encoding='utf-8')


def main(selected=None):
    if selected:
        assert selected<=set(IDS),f'Unknown decoration: {selected-set(IDS)}'
    textures(); layouts=[]
    creators=(investigation,interrogation,fingerprint,interrogation_chair,holding_bars,
              holding_door,security_camera,lockers)
    for index,(id,create) in enumerate(zip(IDS,creators)):
        e=create(); layouts.append(copy.deepcopy(e))
        if index in (0,2,3,7):
            continue  # Approved board, scanner, chair and lockers stay untouched.
        if selected and id not in selected:
            continue
        if index==5:
            write_holding_door(e)
            continue
        limits=(16,16,48) if index==1 else (16,16,16)
        for p in e:
            lo,hi=bounds(p)
            assert all(-1e-6<=lo[i]<hi[i]<=limits[i]+1e-6 for i in range(3)),p['name']
        omit_hidden_faces(e); validate_model_elements(e); states={}
        exports=[(part,region,e) for part,region in enumerate(REGIONS[index])]
        if index==6:
            ceiling=security_camera(True)
            for p in ceiling:
                lo,hi=bounds(p)
                assert all(0<=lo[i]<hi[i]<=16 for i in range(3)),p['name']
            omit_hidden_faces(ceiling)
            exports=[('wall',{},e),('ceiling',{},ceiling)]
        for part,region,geometry in exports:
            pieces=[p for original in geometry if (p:=crop(original,region)) is not None]
            validate_model_elements(pieces)
            for p in pieces:
                assert all(-16<=v<=32 for k in ('from','to') for v in p[k])
                assert all(0<=v<=16 for f in p['faces'].values() for v in f['uv'])
            name=f'{id}_{part}' if index<2 or index==6 else id
            write_json(ASSETS/f'models/block/police/{name}.json',{'parent':'minecraft:block/block','textures':materials(pieces),'elements':pieces})
            for facing,angle in (('north',0),('east',90),('south',180),('west',270)):
                key=f'facing={facing}'
                if index==0: key+=f',primary={str(part==0).lower()}'
                elif index==1: key+=f',part={part}'
                elif index==6: key+=f',ceiling={str(part=="ceiling").lower()}'
                states[key]={'model':f'craftdistrict:block/police/{name}','y':angle}
        write_json(ASSETS/f'blockstates/{id}.json',{'variants':states})
        item=copy.deepcopy(e)
        for p in item:
            if index==1:
                for k in ('from','to'): p[k][2]-=16
                if 'rotation' in p: p['rotation']['origin'][2]-=16
        assert all(-16<=v<=32 for p in item for k in ('from','to') for v in p[k])
        scale=.33 if index==1 else .85 if index==2 else .75
        hand_scale=.25 if index==1 else .5
        write_json(ASSETS/f'models/item/{id}.json',{'parent':'minecraft:block/block','textures':materials(item),'elements':item,'display':{
            'gui':{'rotation':[25,135,0],'scale':[scale,scale,scale]},
            'ground':{'translation':[0,3,0],'scale':[.25,.25,.25]},
            'fixed':{'rotation':[0,180,0],'scale':[.4,.4,.4]},
            'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.3,.3,.3]},
            'thirdperson_lefthand':{'rotation':[75,45,0],'scale':[.3,.3,.3]},
            'firstperson_righthand':{'rotation':[0,45,0],'scale':[hand_scale]*3},
            'firstperson_lefthand':{'rotation':[0,225,0],'scale':[hand_scale]*3}}})
        write_json(ROOT/f'src/main/resources/data/craftdistrict/loot_tables/blocks/{id}.json',{
            'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'craftdistrict:{id}'}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
        for path in materials(e).values(): assert (ASSETS/f'textures/{path.split(":")[1]}.png').is_file(),path
        print(f'{id}: {len(e)} cuboids, {len(REGIONS[index])} occupied parts, four orientations.')
    # Remove the old fourth quadrant after exporting the new three-cell table.
    if not selected or 'police_interrogation_table' in selected:
        (ASSETS/'models/block/police/police_interrogation_table_3.json').unlink(missing_ok=True)
    layouts.append(security_camera(True))  # Shape index 8: ceiling-mounted camera.
    shapes_source(layouts)


if __name__=='__main__': main(set(sys.argv[1:]) or None)
