"""Static asset regression checks; these do not simulate a Minecraft world."""

import sys
import unittest

sys.dont_write_bytecode = True
import generate_bakery_pastry_trays as trays


class PastryTrayAssetTests(unittest.TestCase):
    def test_exact_family_ids_and_order(self):
        self.assertEqual(trays.IDS, ('croissant_tray', 'cookie_tray', 'bread_rolls_tray', 'empty_baking_tray'))

    def test_all_models_have_faces_and_stay_low(self):
        for model_id in trays.IDS:
            with self.subTest(model_id=model_id):
                model, collision = trays.arrangement(model_id)
                self.assertTrue(collision)
                for element in model['elements']:
                    self.assertTrue(element['faces'])
                    self.assertLessEqual(len(element['faces']), 6)
                    lo, hi = trays.bounds(element)
                    self.assertGreaterEqual(lo[1], -1e-6)
                    self.assertLess(hi[1], 3)
                    for face in element['faces'].values():
                        self.assertIn(face['texture'][1:], model['textures'])
                for lo, hi in collision:
                    self.assertTrue(all(0 <= lo[i] <= hi[i] <= 16 for i in range(3)))

    def test_empty_tray_is_just_floor_and_rim(self):
        model, collision = trays.arrangement('empty_baking_tray')
        self.assertEqual(len(model['elements']), 5)
        self.assertEqual(len(collision), 5)
        self.assertEqual(set(model['textures']), {'metal', 'rim', 'particle'})

    def test_texture_turn_rotates_crop_and_vertices_together(self):
        face = {'uv': [2, 3, 12, 14], 'rotation': 0}
        trays.turn_top_texture(face, 90)
        self.assertEqual(face, {'uv': [3, 4, 14, 14], 'rotation': 90})
        trays.turn_top_texture(face, 270)
        self.assertEqual(face, {'uv': [2, 3, 12, 14], 'rotation': 0})

    def test_products_touch_floor_without_spilling_over_rim(self):
        for model_id in trays.IDS[:-1]:
            model, _ = trays.arrangement(model_id)
            products = model['elements'][5:]
            self.assertAlmostEqual(min(trays.bounds(p)[0][1] for p in products), trays.FLOOR, delta=1e-6)
            for p in products:
                lo, hi = trays.bounds(p)
                self.assertGreaterEqual(lo[0], 2.5 - 1e-6)
                self.assertLessEqual(hi[0], 13.5 + 1e-6)
                self.assertGreaterEqual(lo[2], 3.5 - 1e-6)
                self.assertLessEqual(hi[2], 12.5 + 1e-6)

    def test_croissant_front_center_remains_open(self):
        pieces, _, _ = trays.croissant('sample')
        # Inverse-transform a point in the mouth of the crescent. It must not
        # lie in any cuboid; using boxes instead of bounds respects the curves.
        from generate_flour_sacks import rotate_point
        point = (2.3, .4, .7)
        for piece in pieces:
            p = point
            if 'rotation' in piece:
                r = piece['rotation']
                p = rotate_point(point, r['origin'], r['axis'], -r['angle'])
            self.assertFalse(all(piece['from'][i] <= p[i] <= piece['to'][i] for i in range(3)))


if __name__ == '__main__':
    unittest.main()
