"""Resource/model regression checks; these do not exercise Minecraft world behavior."""

import itertools
import json
import math
import sys
import unittest

sys.dont_write_bytecode = True
import generate_industrial_dough_mixer as mixer
from generate_flour_sacks import bounds, rotate_point


def contains(element, point):
    if "rotation" in element:
        r = element["rotation"]
        point = rotate_point(point, r["origin"], r["axis"], -r["angle"])
    return all(element["from"][i] - 1e-6 <= point[i] <= element["to"][i] + 1e-6 for i in range(3))


class MixerAssetsTest(unittest.TestCase):
    def test_model_contract_and_footprint(self):
        whole = mixer.whole_machine()
        mixer.omit_hidden_faces(whole)
        models = [mixer.part(whole, row) for row in range(2)]
        models.append([mixer.shifted(e, -8) for e in whole])
        for index, elements in enumerate(models):
            mixer.validate_model_elements(elements)
            for e in elements:
                self.assertTrue(all(-16 <= value <= 32 for key in ("from", "to") for value in e[key]))
                if "rotation" in e:
                    self.assertIn(e["rotation"]["axis"], "xyz")
                    self.assertIn(e["rotation"]["angle"], (-45, -22.5, 0, 22.5, 45))
                low, high = bounds(e)
                for axis in (0, 2):
                    self.assertGreaterEqual(low[axis], -1e-6)
                    self.assertLessEqual(high[axis], 16 + 1e-6)
                if index < 2:
                    self.assertGreaterEqual(low[1], -1e-6)
                    self.assertLessEqual(high[1], 16 + 1e-6)
                for face in e["faces"].values():
                    self.assertIn(face["texture"][1:], mixer.MATERIALS)
                    self.assertTrue(all(0 <= uv <= 16 for uv in face["uv"]))
                    self.assertIn(face.get("rotation", 0), (0, 90, 180, 270))
        self.assertLess(len(whole), 120)

    def test_bowl_has_empty_center_and_closed_transitions(self):
        bowl = [e for e in mixer.whole_machine() if e["name"].startswith("bowl_")
                and e["name"] != "bowl_cradle" and not e["name"].startswith("bowl_lift")]
        for y in (13.5, 15.5, 16.5, 18, 18.75):
            self.assertFalse(any(contains(e, (8, y, 6.5)) for e in bowl), f"Bowl filled at y={y}")
        # Radial samples around the two stepped ledges must be supported.
        for y, radius in ((13, 3.7), (16, 4.7)):
            for angle in range(0, 360, 5):
                theta = math.radians(angle)
                apothem_factor = max(abs(math.cos(theta - i * math.pi / 4)) for i in range(8))
                distance = radius / apothem_factor
                point = (8 + distance * math.cos(theta), y, 6.5 + distance * math.sin(theta))
                self.assertTrue(any(contains(e, point) for e in bowl), (y, angle))

    def test_split_keeps_seam_geometry_and_uvs(self):
        whole = mixer.whole_machine()
        reconstructed = mixer.part(whole, 0) + [mixer.shifted(e, 16) for e in mixer.part(whole, 1)]
        # Same occupied volume on either side of the split, including diagonals.
        for x, y, z in itertools.product(range(1, 16), (15.99, 16.01), range(1, 16)):
            point = (x, y, z)
            self.assertEqual(any(contains(e, point) for e in whole),
                             any(contains(e, point) for e in reconstructed), point)
        # Both sides share exactly the same interpolated V at the cut.
        for original in whole:
            if original["from"][1] < 16 < original["to"][1]:
                lower = next(e for e in reconstructed if e["name"] == original["name"] and e["to"][1] == 16)
                upper = next(e for e in reconstructed if e["name"] == original["name"] and e["from"][1] == 16)
                self.assertNotIn("up", lower["faces"])
                self.assertNotIn("down", upper["faces"])
                for face in ("north", "south", "east", "west"):
                    if face in lower["faces"] and face in upper["faces"]:
                        self.assertEqual(lower["faces"][face]["uv"][1], upper["faces"][face]["uv"][3])

    def test_controls_and_hook(self):
        whole = mixer.whole_machine()
        panel = next(e for e in whole if e["name"] == "front_control_panel")
        self.assertEqual(panel["faces"], {"north": {"texture": "#control", "uv": [0, 0, 16, 16]}})
        # Right stroke of '2' is at its top, left stroke is at its bottom.
        self.assertEqual(mixer.control_pixel(21, 7), (183, 204, 137, 255))
        self.assertEqual(mixer.control_pixel(17, 11), (183, 204, 137, 255))
        self.assertNotEqual(mixer.control_pixel(17, 7), (183, 204, 137, 255))
        hook = [e for e in whole if e["name"].startswith("hook_") and e["name"] != "hook_drive_collar"]
        self.assertEqual(len(hook), 7)
        self.assertTrue(any(e.get("rotation", {}).get("angle") == 45 for e in hook))
        self.assertFalse(any(contains(e, (8, 17, 6)) for e in hook), "Curl center must remain open")

    def test_eight_states_loot_and_textures(self):
        states = json.loads((mixer.ASSETS / f"blockstates/{mixer.ID}.json").read_text())["variants"]
        self.assertEqual(set(states), {f"facing={f},primary={p}" for f in ("north", "east", "south", "west")
                                      for p in ("true", "false")})
        for model_name in (f"block/bakery/{mixer.ID}_lower", f"block/bakery/{mixer.ID}_upper", f"item/{mixer.ID}"):
            model = json.loads((mixer.ASSETS / f"models/{model_name}.json").read_text())
            mixer.validate_model_elements(model["elements"])
            for path in model["textures"].values():
                self.assertTrue((mixer.ASSETS / f"textures/{path.split(':')[1]}.png").is_file())
        loot = json.loads((mixer.ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{mixer.ID}.json").read_text())
        self.assertEqual(loot["pools"][0]["entries"][0]["name"], f"craftdistrict:{mixer.ID}")
        self.assertEqual(len(loot["pools"]), 1)


if __name__ == "__main__":
    unittest.main()
