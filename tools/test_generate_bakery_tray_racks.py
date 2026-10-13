"""Static rack asset regressions, not Minecraft world interaction tests."""

import json
import sys
import unittest

sys.dont_write_bytecode = True
import generate_bakery_tray_racks as racks
from generate_flour_sacks import bounds


class TrayRackAssetsTest(unittest.TestCase):
    def test_exact_two_variants_and_four_loaded_levels(self):
        self.assertEqual(racks.IDS, ("bakery_tray_rack_empty", "bakery_tray_rack_baked"))
        self.assertEqual(len(racks.LEVELS), 6)
        self.assertEqual(racks.OCCUPIED, (0, 1, 3, 5))
        contents, _ = racks.baked_contents()
        self.assertEqual({e["name"] for e in contents if e["name"].endswith("_floor")},
                         {f"tray_{index}_floor" for index in racks.OCCUPIED})

    def test_shared_frame_in_world_and_inventory(self):
        empty = json.loads((racks.ASSETS / f"blockstates/{racks.IDS[0]}.json").read_text())["multipart"]
        baked = json.loads((racks.ASSETS / f"blockstates/{racks.IDS[1]}.json").read_text())["multipart"]
        self.assertEqual(empty, [e for e in baked if "_frame_" in e["apply"]["model"]])
        self.assertEqual(len(empty), 8)
        self.assertEqual(len(baked), 16)
        for facing in ("north", "east", "south", "west"):
            for primary in ("true", "false"):
                condition = {"facing": facing, "primary": primary}
                self.assertEqual(sum(e["when"] == condition for e in empty), 1)
                self.assertEqual(sum(e["when"] == condition for e in baked), 2)
        items = [json.loads((racks.ASSETS / f"models/item/{model_id}.json").read_text()) for model_id in racks.IDS]
        count = len(items[0]["elements"])
        self.assertEqual(items[0]["elements"], items[1]["elements"][:count])

    def test_model_constraints_and_resource_paths(self):
        for prefix in ("frame", "contents"):
            for row, name in enumerate(("lower", "upper")):
                model = json.loads((racks.MODELS / f"bakery_tray_rack_{prefix}_{name}.json").read_text())
                racks.validate_model_elements(model["elements"])
                for piece in model["elements"]:
                    lo, hi = bounds(piece)
                    self.assertTrue(all(-1e-6 <= lo[i] < hi[i] <= 16+1e-6 for i in range(3)), piece["name"])
                    self.assertTrue(all(-16 <= v <= 32 for key in ("from", "to") for v in piece[key]))
                    if "rotation" in piece:
                        self.assertIn(piece["rotation"]["angle"], (-45, -22.5, 0, 22.5, 45))
                    for face in piece["faces"].values():
                        self.assertTrue(all(0 <= v <= 16 for v in face["uv"]))
                        self.assertIn(face.get("rotation", 0), (0, 90, 180, 270))
                        self.assertIn(face["texture"][1:], model["textures"])
                for path in model["textures"].values():
                    self.assertTrue((racks.ASSETS / f"textures/{path.split(':')[1]}.png").is_file())

    def test_casters_and_empty_open_interior(self):
        frame, shapes = racks.frame()
        bottoms = [e for e in frame if e["name"].endswith("_tire_bottom")]
        self.assertEqual(len(bottoms), 4)
        self.assertTrue(all(e["from"][1] == 0 for e in bottoms))
        # No sheet or full-cube collision closes the empty rack's center.
        for y in (7, 12, 16, 21, 26):
            self.assertFalse(any(all(lo[i] < point < hi[i] for i, point in enumerate((8, y, 8))) for lo, hi in shapes))
        for lo, hi in shapes:
            self.assertTrue(all(0 <= lo[i] < hi[i] <= (32 if i == 1 else 16) for i in range(3)))

    def test_products_rest_on_trays(self):
        _, boxes = racks.baked_contents()
        self.assertEqual(len(boxes), 13)  # Four pans, nine distinct products.
        for lo, hi in boxes[4:]:
            self.assertIn(round(lo[1], 6), [level+.25 for level in (racks.LEVELS[i] for i in racks.OCCUPIED)])
            self.assertGreaterEqual(lo[0], 2.75)
            self.assertLessEqual(hi[0], 13.25)
            self.assertGreaterEqual(lo[2], 3.25)
            self.assertLessEqual(hi[2], 12.75)

    def test_uv_continuity_at_vertical_seam(self):
        frame, _ = racks.frame()
        lower, upper = racks.part(frame, 0), racks.part(frame, 1)
        for original in frame:
            if original["from"][1] < 16 < original["to"][1]:
                a = next(e for e in lower if e["name"] == original["name"])
                b = next(e for e in upper if e["name"] == original["name"])
                self.assertEqual(a["to"][1], 16)
                self.assertEqual(b["from"][1], 0)
                self.assertNotIn("up", a["faces"])
                self.assertNotIn("down", b["faces"])
                for direction in ("north", "south", "west", "east"):
                    if direction in a["faces"] and direction in b["faces"]:
                        self.assertEqual(a["faces"][direction]["uv"][1], b["faces"][direction]["uv"][3])

    def test_loot_each_variant(self):
        for model_id in racks.IDS:
            loot = json.loads((racks.ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{model_id}.json").read_text())
            self.assertEqual(len(loot["pools"]), 1)
            self.assertEqual(loot["pools"][0]["entries"], [{"type": "minecraft:item", "name": f"craftdistrict:{model_id}"}])


if __name__ == "__main__":
    unittest.main()
