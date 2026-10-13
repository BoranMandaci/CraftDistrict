"""Regression checks for vanilla-loadable flour sack block/item models."""

import sys
import unittest

sys.dont_write_bytecode = True
import generate_flour_sacks as sacks


class FlourSackModelTests(unittest.TestCase):
    def test_fully_hidden_element_is_omitted(self):
        outer = sacks.box('outer', [0, 0, 0], [8, 8, 8], 'cloth')
        inner = sacks.box('inner', [1, 1, 1], [7, 7, 7], 'gathers')
        elements = [outer, inner]
        sacks.omit_hidden_faces(elements)
        self.assertEqual(elements, [outer])
        self.assertEqual(len(outer['faces']), 6)

    def test_partly_exposed_element_remains(self):
        outer = sacks.box('outer', [0, 0, 0], [8, 8, 8], 'cloth')
        exposed = sacks.box('exposed', [2, 2, 2], [9, 6, 6], 'gathers')
        elements = [outer, exposed]
        sacks.omit_hidden_faces(elements)
        self.assertIn(exposed, elements)
        self.assertIn('east', exposed['faces'])
        sacks.validate_model_elements(elements)

    def test_empty_face_map_is_rejected(self):
        with self.assertRaises(ValueError):
            sacks.validate_model_elements([{'name': 'invalid', 'faces': {}}])

    def test_every_arrangement_has_loadable_elements(self):
        for variant, layout in sacks.LAYOUTS.items():
            with self.subTest(variant=variant):
                elements, collision = sacks.arrangement(layout)
                self.assertTrue(elements)
                self.assertEqual(len(collision), len(layout) * 3)
                for element in elements:
                    self.assertGreaterEqual(len(element['faces']), 1)
                    self.assertLessEqual(len(element['faces']), 6)


if __name__ == '__main__':
    unittest.main()
