# Bakery Bread Shelf

ID: `craftdistrict:bakery_bread_shelf`. Three angled crate tiers in a 1 x 1 x 2 footprint.
No inventory, interaction, Block Entity, or ticking. Collision is one full cube
per occupied position; shelves and contents use normal baked block models.

## Structure

- `TallDecorationBlock` joins the primary/lower block to the block directly above.
- Both parts share a horizontal `facing`. `primary=true` is lower; `false` is upper.
- Shared placement checks build height, world border, loaded chunks, replacement,
  and entity collision for both positions before placing the lower part.
- The server rechecks the upper position before placing it. A failed upper write
  removes the original lower state, without dropping items or deleting an
  unrelated block substituted by another callback.
- Server removal verifies block identity, facing and the opposite part marker.
  It removes the partner without drops and guards against recursive removal.
- Ordinary player breaking uses the broken part's loot table once in Survival;
  Creative breaking uses vanilla behavior with no drops. Both parts have the same
  item loot table so either can be the part that the player breaks.

The display counter's assets and registration are preserved. Its placement also
benefits from the shared bounds/collision checks. Neither decoration requires a
support block below it; like ordinary building blocks, it can remain floating.

## Editing assets

Run `python tools/generate_bakery_bread_shelf.py` from the project root. The script
uses the counter generator's model helpers and palette without rewriting counter
assets. The counter's wood and bread material textures are shared. The shelf owns
three opaque 16x16 bread tiles: a seeded tile, an alternating fold pattern for the
braided loaf and a toasted sesame crust for the simit-inspired ring. No counter
texture is changed.

## Rustic display-rack redesign

The cabinet geometry is replaced entirely. Two rear uprights and two sloping
side supports stand on sled feet. There is no rear panel, solid side wall, white
plate or full-depth roof. A narrow raised-center wooden header finishes the top.

Three shallow wooden crates tilt forward by 22.5 degrees. Their unrotated floor
undersides are at y=3, 11.25 and 20.75 model units, with front edges stepping back
to z=2, 3.5 and 5. Front retaining rails and low side/back rims hold the display.
Each crate and its entire product group share one X-axis rotation and pivot, so
the bread bottoms rest exactly on the tilted crate floor.

- Bottom crate: a broad round boule with a lower dome, a small crescent pastry
  opening toward the customer, a separate seeded roll behind it and a cinnamon
  pastry in the front-left pocket. This is the fullest tier, with four products.
- Middle crate: one slimmer long scored baguette on the left and two staggered
  simit-inspired sesame rings on the right. The slightly smaller second ring sits
  behind and to the right of the first. Each ring has eight connected perimeter
  cuboids and an actual empty center showing the crate floor. Both low-profile
  rings rest directly on the wood, without overlapping, stacking or white plates.
  This three-product tier is the focal display.
- Top crate: a lower, longer braided loaf with three broad connected folds,
  and a flat berry slice. The two-product presentation leaves some open wood.
  A fold texture follows the three lobes. Small crown gaps and staggered outlines
  replace the tall repeated bread columns.

### Portion and arrangement refinement

Sizes below are measured before the shared tray tilt, in 1/16-block model units:

| Product | Previous width x depth x height | Revised width x depth x height |
| --- | --- | --- |
| Front simit | 6 x 7 x 0.875 | 4.5 x 5.25 x 0.75 |
| Rear simit (added) | - | 4 x 4.25 x 0.75 |
| Round boule | 5.5 x 5.5 x 3.25 | 4.95 x 4.95 x 3.0875 |
| Braided loaf | 9 x 4.5 x 2.5 | 8.1 x 4.275 x 2.375 |
| Baguette | 3 x 9 x 1.875 | 3 x 8.55 x 1.875 |

The front ring retains the previous refinement's size, but moves from
(x=10,z=7.75) to (x=9.5,z=6.5). Its outer bounds are x=[7.25,11.75],
z=[3.875,9.125]. The added rear ring is centered at (x=11,z=11.375), with outer
bounds x=[9,13], z=[9.25,13.5]. A 0.125-unit gap separates the two rings along Z;
the offset centers and different sizes avoid a symmetric pair. All eight pieces
of each ring transform together, preserving its open center and sesame UV pattern.
The long baguette remains beside them. The cinnamon pastry moves from the top
tier to the bottom, retaining its 3 x 2 x 2 size and occupying x=[3,6],
z=[2.75,4.75] before tilt. Other product geometry, tier positions and all materials
are unchanged. `portion` scales each complete product about its floor anchor before
the common tray rotation, so no product floats above its wooden support.

The frame, header, feet, tier positions and wooden crate geometry are preserved
from the rack redesign. Each tier emphasizes different shapes with visible space
between products. There are 68 cuboids total (49 lower, 19 upper), eight more than
the previous single-ring composition. All are
ordinary static vanilla JSON elements. No custom renderer or loader extension,
Block Entity, code behavior, registry ID, blockstate schema or loot change is
required. The full-cube collision per occupied block remains unchanged.

Edit `TIERS` and `tray(index)` for shelf/product composition; edit `part(lower)`
for the rack frame. Author in complete-stand coordinates, using 1/16-block units;
north (-Z) is the front. `move_y` translates both geometry and rotation pivots
into upper-block and item-model space. Exported element coordinates remain in
Minecraft's allowed [-16,32] range. After rotation, all geometry fits inside the
original 1 x 1 x 2 footprint. The sloping side beams are split near the shared
block boundary and join along the same tilted end plane; a small portion of each
beam extends through that boundary. Check the join's lighting in-game, including
across a vertical chunk-section boundary. The item combines both parts in a
centered two-block model. Exports can also open in Blockbench; regeneration
replaces manual edits to exported shelf JSON.

The 22.5-degree angle is the smallest nonzero vanilla element angle supported by
both requested Minecraft versions. A smaller arbitrary tilt or combined-axis
element rotation would require a different modeling technique or renderer.
See the [model format documentation](https://docs.neoforged.net/docs/1.21.1/resources/client/models/).

Optional preview (requires Pillow and NumPy):

```powershell
python tools/preview_bakery_model.py bakery_bread_shelf --views
```

The preview renderer reads exported models, applies element rotations and writes
three-quarter, front and side views plus a labeled front/side comparison sheet
under the ignored `build/asset-previews/` directory. It does not simulate the
game's ambient occlusion, chunk lighting or texture filtering, and is not an
in-game screenshot.

## Manual in-world validation

These are checks to perform, not claims of completed in-world tests. Run each on
Forge/Fabric 1.20.1 and NeoForge/Fabric 1.21.1, in a separate test world with cheats.

1. Find Bakery Bread Shelf in the CraftDistrict creative tab. Place it on a flat
   area: verify both parts, all three tilted crate tiers, textures and collision.
   Look from the front and side: products must rest on their crate floors, the
   two simit openings must be visible, the braid must read as one loaf,
   the open frame/header must load, and the beam join must stay continuous. Repeat
   with the lower block at y=15 so the model crosses a chunk-section boundary.
2. Face north, east, south and west and place a fresh shelf each time. Check F3:
   both parts share the facing opposite your placement direction.
3. Obstruct the upper position with stone. Placement must fail without replacing
   stone, leaving a lower half, or consuming a Survival item. Also test an entity
   occupying the upper position and an already occupied lower position.
4. At the Overworld build limit, placement at y=319 must fail; a lower part at
   y=318 and upper at y=319 should fit. Check the world-border edge as well.
5. In Survival, break the lower and upper halves of separate fresh shelves. Both
   must disappear, with exactly one shelf item dropped in each case. Clear old
   dropped items first so counting is unambiguous.
6. In Creative, repeat both break choices. Both parts must disappear with no drops.
7. Place adjacent shelves, including different facings. Break one: the other
   shelves and unrelated nearby blocks must remain intact.
8. Save, quit, reopen, and revisit after chunk unloading. Both parts, facing,
   collision and paired breaking must persist.
9. Recheck the display counter's placement, blocked second position and paired
   breaking because placement validation is shared.

Directly spawning an individual part with commands is outside normal placement;
there is no ticking repair of orphan states. Placement-event cancellation or
late world changes from other mods need compatibility testing: a late rollback
uses vanilla BlockItem flow and may still consume the attempted placement item.
