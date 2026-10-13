"""Static model/resource tests; no placement or gameplay test is implied."""

import json
import sys
import unittest
from collections import Counter

sys.dont_write_bytecode = True
import generate_refrigerated_displays as displays
from generate_flour_sacks import bounds
from PIL import Image


class RefrigeratedDisplayAssetsTest(unittest.TestCase):
    def test_exact_variants_and_shared_case(self):
        self.assertEqual(displays.IDS, ("refrigerated_cake_display", "refrigerated_pastry_display"))
        states = [json.loads((displays.ASSETS / f"blockstates/{name}.json").read_text())["multipart"] for name in displays.IDS]
        frames = [[e for e in s if "/refrigerated_display_case_" in e["apply"]["model"]] for s in states]
        self.assertEqual(frames[0], frames[1])
        self.assertEqual(len(frames[0]), 16)
        for entries in states:
            self.assertEqual(len(entries), 32)
            for facing in ("north", "east", "south", "west"):
                for part in range(4):
                    self.assertEqual(sum(e["when"] == {"facing": facing, "part": str(part)} for e in entries), 2)
        items = [json.loads((displays.ASSETS / f"models/item/{name}.json").read_text()) for name in displays.IDS]
        case, _ = items
        case_elements = [e for e in case["elements"] if e["name"] in {p["name"] for p in displays.shared_case()}]
        self.assertEqual(case_elements, items[1]["elements"][:len(case_elements)])

    def test_binary_alpha_single_plane_glass(self):
        tile = Image.open(displays.ASSETS / "textures/block/bakery/refrigerated_display_glass.png").convert("RGBA")
        alpha = list(tile.getchannel("A").tobytes())
        self.assertEqual(set(alpha), {0, 255})
        self.assertGreater(alpha.count(0)/len(alpha), .99)
        planes = [e for e in displays.shared_case() if e["name"].endswith("_glass")]
        self.assertEqual(len(planes), 3)
        for e in planes:
            self.assertEqual(sum(a == b for a, b in zip(e["from"], e["to"])), 1)
            self.assertEqual(len(e["faces"]), 2)
            self.assertTrue(all(f["texture"] == "#glass" and "cullface" not in f for f in e["faces"].values()))
        client = (displays.ROOT / "src/main/java/com/craftdistrict/client/CraftDistrictClient.java").read_text()
        self.assertIn("RenderType.cutout()", client)
        self.assertIn("REFRIGERATED_CAKE_DISPLAY.get()", client)
        self.assertIn("REFRIGERATED_PASTRY_DISPLAY.get()", client)

    def test_models_textures_and_local_bounds(self):
        for prefix in ("case", "cake_contents", "pastry_contents"):
            for part in range(4):
                model = json.loads((displays.MODELS / f"refrigerated_display_{prefix}_{part}.json").read_text())
                self.assertEqual(model["render_type"], "minecraft:cutout")
                displays.validate_model_elements(model["elements"])
                for piece in model["elements"]:
                    lo, hi = bounds(piece)
                    self.assertTrue(all(-1e-6 <= lo[i] <= hi[i] <= 16+1e-6 for i in range(3)), (prefix, part, piece["name"], lo, hi))
                    if "rotation" in piece:
                        self.assertIn(piece["rotation"]["angle"], (-45, -22.5, 0, 22.5, 45))
                    for face in piece["faces"].values():
                        self.assertTrue(all(0 <= uv <= 16 for uv in face["uv"]))
                        self.assertIn(face["texture"][1:], model["textures"])
                        self.assertIn(face.get("rotation", 0), (0, 90, 180, 270))
                for path in model["textures"].values():
                    self.assertTrue((displays.ASSETS / f"textures/{path.split(':')[1]}.png").is_file())

    def test_dessert_contact_spacing_and_shelf_composition(self):
        for variant in ("cake", "pastry"):
            _, groups = displays.composition(variant)
            self.assertEqual(Counter(g["shelf"] for g in groups), {0: 4, 1: 3, 2: 2})
            for i, group in enumerate(groups):
                lo, hi = group["lo"], group["hi"]
                self.assertAlmostEqual(lo[1], displays.SHELVES[group["shelf"]]+displays.SHELF_THICKNESS)
                self.assertTrue(1.75 <= lo[0] < hi[0] <= 30.25)
                self.assertTrue(2 <= lo[2] < hi[2] <= 14.25)
                ceiling = displays.SHELVES[group["shelf"]+1] if group["shelf"] < 2 else 30.375
                self.assertLess(hi[1], ceiling)
                self.assertFalse(lo[0] < 16 < hi[0], "Rotated products must stay in their own column")
                for other in groups[i+1:]:
                    if other["shelf"] == group["shelf"]:
                        overlap = all(min(hi[a], other["hi"][a]) > max(lo[a], other["lo"][a]) for a in (0,2))
                        self.assertFalse(overlap, (group["name"], other["name"]))

    def test_directional_uv_clipping_and_internal_faces(self):
        element = displays.box("test_panel", [0, 0, 1], [32, 32, 2], "steel")
        lo, right = displays.clip(element, 0, 0), displays.clip(element, 1, 0)
        self.assertEqual(lo["faces"]["north"]["uv"], [8, 8, 16, 16])
        self.assertEqual(right["faces"]["north"]["uv"], [0, 8, 8, 16])
        self.assertEqual(lo["faces"]["south"]["uv"], [0, 8, 8, 16])
        self.assertNotIn("east", lo["faces"])
        self.assertNotIn("west", right["faces"])
        self.assertNotIn("up", lo["faces"])
        upper = displays.clip(element, 0, 1)
        self.assertNotIn("down", upper["faces"])
        self.assertEqual(lo["faces"]["north"]["uv"][1], upper["faces"]["north"]["uv"][3])
        # Front glass remains an exact plane; the horizontal/vertical pieces
        # partition its UVs without changing their direction or duplicating area.
        glass = next(e for e in displays.shared_case() if e["name"] == "front_glass")
        pieces = [displays.clip(glass, p%2, p//2) for p in range(4)]
        self.assertEqual(len(pieces), 4)
        self.assertTrue(all(p["from"][2] == p["to"][2] == 1.375 for p in pieces))
        self.assertAlmostEqual(sum((p["to"][0]-p["from"][0])*(p["to"][1]-p["from"][1]) for p in pieces), 29*22.75)

    def test_item_geometry_and_loot(self):
        for name in displays.IDS:
            item = json.loads((displays.ASSETS / f"models/item/{name}.json").read_text())
            displays.validate_model_elements(item["elements"])
            self.assertTrue(all(-16 <= v <= 32 for e in item["elements"] for key in ("from", "to") for v in e[key]))
            loot = json.loads((displays.ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{name}.json").read_text())
            self.assertEqual(len(loot["pools"]), 1)
            self.assertEqual(loot["pools"][0]["entries"], [{"type":"minecraft:item", "name":f"craftdistrict:{name}"}])


if __name__ == "__main__":
    unittest.main()
