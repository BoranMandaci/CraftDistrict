"""Static asset regression checks; placement still needs a running Minecraft world."""

import json
import re
import sys
import unittest

sys.dont_write_bytecode = True
import generate_bakery_signage as signage
from generate_flour_sacks import bounds, rotate_point


class SignageAssetsTest(unittest.TestCase):
    def test_five_ids_and_fixed_variant_order(self):
        self.assertEqual(signage.IDS, ("bakery_hanging_shop_sign", "bakery_wall_shop_sign",
                                      "bakery_menu_board_wall", "bakery_daily_special_board", "bakery_sidewalk_menu_board"))
        source = (signage.ROOT / "src/main/java/com/craftdistrict/block/BakerySignageBlock.java").read_text()
        variants = re.findall(r'[A-Z_]+\("([a-z_]+)"\)', source)
        self.assertEqual(variants, ["hanging_shop", "wall_shop", "wall_menu", "daily_special", "sidewalk_menu"])
        self.assertIn("context.getClickedFace()", source)
        self.assertIn("!face.getAxis().isHorizontal()", source)

    def test_vanilla_contract_shapes_and_rotations(self):
        for model_id in signage.IDS:
            model, collision = signage.arrangement(model_id)
            signage.validate_model_elements(model["elements"])
            self.assertLessEqual(len(model["elements"]), 24)
            for piece in model["elements"]:
                self.assertTrue(all(-16 <= value <= 32 for key in ("from", "to") for value in piece[key]))
                if "rotation" in piece:
                    self.assertIn(piece["rotation"]["angle"], (-45, -22.5, 0, 22.5, 45))
                    self.assertFalse(piece["rotation"]["rescale"])
                for face in piece["faces"].values():
                    self.assertEqual(len(face["uv"]), 4)
                    self.assertTrue(all(0 <= value <= 16 for value in face["uv"]))
                    self.assertIn(face["texture"][1:], model["textures"])
                    self.assertIn(face.get("rotation", 0), (0, 90, 180, 270))
            for lo, hi in collision:
                self.assertTrue(all(0 <= lo[i] < hi[i] <= 16 for i in range(3)), (model_id, lo, hi))
                for _ in range(4):
                    lo, hi = [16-hi[2], lo[1], lo[0]], [16-lo[2], hi[1], hi[0]]
                    self.assertTrue(all(0 <= lo[i] < hi[i] <= 16 for i in range(3)))

    def test_wall_contact_and_thin_boards(self):
        for model_id in signage.IDS[:-1]:
            model, _ = signage.arrangement(model_id)
            limits = [bounds(e) for e in model["elements"]]
            self.assertEqual(max(hi[2] for lo, hi in limits), 16)
            if model_id != signage.IDS[0]:
                self.assertLessEqual(16-min(lo[2] for lo, hi in limits), 2.5)
        hanging = signage.hanging()
        brace = next(e for e in hanging if e["name"] == "bracket_brace")
        r = brace["rotation"]
        lower = rotate_point((8, 11.5, 13.875), r["origin"], r["axis"], r["angle"])
        upper = rotate_point((8, 15, 13.875), r["origin"], r["axis"], r["angle"])
        self.assertGreaterEqual(lower[2]+.75/2**.5/2, 15.25)
        self.assertGreaterEqual(upper[1]+.75/2**.5/2, 14.5)

    def test_double_sided_artwork_keeps_outward_uvs(self):
        for model_id, names, faces in (
            (signage.IDS[0], ("plaque_left_artwork", "plaque_right_artwork"), ("west", "east")),
            (signage.IDS[4], ("front_artwork", "rear_artwork"), ("north", "south"))):
            model, _ = signage.arrangement(model_id)
            for name, direction in zip(names, faces):
                item = next(e for e in model["elements"] if e["name"] == name)
                self.assertEqual(list(item["faces"]), [direction])
                self.assertEqual(item["faces"][direction]["uv"], [0, 0, 16, 16])
                self.assertNotIn("rotation", item["faces"][direction])
        # Unequal menu/name line lengths give an asymmetric UV fixture.
        self.assertNotEqual(signage.pixel("menu", 11, 16), signage.pixel("menu", 20, 16))

    def test_aframe_feet_and_nonoverlapping_rails(self):
        elements, _ = signage.sidewalk()
        feet = [p for p in elements if p["name"].endswith("_foot")]
        self.assertEqual(len(feet), 4)
        self.assertTrue(all(p["from"][1] == 0 for p in feet))
        for p in elements:
            if p["name"].endswith("_leg"):
                self.assertEqual(p["to"][1], 1.5)
        # Coplanar same-facing frame/leg surfaces would flicker in the game.
        for i, e in enumerate(elements):
            for other in elements[i+1:]:
                if e.get("rotation") != other.get("rotation"):
                    continue
                for face in set(e["faces"]) & set(other["faces"]):
                    axis, high = {"north": (2, False), "south": (2, True), "west": (0, False),
                                  "east": (0, True), "down": (1, False), "up": (1, True)}[face]
                    key = "to" if high else "from"
                    if abs(e[key][axis]-other[key][axis]) > 1e-8:
                        continue
                    overlap = all(min(e["to"][a], other["to"][a])-max(e["from"][a], other["from"][a]) > 1e-8
                                  for a in range(3) if a != axis)
                    self.assertFalse(overlap, (e["name"], other["name"], face))

    def test_exported_paths_states_and_loot(self):
        for model_id in signage.IDS:
            states = json.loads((signage.ASSETS / f"blockstates/{model_id}.json").read_text())["variants"]
            self.assertEqual(set(states), {f"facing={f}" for f in ("north", "east", "south", "west")})
            for key, angle in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                self.assertEqual(states[f"facing={key}"], {"model": f"craftdistrict:block/bakery/{model_id}", "y": angle})
            for name in (f"block/bakery/{model_id}", f"item/{model_id}"):
                model = json.loads((signage.ASSETS / f"models/{name}.json").read_text())
                signage.validate_model_elements(model["elements"])
                for path in model["textures"].values():
                    self.assertTrue((signage.ASSETS / f"textures/{path.split(':')[1]}.png").is_file())
            loot = json.loads((signage.ROOT / f"src/main/resources/data/craftdistrict/loot_tables/blocks/{model_id}.json").read_text())
            self.assertEqual(len(loot["pools"]), 1)
            self.assertEqual(loot["pools"][0]["entries"][0]["name"], f"craftdistrict:{model_id}")


if __name__ == "__main__":
    unittest.main()
