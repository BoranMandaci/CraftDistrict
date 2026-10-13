"""Static Stage 1 regression checks; no running-world behavior is claimed."""

import json
import sys
import unittest

sys.dont_write_bytecode = True
from PIL import Image
import generate_police_entrance as tools


class PoliceEntranceAssetsTest(unittest.TestCase):
    def test_exact_stage_ids_registration_and_mounting(self):
        self.assertEqual(tools.IDS,("police_reception_desk","police_department_sign","police_waiting_area_bench"))
        registry=(tools.ROOT/'src/main/java/com/craftdistrict/registry/CraftDistrictBlocks.java').read_text()
        for block_id in tools.IDS: self.assertEqual(registry.count(f'register("{block_id}"'),1)
        block=(tools.ROOT/'src/main/java/com/craftdistrict/block/PoliceEntranceBlock.java').read_text()
        self.assertIn('extends MultiBlockDecorationBlock',block)
        self.assertIn('getClockWise()',block)
        self.assertIn('context.getClickedFace()',block)
        self.assertIn('face.getAxis().isHorizontal() ? face : null',block)
        self.assertIn('isFaceSturdy(level, support, direction.getOpposite())',block)
        base=(tools.ROOT/'src/main/java/com/craftdistrict/block/MultiBlockDecorationBlock.java').read_text()
        self.assertIn('return context.getHorizontalDirection().getOpposite()',base)
        self.assertIn('stateForPart(placementState, index).canSurvive(level, partPos)',base)
        self.assertIn('stateForPart(state, index).canSurvive(level, partPos)',base)
        self.assertIn('!state.canSurvive(level, pos)',base)
        self.assertIn('record RemovalKey(Level level, Block block, BlockPos origin)',base)
        self.assertIn('if (removing.add(key))',base)
        self.assertNotIn('ThreadLocal<Boolean>',base)

    def test_two_parts_eight_states_and_legal_models(self):
        for block_id in tools.IDS:
            states=json.loads((tools.ASSETS/f'blockstates/{block_id}.json').read_text())['variants']
            self.assertEqual(set(states),{f'facing={face},primary={primary}' for face in ('north','east','south','west') for primary in ('true','false')})
            elements,_=tools.arrangement(block_id)
            self.assertLessEqual(len(elements),48 if block_id=='police_reception_desk' else 40)
            for part,label in ((0,'primary'),(1,'extension')):
                model=json.loads((tools.MODELS/f'{block_id}_{label}.json').read_text())
                self.assertEqual(model['elements'],[p for e in elements if (p:=tools.split_part(e,part)) is not None])
                tools.validate_model_elements(model['elements'])
                for e in model['elements']:
                    lo,hi=tools.bounds(e)
                    self.assertTrue(all(-1e-6 <= lo[i] < hi[i] <= 16+1e-6 for i in range(3)),e['name'])
                    if 'rotation' in e:
                        self.assertIn(e['rotation']['angle'],(-45,-22.5,0,22.5,45))
                    for face in e['faces'].values():
                        self.assertTrue(all(0 <= v <= 16 for v in face['uv']))
                        self.assertNotIn('cullface',face)
                        self.assertIn(face['texture'][1:],model['textures'])

    def test_seam_uvs_and_directional_text(self):
        fixture=tools.box('crossing',[2,4,3],[30,8,4],'label')
        a,b=tools.split_part(fixture,0),tools.split_part(fixture,1)
        self.assertEqual(a['faces']['north']['uv'],[8,0,16,16])
        self.assertEqual(b['faces']['north']['uv'],[0,0,8,16])
        self.assertEqual(a['faces']['south']['uv'],[0,0,8,16])
        self.assertEqual(b['faces']['south']['uv'],[8,0,16,16])
        self.assertNotIn('east',a['faces'])
        self.assertNotIn('west',b['faces'])
        plaque=next(p for p in tools.department_sign() if p['name']=='outward_police_lettering')
        self.assertEqual(plaque['faces']['north'],{'texture':'#label','uv':[0,0,16,16]})
        self.assertEqual([name for name,f in plaque['faces'].items() if f['texture']=='#label'],['north'])
        image=Image.open(tools.ASSETS/'textures/block/police/entrance_label.png')
        self.assertEqual(image.size,(64,16))
        # Exact asymmetric P glyph provides a directional regression fixture.
        for y,row in enumerate(tools.GLYPHS['P']):
            for x,mark in enumerate(row):
                self.assertEqual(image.getpixel((18+x,4+y))[:3],(236,241,245) if mark=='1' else (38,54,76))

    def test_bench_has_three_seats_and_open_underframe(self):
        elements=tools.waiting_bench()
        seats=[e for e in elements if e['name'].endswith('_padded_surface')]
        backs=[e for e in elements if e['name'].endswith('_backrest')]
        self.assertEqual(len(seats),3); self.assertEqual(len(backs),3)
        self.assertEqual(sum(e['name'].endswith('_floor_foot') for e in elements),2)
        supports=[e for e in elements if e['name'].endswith('_back_support')]
        crossbar=next(e for e in elements if e['name']=='back_support_crossbar')
        self.assertEqual(len(supports),2)
        for support in supports:
            self.assertEqual(support['to'][1],crossbar['to'][1])
        for a,b in zip(seats,seats[1:]): self.assertGreater(b['from'][0],a['to'][0])
        for back in backs: self.assertEqual(back['rotation']['angle'],22.5)
        # Central space under seats stays open rather than becoming a full cube.
        for e in elements:
            lo,hi=tools.bounds(e)
            self.assertFalse(all(lo[i] < v < hi[i] for i,v in enumerate((16,2,6))))

    def test_reception_counter_height_and_staff_props(self):
        e={part['name']:part for part in tools.reception_desk()}
        self.assertEqual(e['light_public_counter_ledge']['to'][1],16)
        self.assertEqual(e['light_public_counter_ledge']['from'][0],0)
        self.assertEqual(e['light_public_counter_ledge']['to'][0],32)
        staff=e['light_staff_work_surface']
        self.assertEqual(staff['to'][1],11.5)
        self.assertEqual(staff['from'][2],e['light_public_counter_ledge']['to'][2])
        for name in ('monitor_foot','keyboard','telephone_base','document_folder','secondary_file','pen_cup'):
            self.assertEqual(e[name]['from'][1],staff['to'][1])
            self.assertGreaterEqual(e[name]['from'][2],staff['from'][2])
            self.assertLessEqual(e[name]['to'][2],staff['to'][2])
        screen=e['monitor_staff_facing_screen']
        self.assertEqual(screen['faces']['south'],{'texture':'#monitor','uv':[0,0,16,16]})
        self.assertEqual([name for name,f in screen['faces'].items() if f['texture']=='#monitor'],['south'])
        housing=e['monitor_housing']
        self.assertEqual(housing['to'][0]-housing['from'][0],8.5)
        self.assertEqual(housing['to'][1]-housing['from'][1],3.5)
        phone=e['telephone_keypad_and_display']
        self.assertEqual(phone['faces']['up'],{'texture':'#phone','uv':[0,0,16,16]})
        grip=e['telephone_handset_grip']
        self.assertLess(grip['to'][0],phone['from'][0])
        self.assertGreater(grip['from'][1],e['telephone_base']['to'][1])
        for name in ('telephone_handset_front_receiver','telephone_handset_rear_receiver'):
            receiver=e[name]
            self.assertLess(receiver['to'][0],phone['from'][0])
            self.assertTrue(all(min(grip['to'][i],receiver['to'][i]) >= max(grip['from'][i],receiver['from'][i]) for i in range(3)))

    def test_fitted_collision_crops_and_rotates(self):
        for block_id in tools.IDS:
            elements,collision=tools.arrangement(block_id)
            for part in range(2):
                envelopes=[]
                for original_lo,original_hi in collision:
                    low,high=max(original_lo[0],part*16),min(original_hi[0],part*16+16)
                    if low >= high: continue
                    lo,hi=[low-part*16,*original_lo[1:]],[high-part*16,*original_hi[1:]]
                    envelopes.append((lo,hi))
                    start=lo+hi
                    for _ in range(4):
                        self.assertTrue(all(-1e-6 <= lo[i] < hi[i] <= 16+1e-6 for i in range(3)))
                        lo,hi=[16-hi[2],lo[1],lo[0]],[16-lo[2],hi[1],hi[0]]
                    for a,b in zip(lo+hi,start): self.assertAlmostEqual(a,b)
                for original in elements:
                    e=tools.split_part(original,part)
                    if e is None: continue
                    lo,hi=tools.bounds(e)
                    self.assertTrue(any(all(a[i]-1e-6 <= lo[i] < hi[i] <= b[i]+1e-6 for i in range(3)) for a,b in envelopes),e['name'])
                if block_id=='police_department_sign':
                    self.assertEqual(max(hi[2] for lo,hi in envelopes),16)
                    self.assertGreaterEqual(min(lo[2] for lo,hi in envelopes),14.625)

    def test_resources_item_and_one_item_loot(self):
        for block_id in tools.IDS:
            for path in (tools.MODELS/f'{block_id}_primary.json',tools.MODELS/f'{block_id}_extension.json',tools.ASSETS/f'models/item/{block_id}.json'):
                model=json.loads(path.read_text()); tools.validate_model_elements(model['elements'])
                for texture in model['textures'].values(): self.assertTrue((tools.ASSETS/f'textures/{texture.split(":")[1]}.png').is_file(),texture)
            loot=json.loads((tools.ROOT/f'src/main/resources/data/craftdistrict/loot_tables/blocks/{block_id}.json').read_text())
            self.assertEqual(loot['pools'][0]['entries'],[{'type':'minecraft:item','name':f'craftdistrict:{block_id}'}])
            self.assertEqual(loot['pools'][0]['rolls'],1)
        for tile in tools.TILES:
            image=Image.open(tools.ASSETS/f'textures/block/police/entrance_{tile}.png').convert('RGBA')
            self.assertEqual(image.getchannel('A').getextrema(),(255,255))


if __name__=='__main__': unittest.main()
