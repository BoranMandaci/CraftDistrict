"""Static asset/shape regression checks; actual wall interactions require Minecraft."""

import json
import re
import sys
import unittest

sys.dont_write_bytecode = True
import generate_bakery_wall_tools as tools
from generate_flour_sacks import bounds
from PIL import Image


def overlap(a,b):
    lo,hi = bounds(a)
    other_lo,other_hi = bounds(b)
    return all(min(hi[i],other_hi[i]) >= max(lo[i],other_lo[i])-1e-6 for i in range(3))


class WallToolAssetsTest(unittest.TestCase):
    def test_exact_ids_and_inherited_mounting(self):
        self.assertEqual(tools.IDS,("bakery_bread_peel_rack","bakery_utensil_wall_rack","bakery_rolling_pin_rack"))
        source = (tools.ROOT / "src/main/java/com/craftdistrict/block/BakeryWallToolBlock.java").read_text()
        self.assertIn("extends BakerySignageBlock",source)
        self.assertIn("super(BakerySignageBlock.Variant.WALL_SHOP, properties)",source)
        self.assertNotIn("getStateForPlacement",source)
        self.assertNotIn("canSurvive",source)
        self.assertNotIn("updateShape",source)
        self.assertEqual(re.findall(r'[A-Z_]+\("([a-z_]+)"\)',source),["bread_peel","utensil","rolling_pin"])

    def test_valid_models_shapes_and_four_directions(self):
        for model_id in tools.IDS:
            model,collision = tools.arrangement(model_id)
            tools.validate_model_elements(model["elements"])
            self.assertLessEqual(len(model["elements"]),40)
            self.assertEqual(max(hi[2] for lo,hi in collision),16)
            self.assertGreaterEqual(min(lo[2] for lo,hi in collision),11.375)
            for e in model["elements"]:
                lo,hi = bounds(e)
                self.assertTrue(all(-1e-6 <= lo[i] <= hi[i] <= 16+1e-6 for i in range(3)))
                if "rotation" in e:
                    self.assertIn(e["rotation"]["angle"],(-45,-22.5,0,22.5,45))
                for f in e["faces"].values():
                    self.assertTrue(all(0 <= uv <= 16 for uv in f["uv"]))
                    self.assertIn(f.get("rotation",0),(0,90,180,270))
                    self.assertIn(f["texture"][1:],model["textures"])
            for original_lo,original_hi in collision:
                lo,hi = original_lo,original_hi
                for turn in range(4):
                    self.assertTrue(all(0 <= lo[i] < hi[i] <= 16 for i in range(3)))
                    lo,hi = [16-hi[2],lo[1],lo[0]],[16-lo[2],hi[1],hi[0]]
                self.assertEqual(lo,original_lo)
                self.assertEqual(hi,original_hi)

    def test_peels_are_flat_wide_heads_with_long_handles(self):
        e,_ = tools.bread_peels()
        for name in ("long_peel","short_peel"):
            head = [p for p in e if p["name"].startswith(name+"_paddle")]
            self.assertEqual(len(head),6)
            limits = [bounds(p) for p in head]
            width = max(b[0] for a,b in limits)-min(a[0] for a,b in limits)
            depth = max(b[2] for a,b in limits)-min(a[2] for a,b in limits)
            handle = next(p for p in e if p["name"] == name+"_long_handle")
            neck = next(p for p in e if p["name"] == name+"_neck")
            self.assertGreater(width,4)
            self.assertLess(depth,1)
            self.assertGreater(handle["to"][1]-handle["from"][1],7)
            self.assertTrue(overlap(handle,neck))
            self.assertTrue(any(overlap(p,neck) for p in head))
            self.assertTrue(overlap(handle,next(p for p in e if p["name"] == name+"_handle_clamp")))

    def test_utensils_connect_to_hooks(self):
        e,_ = tools.utensils()
        for index,name in enumerate(("scraper_hanging_tab","spatula_handle","cutter_handle","palette_knife_handle")):
            handle = next(p for p in e if p["name"] == name)
            hook = next(p for p in e if p["name"] == f"hook_{index}_tip")
            self.assertTrue(overlap(handle,hook),name)
        wheel = [p for p in e if p["name"].startswith("dough_cutter_wheel")]
        self.assertEqual(len(wheel),6)
        self.assertTrue(any(overlap(p,next(p for p in e if p["name"] == "cutter_left_fork")) for p in wheel))
        self.assertTrue(any(overlap(p,next(p for p in e if p["name"] == "cutter_right_fork")) for p in wheel))

    def test_three_round_pins_and_cradles(self):
        e,_ = tools.rolling_pins()
        widths = []
        for index in range(3):
            pieces = [p for p in e if p["name"].startswith(f"pin_{index}_barrel")]
            self.assertEqual(len(pieces),6)
            self.assertTrue(any(p.get("rotation",{}).get("axis") == "x" for p in pieces))
            widths.append(max(p["to"][0] for p in pieces)-min(p["from"][0] for p in pieces))
            for side in ("left","right"):
                handle = next(p for p in e if p["name"] == f"pin_{index}_{side}_handle")
                cradle = next(p for p in e if p["name"] == f"pin_{index}_{side}_cradle")
                self.assertTrue(any(overlap(p,handle) for p in pieces))
                self.assertTrue(any(overlap(p,cradle) for p in pieces))
        self.assertEqual(len(set(widths)),3)

    def test_exported_resources_and_outward_uvs(self):
        p = tools.box("asymmetric_uv_fixture",[2,3,12],[5,9,13],"rail")
        self.assertEqual(p["faces"]["north"]["uv"],[11,7,14,13])
        self.assertEqual(p["faces"]["south"]["uv"],[2,7,5,13])
        for model_id in tools.IDS:
            states = json.loads((tools.ASSETS / f"blockstates/{model_id}.json").read_text())["variants"]
            self.assertEqual(set(states),{f"facing={face}" for face in ("north","east","south","west")})
            for folder in ("models/block/bakery","models/item"):
                model = json.loads((tools.ASSETS / f"{folder}/{model_id}.json").read_text())
                tools.validate_model_elements(model["elements"])
                for path in model["textures"].values():
                    self.assertTrue((tools.ASSETS / f"textures/{path.split(':')[1]}.png").is_file())
            loot = json.loads((tools.ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{model_id}.json").read_text())
            self.assertEqual(loot["pools"][0]["entries"],[{"type":"minecraft:item","name":f"craftdistrict:{model_id}"}])
        for name in tools.TILES:
            tile = Image.open(tools.ASSETS / f"textures/block/bakery/bakery_wall_tools_{name}.png").convert("RGBA")
            self.assertEqual(tile.size,(16,16))
            self.assertEqual(tile.getchannel("A").getextrema(),(255,255))


if __name__ == "__main__":
    unittest.main()
