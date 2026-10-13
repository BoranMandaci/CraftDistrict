"""Static geometry/resource checks; these do not exercise a running world."""

import json
import sys
import unittest

sys.dont_write_bytecode = True
import generate_bakery_bread_slicer as slicer
from PIL import Image


class BreadSlicerAssetsTest(unittest.TestCase):
    def test_single_id_and_inherited_support(self):
        self.assertEqual(slicer.ID,"bakery_bread_slicer")
        source = (slicer.ROOT / "src/main/java/com/craftdistrict/block/BreadSlicerBlock.java").read_text()
        self.assertIn("extends CashRegisterBlock",source)
        for method in ("canSurvive","getStateForPlacement","updateShape"):
            self.assertNotIn(method,source)
        self.assertIn("BreadSlicerShapes.SHAPES[turn]",source)

    def test_legal_geometry_and_references(self):
        model,_ = slicer.arrangement()
        slicer.validate_model_elements(model["elements"])
        self.assertLessEqual(len(model["elements"]),70)
        for p in model["elements"]:
            self.assertTrue(1 <= len(p["faces"]) <= 6)
            lo,hi = slicer.bounds(p)
            self.assertTrue(all(-1e-6 <= lo[i] < hi[i] <= 16+1e-6 for i in range(3)))
            if "rotation" in p:
                self.assertIn(p["rotation"]["angle"],(-45,-22.5,0,22.5,45))
                self.assertIn(p["rotation"]["axis"],("x","y","z"))
            for f in p["faces"].values():
                self.assertTrue(all(0 <= v <= 16 for v in f["uv"]))
                self.assertIn(f["texture"][1:],model["textures"])
                self.assertNotIn("cullface",f)
        for texture in model["textures"].values():
            self.assertTrue((slicer.ASSETS / f"textures/{texture.split(':')[1]}.png").is_file(),texture)

    def test_bread_separation_and_contact(self):
        e,_ = slicer.geometry()
        bed = next(p for p in e if p["name"] == "bread_path_bed")
        limits = []
        for prefix in ("whole_input_loaf",*(f"output_slice_{i}" for i in range(5))):
            pieces = [p for p in e if p["name"].startswith(prefix)]
            self.assertEqual(len(pieces),6)
            self.assertEqual(sum("rotation" in p for p in pieces),4)
            b = [slicer.bounds(p) for p in pieces]
            lo = [min(a[i] for a,c in b) for i in range(3)]
            hi = [max(c[i] for a,c in b) for i in range(3)]
            self.assertAlmostEqual(lo[1],bed["to"][1],places=5)
            self.assertTrue(bed["from"][0] <= lo[0] < hi[0] <= bed["to"][0])
            self.assertTrue(bed["from"][2] <= lo[2] < hi[2] <= bed["to"][2])
            if prefix.startswith("output"):
                self.assertAlmostEqual(hi[2]-lo[2],.65)
                self.assertTrue(all(p["faces"]["north"]["texture"] == "#crumb" for p in pieces))
                limits.append((lo,hi))
        for previous,next_slice in zip(limits,limits[1:]):
            self.assertGreater(next_slice[0][2]-previous[1][2],.35)

    def test_cached_shapes_rotate_and_enclose_geometry(self):
        model,collision = slicer.arrangement()
        for p in model["elements"]:
            lo,hi = slicer.bounds(p)
            self.assertTrue(any(all(a[i]-1e-6 <= lo[i] < hi[i] <= b[i]+1e-6 for i in range(3))
                                for a,b in collision),p["name"])
        for original_lo,original_hi in collision:
            lo,hi = original_lo,original_hi
            for turn in range(4):
                self.assertTrue(all(0 <= lo[i] < hi[i] <= 16 for i in range(3)))
                lo,hi = [16-hi[2],lo[1],lo[0]],[16-lo[2],hi[1],hi[0]]
            for a,b in zip(lo+hi,original_lo+original_hi):
                self.assertAlmostEqual(a,b)

    def test_control_uv_orientation_and_texture_atlas(self):
        e,_ = slicer.geometry()
        panel = next(p for p in e if p["name"] == "front_indicator")
        self.assertEqual(panel["faces"]["north"]["uv"],[2,2,14,7])
        self.assertNotIn("rotation",panel)
        fixture = slicer.box("uv_fixture",[2,3,4],[5,9,6],"metal")
        self.assertEqual(fixture["faces"]["north"]["uv"],[11,7,14,13])
        img = Image.open(slicer.ASSETS / f"textures/block/bakery/{slicer.ID}_controls.png").convert("RGBA")
        self.assertNotEqual(img.getpixel((3,2)),img.getpixel((12,2)))
        self.assertEqual(img.getpixel((4,10)),(70,160,99,255))
        self.assertEqual(img.getpixel((10,10)),(190,61,51,255))
        for name in slicer.TILES:
            tile = Image.open(slicer.ASSETS / f"textures/block/bakery/{slicer.ID}_{name}.png").convert("RGBA")
            self.assertEqual(tile.size,(16,16))
            self.assertEqual(tile.getchannel("A").getextrema(),(255,255))

    def test_exports_four_facings_item_and_loot(self):
        states = json.loads((slicer.ASSETS / f"blockstates/{slicer.ID}.json").read_text())["variants"]
        self.assertEqual(set(states),{f"facing={f}" for f in ("north","east","south","west")})
        for face,angle in (("north",0),("east",90),("south",180),("west",270)):
            self.assertEqual(states[f"facing={face}"],{"model":f"craftdistrict:block/bakery/{slicer.ID}","y":angle})
        block = json.loads((slicer.MODELS / f"{slicer.ID}.json").read_text())
        item = json.loads((slicer.ASSETS / f"models/item/{slicer.ID}.json").read_text())
        self.assertEqual(block["elements"],item["elements"])
        self.assertEqual(block["textures"],item["textures"])
        loot = json.loads((slicer.ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{slicer.ID}.json").read_text())
        self.assertEqual(loot["pools"][0]["entries"],[{"type":"minecraft:item","name":f"craftdistrict:{slicer.ID}"}])
        self.assertEqual(loot["pools"][0]["rolls"],1)


if __name__ == "__main__":
    unittest.main()
