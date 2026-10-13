"""Editable static pastry trays; Python standard library only, NORTH is front.

Each layout drives vanilla block/item JSON and cached Java collision envelopes.
Imports reuse established asset helpers without running another generator.
Only this new family and PastryTrayShapes.java are written.
"""

import copy
import math
import struct
import sys
import zlib

sys.dont_write_bytecode = True
from generate_bakery_display_counter import ASSETS, MODELS, ROOT, write_json
from generate_flour_sacks import (
    bounds, cloth_uvs as product_uvs, omit_hidden_faces, place,
    rounded_prism, validate_model_elements,
)

FACES = ('north', 'south', 'west', 'east', 'up', 'down')
TILES = ('metal', 'rim', 'croissant', 'croissant_top', 'cookie',
         'cookie_chip_a', 'cookie_chip_b', 'cookie_plain', 'roll', 'roll_top')
MATERIALS = {name: 'craftdistrict:block/bakery/bakery_trays_' + name for name in TILES}
MATERIALS['particle'] = MATERIALS['metal']
FLOOR = .35
# Order matches PastryTrayBlock.Variant, not a saved blockstate property.
IDS = ('croissant_tray', 'cookie_tray', 'bread_rolls_tray', 'empty_baking_tray')

# Center X/Z, scale and Y rotation for the three open crescent profiles.
CROISSANTS = ((5.1, 5.65, 1, 0), (5.5, 10.5, .97, 0), (10.6, 8.4, 1, 0))
# Center X/Z, diameter, thickness, top texture, Y rotation.
COOKIES = (
    (4.7, 5.5, 2.5, .65, 'cookie_chip_a', 22.5),
    (8.25, 5.6, 2.7, .6, 'cookie_plain', 0),
    (11.5, 6.1, 2.6, .7, 'cookie_chip_b', -22.5),
    (4.65, 10, 2.8, .7, 'cookie_plain', 22.5),
    (8.5, 10.4, 3, .65, 'cookie_chip_b', -22.5),
    (11.7, 10.7, 2.3, .6, 'cookie_chip_a', 0),
)
# Center X/Z, diameter, height, Y rotation, scoring UV quarter-turn.
ROLLS = ((5.4, 5.75, 3.2, 1.35, 0, 0), (10.8, 5.9, 3, 1.3, 0, 90),
         (4.6, 10.2, 2.8, 1.25, 0, 180), (8.2, 10.6, 3, 1.45, 0, 0),
         (11.7, 10.3, 2.8, 1.2, 0, 90))


def png(name, sampler):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    rows = b''.join(b'\0' + bytes(c for x in range(16) for c in sampler(x, y)) for y in range(16))
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b'IDAT', zlib.compress(rows, 9)) + chunk(b'IEND', b'')
    target = ASSETS / f'textures/block/bakery/bakery_trays_{name}.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)


def textures():
    def metal(x, y):
        value = (130, 139, 143)
        if (x, y) in {(3, 5), (4, 5), (5, 5), (10, 11), (11, 11)}:
            value = (156, 164, 166)
        elif x in (0, 15) or y in (0, 15):
            value = (111, 120, 125)
        elif (x * 3 + y * 5) % 17 == 0:
            value = (135, 144, 148)
        return (*value, 255)

    def rim(x, y):
        return (*((174, 181, 183) if y < 2 else (83, 94, 101) if y > 13
                  else (117, 128, 135) if (x + y) % 9 == 0 else (142, 151, 155)), 255)

    def baked(x, y, kind, top=False):
        palettes = {
            'croissant': ((186, 108, 38), (209, 139, 56), (229, 164, 77)),
            'cookie': ((180, 116, 56), (205, 146, 76), (222, 171, 99)),
            'roll': ((177, 103, 39), (201, 131, 52), (224, 160, 74)),
        }
        index = 1 if (x * 3 + y * 7) % 13 else 2
        if x in (0, 15) or y in (0, 15):
            index = 0
        color = palettes[kind][index]
        if top:
            color = tuple(min(255, c + 9) for c in color)
        return (*color, 255)

    def croissant_top(x, y):
        # Laminated dough bands are pixels, not extra strips of geometry.
        if (x + y // 3) % 5 == 1 and 2 <= y <= 13:
            return (243, 195, 117, 255)
        if (x + y // 3) % 5 == 0 and 2 <= y <= 13:
            return (185, 108, 37, 255)
        return baked(x, y, 'croissant', True)

    def cookie_top(x, y, variant):
        chips = ({(4, 5), (10, 4), (7, 10), (12, 11), (3, 12)},
                 {(5, 4), (11, 7), (5, 11), (10, 12)})
        if variant < 2:
            if any((x - cx, y - cy) in ((0, 0), (1, 0), (0, 1)) for cx, cy in chips[variant]):
                return (86 + variant * 10, 48 + variant * 7, 29, 255)
        elif (x * 5 + y * 3) % 19 == 0:
            return (238, 198, 137, 255)
        color = baked(x, y, 'cookie', True)
        return tuple(min(255, c + (3 if variant == 2 else 0)) for c in color[:3]) + (255,)

    def roll_top(x, y):
        if 5 <= y <= 10 and x in (4 + y // 4, 9 + y // 4):
            return (246, 206, 137, 255)
        radius = math.hypot(x - 7.5, y - 7.5)
        if radius > 7:
            return (187, 111, 39, 255)
        if radius > 5.5:
            return (214, 145, 57, 255)
        return (232, 173, 86, 255) if (x + y * 3) % 23 else (240, 187, 107, 255)

    samplers = {
        'metal': metal, 'rim': rim,
        'croissant': lambda x, y: baked(x, y, 'croissant'), 'croissant_top': croissant_top,
        'cookie': lambda x, y: baked(x, y, 'cookie'),
        'cookie_chip_a': lambda x, y: cookie_top(x, y, 0),
        'cookie_chip_b': lambda x, y: cookie_top(x, y, 1),
        'cookie_plain': lambda x, y: cookie_top(x, y, 2),
        'roll': lambda x, y: baked(x, y, 'roll'), 'roll_top': roll_top,
    }
    for name, sampler in samplers.items():
        png(name, sampler)


def box(name, start, end, material, top=None):
    element = {'name': name, 'from': list(start), 'to': list(end), 'faces': {
        face: {'texture': '#' + material, 'uv': [0, 0, 16, 16]} for face in FACES}}
    if top:
        element['faces']['up']['texture'] = '#' + top
    return element


def angled(name, center, size, angle, material, top):
    element = box(name, [center[i] - size[i] / 2 for i in range(3)],
                  [center[i] + size[i] / 2 for i in range(3)], material, top)
    element['rotation'] = {'axis': 'y', 'angle': angle, 'origin': list(center), 'rescale': False}
    return element


def tray():
    return [
        box('pressed_metal_floor', [2, 0, 3], [14, FLOOR, 13], 'metal'),
        box('front_rolled_rim', [2, FLOOR, 3], [14, .85, 3.5], 'rim'),
        box('rear_rolled_rim', [2, FLOOR, 12.5], [14, .85, 13], 'rim'),
        box('left_rolled_rim', [2, FLOOR, 3.5], [2.5, .85, 12.5], 'rim'),
        box('right_rolled_rim', [13.5, FLOOR, 3.5], [14, .85, 12.5], 'rim'),
    ]


def croissant(name):
    middle = rounded_prism(name + '_arched_middle', [1.3, 0, 1.95], [3.3, 1.5, 3.2], 'x', 'croissant')
    arm_size = (1.15, 1.1, 1.8)
    left = [place(p, (.32, 0, 1), arm_size, 22.5) for p in
            rounded_prism(name + '_left_curve', [0, 0, 0], arm_size, 'y', 'croissant')]
    right = [place(p, (3.12, 0, 1), arm_size, -22.5) for p in
             rounded_prism(name + '_right_curve', [0, 0, 0], arm_size, 'y', 'croissant')]
    left.append(angled(name + '_left_tapered_tip', [.85, .3, .7], [.7, .6, 1.1], -22.5, 'croissant', 'croissant_top'))
    right.append(angled(name + '_right_tapered_tip', [3.75, .3, .7], [.7, .6, 1.1], 22.5, 'croissant', 'croissant_top'))
    pieces = middle + left + right
    for piece in pieces:
        piece['faces']['up']['texture'] = '#croissant_top'
    return pieces, (middle, left, right), (4.6, 1.5, 3.4)


def cookie(name, diameter, height, top):
    pieces = rounded_prism(name, [0, 0, 0], [diameter, height, diameter], 'y', 'cookie')
    for piece in pieces:
        piece['faces']['up']['texture'] = '#' + top
    return pieces, (pieces,), (diameter, height, diameter)


def roll(name, diameter, height):
    # One continuous octagonal loaf; the rounded crust and shallow top scoring
    # are textured, without stacked caps or a square cabinet-like footprint.
    pieces = rounded_prism(name + '_round_loaf', [0, 0, 0], [diameter, height, diameter], 'y', 'roll')
    for piece in pieces:
        piece['faces']['up']['texture'] = '#roll_top'
    return pieces, (pieces,), (diameter, height, diameter)


def turn_top_texture(face, angle):
    # Turn the whole atlas field, including the cropped UV rectangle. Rotating
    # only each face's vertices would stretch radial shading into separate bands.
    for _ in range(angle // 90):
        u0, v0, u1, v1 = face['uv']
        face['uv'] = [v0, 16 - u1, v1, 16 - u0]
        face['rotation'] = (face.get('rotation', 0) + 90) % 360


def add_product(elements, collision, source, center, yaw, scale=1, scoring=0):
    pieces, groups, dimensions = source
    if scale != 1:
        for piece in pieces:
            for key in ('from', 'to'):
                piece[key] = [v * scale for v in piece[key]]
            if 'rotation' in piece:
                piece['rotation']['origin'] = [v * scale for v in piece['rotation']['origin']]
        dimensions = tuple(v * scale for v in dimensions)
    product_uvs(pieces, dimensions)
    position = (center[0] - dimensions[0] / 2, FLOOR, center[1] - dimensions[2] / 2)
    placed = [place(piece, position, dimensions, yaw) for piece in pieces]
    if scoring:
        # Add rather than replace the UV quarter-turn used to normalize an
        # element rotation. All scoring faces stay outward and unmirrored.
        for piece in placed:
            face = piece['faces'].get('up')
            if face and face['texture'] == '#roll_top':
                turn_top_texture(face, scoring)
    for part in placed:
        lo, hi = bounds(part)
        assert 2.5 - 1e-6 <= lo[0] <= hi[0] <= 13.5 + 1e-6, (part['name'], lo, hi)
        assert 3.5 - 1e-6 <= lo[2] <= hi[2] <= 12.5 + 1e-6, (part['name'], lo, hi)
        assert abs(lo[1] - FLOOR) < 1e-6 or lo[1] >= FLOOR, (part['name'], lo)
    elements.extend(placed)
    for group in groups:
        envelopes = [bounds(place(piece, position, dimensions, yaw)) for piece in group]
        collision.append(([min(a[i] for a, b in envelopes) for i in range(3)],
                          [max(b[i] for a, b in envelopes) for i in range(3)]))


def arrangement(model_id):
    elements = tray()
    collision = [bounds(piece) for piece in elements]
    if model_id == 'croissant_tray':
        for i, (x, z, scale, yaw) in enumerate(CROISSANTS):
            add_product(elements, collision, croissant(f'croissant_{i + 1}'), (x, z), yaw, scale)
    elif model_id == 'cookie_tray':
        for i, (x, z, diameter, height, top, yaw) in enumerate(COOKIES):
            add_product(elements, collision, cookie(f'cookie_{i + 1}', diameter, height, top), (x, z), yaw)
    elif model_id == 'bread_rolls_tray':
        for i, (x, z, diameter, height, yaw, scoring) in enumerate(ROLLS):
            add_product(elements, collision, roll(f'roll_{i + 1}', diameter, height),
                        (x, z), yaw, scoring=scoring)
    elif model_id != 'empty_baking_tray':
        raise ValueError(model_id)
    omit_hidden_faces(elements)
    validate_model_elements(elements)
    for piece in elements:
        lo, hi = bounds(piece)
        assert all(-1e-6 <= lo[i] <= hi[i] <= 16 + 1e-6 for i in range(3)), piece['name']
        assert all(math.isfinite(value) and -16 <= value <= 32
                   for corner in ('from', 'to') for value in piece[corner]), piece['name']
        if 'rotation' in piece:
            assert piece['rotation']['angle'] in (-45, -22.5, 0, 22.5, 45), piece['name']
        for face in piece['faces'].values():
            assert face['texture'][1:] in MATERIALS, piece['name']
            assert face.get('rotation', 0) in (0, 90, 180, 270), piece['name']
            assert len(face['uv']) == 4 and all(math.isfinite(value) and 0 <= value <= 16
                                               for value in face['uv']), piece['name']
    used = {face['texture'][1:] for piece in elements for face in piece['faces'].values()} | {'particle'}
    materials = {name: MATERIALS[name] for name in MATERIALS if name in used}
    return {'parent': 'minecraft:block/block', 'textures': materials, 'elements': elements}, collision


def write_shapes(shapes):
    rows = []
    for model_id, boxes in shapes.items():
        rows.extend([f'            // {model_id}', '            rotations(Shapes.or('])
        for i, (lo, hi) in enumerate(boxes):
            coords = ', '.join(str(round(v, 6)) for v in lo + hi)
            rows.append('                    Block.box(' + coords + ')' + (',' if i < len(boxes) - 1 else ').optimize()),'))
    source = '''package com.craftdistrict.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Generated by tools/generate_bakery_pastry_trays.py; edit layouts there. */
final class PastryTrayShapes {
    static final VoxelShape[][] SHAPES = {
''' + '\n'.join(rows) + '''
    };

    private PastryTrayShapes() {
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
    (ROOT / 'src/main/java/com/craftdistrict/block/PastryTrayShapes.java').write_text(source, encoding='utf-8')


def main():
    textures()
    shapes = {}
    for model_id in IDS:
        model, collision = arrangement(model_id)
        write_json(MODELS / f'{model_id}.json', model)
        write_json(ASSETS / f'blockstates/{model_id}.json', {'variants': {
            f'facing={facing}': {'model': f'craftdistrict:block/bakery/{model_id}', 'y': angle}
            for facing, angle in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
        item = copy.deepcopy(model)
        item['display'] = {
            'gui': {'rotation': [45, 135, 0], 'translation': [0, 5, 0], 'scale': [.9, .9, .9]},
            'ground': {'translation': [0, 3, 0], 'scale': [.5, .5, .5]},
            'fixed': {'rotation': [0, 180, 0], 'translation': [0, 5, 0], 'scale': [.8, .8, .8]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.5, .5, .5]},
            'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.5, .5, .5]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 4, 0], 'scale': [.7, .7, .7]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 4, 0], 'scale': [.7, .7, .7]},
        }
        write_json(ASSETS / f'models/item/{model_id}.json', item)
        write_json(ROOT / f'src/main/resources/data/craftdistrict/loot_tables/blocks/{model_id}.json', {
            'type': 'minecraft:block', 'pools': [{'rolls': 1,
            'entries': [{'type': 'minecraft:item', 'name': f'craftdistrict:{model_id}'}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        shapes[model_id] = collision
        print(f'{model_id}: {len(model["elements"])} visible cuboids, {len(collision)} cached shape envelopes, four facings')
    write_shapes(shapes)
    print(f'{len(TILES)} shared 16x16 opaque textures; only the pastry tray family was generated.')


if __name__ == '__main__':
    main()
